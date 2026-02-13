const http = require('http');
const fs = require('fs');
const path = require('path');
const jpegJs = require('jpeg-js');
const { PNG } = require('pngjs');

const root = process.cwd();
const port = 8888;
const verboseLogging = process.env.STARSECTOR_SERVER_VERBOSE === '1';
const missingImageFallback = path.join(
  root,
  'starsector',
  'starsector',
  'graphics',
  'fx',
  'empty.png'
);

const verbose = (...args) => {
  if (verboseLogging) {
    console.log(...args);
  }
};

const jpegToPngCache = new Map();
const MAX_JPEG_CACHE_ENTRIES = 512;

const convertJpegBufferToPng = (jpegBuffer, cacheKey) => {
  if (cacheKey && jpegToPngCache.has(cacheKey)) {
    return jpegToPngCache.get(cacheKey);
  }

  const decoded = jpegJs.decode(jpegBuffer, {
    useTArray: true,
    tolerantDecoding: true,
  });
  if (!decoded || !decoded.width || !decoded.height || !decoded.data) {
    throw new Error('jpeg decode failed');
  }

  const png = new PNG({ width: decoded.width, height: decoded.height });
  Buffer.from(decoded.data).copy(png.data);
  const pngBuffer = PNG.sync.write(png, { colorType: 6 });

  if (cacheKey) {
    jpegToPngCache.set(cacheKey, pngBuffer);
    if (jpegToPngCache.size > MAX_JPEG_CACHE_ENTRIES) {
      const oldestKey = jpegToPngCache.keys().next().value;
      if (oldestKey !== undefined) {
        jpegToPngCache.delete(oldestKey);
      }
    }
  }
  return pngBuffer;
};

const parseRange = (rangeHeader, fileSize) => {
  const parsed = /^bytes=(\d*)-(\d*)$/i.exec((rangeHeader || '').trim());
  if (!parsed) return null;

  const startPart = parsed[1];
  const endPart = parsed[2];
  let start;
  let end;

  if (startPart === '' && endPart !== '') {
    const suffixLength = Number.parseInt(endPart, 10);
    if (!Number.isFinite(suffixLength) || suffixLength <= 0) return null;
    start = Math.max(0, fileSize - suffixLength);
    end = fileSize - 1;
  } else {
    start = Number.parseInt(startPart, 10);
    end = endPart ? Number.parseInt(endPart, 10) : fileSize - 1;
  }

  if (!Number.isFinite(start) || !Number.isFinite(end)) return null;
  if (start < 0) start = 0;
  if (end >= fileSize) end = fileSize - 1;
  if (fileSize === 0 || start >= fileSize || start > end) return null;

  return { start, end };
};

const sanitizeTabSizedPath = (value) => value.replace(/\t-?\d+(?=\/|$)/g, '');
const pushUnique = (arr, value) => {
  if (!value) return;
  if (!arr.includes(value)) arr.push(value);
};

const buildRelativeCandidates = (normalizedRelativePath) => {
  const out = [];
  pushUnique(out, normalizedRelativePath);

  const withFilesPrefix = normalizedRelativePath.replace(
    /^starsector[\\/]+starsector[\\/]+files[\\/]+/i,
    'files/'
  );
  pushUnique(out, withFilesPrefix);

  const filesToGameRoot = normalizedRelativePath.replace(
    /^files[\\/]+/i,
    'starsector/starsector/'
  );
  pushUnique(out, filesToGameRoot);

  const gameFilesToGameRoot = normalizedRelativePath.replace(
    /^starsector[\\/]+starsector[\\/]+files[\\/]+/i,
    'starsector/starsector/'
  );
  pushUnique(out, gameFilesToGameRoot);

  // Some flows probe /files/saves/missions/* for mission data.
  const missionFromFilesSaves = normalizedRelativePath.replace(
    /^starsector[\\/]+starsector[\\/]+files[\\/]+saves[\\/]+missions[\\/]+/i,
    'starsector/starsector/data/missions/'
  );
  pushUnique(out, missionFromFilesSaves);

  const missionFromRootFilesSaves = normalizedRelativePath.replace(
    /^files[\\/]+saves[\\/]+missions[\\/]+/i,
    'starsector/starsector/data/missions/'
  );
  pushUnique(out, missionFromRootFilesSaves);

  return out;
};

const normalizeIndexList = (raw) => {
  if (!raw) return '';
  const lines = raw.split(/\r?\n/);
  const out = [];
  for (const line of lines) {
    // Preserve the original "<entry>\t<size>" contract used by CheerpOS.
    // Only sanitize malformed encoded tab-size fragments in the entry token itself.
    const tab = line.indexOf('\t');
    if (tab < 0) {
      const clean = sanitizeTabSizedPath(line).trim();
      if (clean) out.push(clean);
      continue;
    }
    const entry = sanitizeTabSizedPath(line.slice(0, tab)).trim();
    const sizePart = line.slice(tab + 1).trim();
    if (!entry) continue;
    if (sizePart.length === 0) {
      out.push(entry);
      continue;
    }
    out.push(`${entry}\t${sizePart}`);
  }
  return out.join('\n') + '\n';
};
const server = http.createServer((req, res) => {
  verbose(`[${req.method}] ${req.url}`);
  // 🛡️ COEP/COOP disabled for testing - may not be needed for this game
  // Commented out to allow CheerpJ CDN iframe to load
  // res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
  // res.setHeader('Cross-Origin-Embedder-Policy', 'credentialless');
  
  // 🛡️ Content Security Policy (allow inline scripts + CheerpJ CDN + WASM 'unsafe-eval')
  res.setHeader('Content-Security-Policy', 
    "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cjrtnc.leaningtech.com; connect-src 'self' https://cjrtnc.leaningtech.com; img-src 'self' data:; style-src 'self' 'unsafe-inline' https://cjrtnc.leaningtech.com; frame-src 'self' https://cjrtnc.leaningtech.com"
  );
  
  // 🛡️ Additional security headers
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  
  let url = req.url.split('?')[0];
  if (url === '/starsectorquick') url = '/';
  if (url.startsWith('/starsectorquick/')) {
    url = url.substring('/starsectorquick'.length);
  }
  const relativePath = (url === '/' ? 'launch.html' : url).replace(/^[/\\]+/, '');
  let decodedRelativePath = relativePath;
  try {
    decodedRelativePath = decodeURIComponent(relativePath);
  } catch (decodeErr) {
    decodedRelativePath = relativePath;
  }
  const safeRelativePath = sanitizeTabSizedPath(decodedRelativePath);
  const normalizedRelativePath = safeRelativePath
    .replace(
      /^starsector[\\/]+starsector[\\/]+app[\\/]+starsector[\\/]+starsector[\\/]+/i,
      'starsector/starsector/'
    )
    .replace(
      /^starsector[\\/]+starfarer\.res[\\/]+res[\\/]+/i,
      'starsector/starsector/'
    )
    .replace(
      /^starfarer\.res[\\/]+res[\\/]+/i,
      'starsector/starsector/'
    )
    .replace(
      /^starsector[\\/]+starsector[\\/]+null[\\/]+/i,
      'starsector/starsector/data/'
    )
    .replace(
      /^starsector[\\/]+starsector[\\/]+build[\\/]+/i,
      'build/'
    )
    .replace(
      /^app[\\/]+build[\\/]+/i,
      'build/'
    )
    .replace(
      /^starsectorquick[\\/]+build[\\/]+/i,
      'build/'
    );
  const relativeCandidates = buildRelativeCandidates(normalizedRelativePath);
  const filePathCandidates = relativeCandidates.map((candidate) => path.join(root, candidate));
  let filePath = filePathCandidates[0];

  const ext = path.extname(filePath);
  let ctx = 'text/html';
  if(ext === '.js') ctx = 'text/javascript';
  if(ext === '.css') ctx = 'text/css';
  if(ext === '.wasm') ctx = 'application/wasm';
  if(ext === '.jar') ctx = 'application/java-archive';
  if(ext === '.json') ctx = 'application/json';
  if(ext === '.png') ctx = 'image/png';
  if(ext === '.jpg') ctx = 'image/jpeg';
  if(ext === '.svg') ctx = 'image/svg+xml';
  if(ext === '.list') ctx = 'text/plain; charset=utf-8';
  if(ext === '.csv') ctx = 'text/plain; charset=utf-8';
  if(ext === '.wpn') ctx = 'text/plain; charset=utf-8';
  if(ext === '.proj') ctx = 'text/plain; charset=utf-8';
  if(ext === '.ship') ctx = 'text/plain; charset=utf-8';
  if(ext === '.variant') ctx = 'text/plain; charset=utf-8';
  if(ext === '.skin') ctx = 'text/plain; charset=utf-8';
  if(ext === '.java') ctx = 'text/plain; charset=utf-8';

  const serveFile = (resolvedPath, stats, contentType) => {
    const effectiveType = contentType || ctx;
    const serveBuffer = (buffer, bufferType) => {
      const range = req.headers.range;
      const fileSize = buffer.length;
      if (range) {
        const parsedRange = parseRange(range, fileSize);
        if (!parsedRange) {
          res.writeHead(416, { 'Content-Range': `bytes */${fileSize}` });
          res.end();
          return;
        }
        const { start, end } = parsedRange;
        const chunk = buffer.subarray(start, end + 1);
        res.writeHead(206, {
          'Content-Range': `bytes ${start}-${end}/${fileSize}`,
          'Accept-Ranges': 'bytes',
          'Content-Length': chunk.length,
          'Content-Type': bufferType,
        });
        res.end(chunk);
        return;
      }

      res.writeHead(200, {
        'Content-Length': fileSize,
        'Content-Type': bufferType,
        'Accept-Ranges': 'bytes',
      });
      res.end(buffer);
      verbose('200:', req.url, '(', bufferType, ')');
    };

    if (/\.(jpg|jpeg)$/i.test(resolvedPath)) {
      const cacheKey = `${resolvedPath}:${stats.size}:${stats.mtimeMs}`;
      fs.readFile(resolvedPath, (jpegErr, jpegBuffer) => {
        if (jpegErr) {
          verbose('JPEG-READ-FAIL:', req.url, jpegErr.message || jpegErr);
          res.writeHead(404, {'Content-Type': 'text/html'});
          res.end('<h1>404 Not Found</h1><p>' + resolvedPath + '</p>');
          return;
        }

        try {
          const pngBuffer = convertJpegBufferToPng(jpegBuffer, cacheKey);
          serveBuffer(pngBuffer, 'image/png');
        } catch (convertErr) {
          verbose(
            'JPEG-CONVERT-FAIL:',
            req.url,
            convertErr && convertErr.message ? convertErr.message : String(convertErr)
          );
          fs.stat(missingImageFallback, (fallbackErr, fallbackStats) => {
            if (!fallbackErr && fallbackStats.isFile()) {
              verbose('JPEG-CONVERT->IMG-FALLBACK:', req.url, '->', missingImageFallback);
              serveFile(missingImageFallback, fallbackStats, 'image/png');
              return;
            }
            res.writeHead(500, {'Content-Type': 'text/plain; charset=utf-8'});
            res.end('JPEG conversion failed and fallback image is unavailable.');
          });
        }
      });
      return;
    }

    if (path.basename(resolvedPath).toLowerCase() === 'index.list') {
      fs.readFile(resolvedPath, 'utf8', (readErr, raw) => {
        if (readErr) {
          res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
          res.end('Failed to read index.list');
          return;
        }
        const normalized = normalizeIndexList(raw);
        serveBuffer(Buffer.from(normalized, 'utf8'), 'text/plain; charset=utf-8');
      });
      return;
    }

    const fileSize = stats.size;
    const range = req.headers.range;
    if (range) {
      const parsedRange = parseRange(range, fileSize);
      if (!parsedRange) {
        res.writeHead(416, { 'Content-Range': `bytes */${fileSize}` });
        res.end();
        return;
      }
      const { start, end } = parsedRange;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(resolvedPath, { start, end });
      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': effectiveType,
      });
      file.pipe(res);
      return;
    }

    res.writeHead(200, {
      'Content-Length': fileSize,
      'Content-Type': effectiveType,
      'Accept-Ranges': 'bytes',
    });
    fs.createReadStream(resolvedPath).pipe(res);
    verbose('200:', req.url, '(', effectiveType, ')');
  };

  const tryServeCandidate = (idx) => {
    const candidatePath = filePathCandidates[idx];
    fs.stat(candidatePath, (err, stats) => {
      if (err) {
        if (idx + 1 < filePathCandidates.length) {
          tryServeCandidate(idx + 1);
          return;
        }
        if (req.url === '/favicon.ico') {
          res.writeHead(204);
          res.end();
          return;
        }
        if (/\.(png|jpg|jpeg)$/i.test(filePath)) {
          fs.stat(missingImageFallback, (fallbackErr, fallbackStats) => {
            if (!fallbackErr && fallbackStats.isFile()) {
              verbose('404->IMG-FALLBACK:', req.url, '->', missingImageFallback);
              serveFile(missingImageFallback, fallbackStats, 'image/png');
              return;
            }
            verbose('404:', req.url, '->', filePath, '(fallback missing)');
            res.writeHead(404, {'Content-Type': 'text/html'});
            res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
          });
          return;
        }
        verbose('404:', req.url, '->', filePath, 'candidates=', filePathCandidates);
        res.writeHead(404, {'Content-Type': 'text/html'});
        res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
        return;
      }

      filePath = candidatePath;
      if (stats.isDirectory()) {
        const indexListPath = path.join(filePath, 'index.list');
        fs.stat(indexListPath, (indexErr, indexStats) => {
          if (!indexErr && indexStats.isFile()) {
            verbose('DIR->INDEX:', req.url, '->', indexListPath);
            serveFile(indexListPath, indexStats, 'text/plain; charset=utf-8');
            return;
          }

          verbose('DIR:', req.url, '->', filePath);
          res.writeHead(404, {'Content-Type': 'text/html'});
          res.end('<h1>404 Not Found (Directory)</h1>');
        });
        return;
      }

      serveFile(filePath, stats, ctx);
    });
  };

  tryServeCandidate(0);
});

server.listen(port, '0.0.0.0', () => {
  console.log('🎮 Game server running!');
  console.log('📍 URL: http://localhost:' + port);
  console.log('🛡️ Security headers enabled for SharedArrayBuffer');
  console.log('📂 Root directory:', root);
  console.log('✨ Press Ctrl+C to stop');
});
