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

const javaFallbackRoots = [
  path.join(root, 'starsector-gwt', 'src', 'main', 'java'),
  path.join(root, 'tmp_cfr_core'),
];
const enableJavaFallback = process.env.STARSECTOR_ENABLE_JAVA_FALLBACK === '1';
const allowApiJavaFallback = process.env.STARSECTOR_ALLOW_API_JAVA_FALLBACK === '1';
const enableSyntheticJavaFallback = process.env.STARSECTOR_ENABLE_SYNTHETIC_JAVA_FALLBACK !== '0';
const enableSyntheticSerialFile = process.env.STARSECTOR_ENABLE_SYNTHETIC_SERIAL !== '0';
const enableEmptyDirectoryFallback = process.env.STARSECTOR_ENABLE_EMPTY_DIRECTORY_FALLBACK !== '0';
const javaBasenameIndex = new Map();
let javaBasenameIndexed = false;
const variantFallbackRoot = path.join(root, 'starsector', 'starsector', 'data', 'variants');
const variantBasenameIndex = new Map();
let variantBasenameIndexed = false;

const normalizeSlashPath = (value) =>
  String(value || '')
    .replace(/\\/g, '/')
    .replace(/^\/+/, '');

const addJavaFallbackCandidate = (set, value) => {
  const normalized = normalizeSlashPath(value);
  if (!normalized) return;
  set.add(normalized);
};

const addNestedJavaCollapseCandidates = (set, value) => {
  let current = normalizeSlashPath(value);
  for (let i = 0; i < 4; i += 1) {
    const segments = current.split('/');
    if (segments.length < 2) break;
    const last = segments[segments.length - 1] || '';
    const parent = segments[segments.length - 2] || '';
    if (!/\.java$/i.test(last) || !parent || /\.java$/i.test(parent)) break;
    const collapsed = [...segments.slice(0, -2), `${parent}.java`].join('/');
    if (set.has(collapsed)) break;
    set.add(collapsed);
    current = collapsed;
  }
};

const indexJavaFallbackByBasename = () => {
  if (javaBasenameIndexed) return;
  javaBasenameIndexed = true;

  const walk = (dir) => {
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch (_err) {
      return;
    }
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(full);
        continue;
      }
      if (!entry.isFile() || !/\.java$/i.test(entry.name)) continue;
      const key = entry.name.toLowerCase();
      const list = javaBasenameIndex.get(key) || [];
      list.push(full);
      javaBasenameIndex.set(key, list);
    }
  };

  for (const rootDir of javaFallbackRoots) {
    walk(rootDir);
  }
};

const findJavaFallbackPath = (relativePath) => {
  if (!enableJavaFallback) {
    return null;
  }
  const normalizedRelativePath = normalizeSlashPath(relativePath);
  if (!/\.java$/i.test(normalizedRelativePath)) return null;
  const lowerRelative = normalizedRelativePath.toLowerCase();

  // Default-safe behavior: do not serve decompiled API Java sources as runtime fallback.
  // Janino should resolve API types from classpath jars, not transient .java mirrors.
  if (!allowApiJavaFallback && lowerRelative.includes('com/fs/starfarer/api/')) {
    return null;
  }

  const candidates = new Set();
  addJavaFallbackCandidate(candidates, normalizedRelativePath);
  addJavaFallbackCandidate(
    candidates,
    normalizedRelativePath.replace(/^starsector\/starsector\//i, '')
  );

  const comIdx = normalizedRelativePath.toLowerCase().indexOf('com/fs/');
  if (comIdx >= 0) {
    addJavaFallbackCandidate(candidates, normalizedRelativePath.slice(comIdx));
  }
  const dataScriptsIdx = normalizedRelativePath.toLowerCase().indexOf('data/scripts/');
  if (dataScriptsIdx >= 0) {
    addJavaFallbackCandidate(candidates, normalizedRelativePath.slice(dataScriptsIdx));
  }

  for (const candidate of Array.from(candidates)) {
    addNestedJavaCollapseCandidates(candidates, candidate);
  }

  for (const candidate of candidates) {
    for (const rootDir of javaFallbackRoots) {
      const fullPath = path.join(rootDir, candidate);
      try {
        const stat = fs.statSync(fullPath);
        if (stat.isFile()) return fullPath;
      } catch (_err) {
        // continue
      }
    }
  }

  const baseName = path.basename(normalizedRelativePath).toLowerCase();
  const allowBasenameFallbackOutsideApi =
    /^(aihints|factions|commodities|shiproles|hullmods|tags|stats|skills|memflags|submarkets|global|color)\.java$/i.test(
      baseName
    );

  // Basename-only lookup is restricted to API paths and a small allow-list of
  // well-known Starsector constants classes that scripts probe by short name.
  if (!lowerRelative.includes('com/fs/starfarer/api/') && !allowBasenameFallbackOutsideApi) {
    return null;
  }

  indexJavaFallbackByBasename();
  const byName = javaBasenameIndex.get(baseName) || [];
  if (!byName.length) return null;

  const preferred =
    byName.find((p) => /[\\/]com[\\/]fs[\\/]starfarer[\\/]api[\\/]/i.test(p)) || byName[0];
  return preferred || null;
};

const indexVariantFallbackByBasename = () => {
  if (variantBasenameIndexed) return;
  variantBasenameIndexed = true;

  const walk = (dir) => {
    let entries;
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch (_err) {
      return;
    }
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(full);
        continue;
      }
      if (!entry.isFile() || !/\.variant$/i.test(entry.name)) continue;
      const key = entry.name.toLowerCase();
      const list = variantBasenameIndex.get(key) || [];
      list.push(full);
      variantBasenameIndex.set(key, list);
    }
  };

  walk(variantFallbackRoot);
};

const findVariantFallbackPath = (relativePath) => {
  const normalizedRelativePath = normalizeSlashPath(relativePath);
  if (!/\.variant$/i.test(normalizedRelativePath)) return null;
  if (!normalizedRelativePath.toLowerCase().includes('starsector/starsector/data/variants/')) {
    return null;
  }

  indexVariantFallbackByBasename();
  const baseName = path.basename(normalizedRelativePath).toLowerCase();
  const matches = variantBasenameIndex.get(baseName) || [];
  if (!matches.length) return null;
  if (matches.length === 1) return matches[0];

  // Pick the shallowest path to keep behavior deterministic across duplicate basenames.
  const sorted = matches
    .slice()
    .sort((a, b) => a.length - b.length || a.localeCompare(b));
  return sorted[0] || null;
};

const findHullAliasFallbackPath = (relativePath) => {
  const normalizedRelativePath = normalizeSlashPath(relativePath);
  if (!/\.ship$/i.test(normalizedRelativePath)) return null;
  const marker = 'starsector/starsector/data/hulls/';
  const lower = normalizedRelativePath.toLowerCase();
  const markerIdx = lower.indexOf(marker);
  if (markerIdx < 0) return null;

  const local = normalizedRelativePath.slice(markerIdx + marker.length);
  if (!local || local.includes('/')) return null;

  const base = local.replace(/\.ship$/i, '');
  if (!base.includes('_')) return null;
  const parts = base.split('_').filter(Boolean);
  if (parts.length < 2) return null;

  for (let keep = parts.length - 1; keep >= 1; keep -= 1) {
    const candidateBase = parts.slice(0, keep).join('_');
    if (!candidateBase) continue;
    const candidate = path.join(root, 'starsector', 'starsector', 'data', 'hulls', `${candidateBase}.ship`);
    try {
      const stat = fs.statSync(candidate);
      if (stat.isFile()) return candidate;
    } catch (_err) {
      // continue
    }
  }

  return null;
};

const buildSyntheticJavaSource = (normalizedRelativePath) => {
  const baseRaw = path.basename(String(normalizedRelativePath || ''), '.java');
  let className = String(baseRaw || 'SyntheticStub').replace(/[^A-Za-z0-9_$]/g, '_');
  if (!/^[A-Za-z_$]/.test(className)) {
    className = `_${className}`;
  }
  return `/* synthetic java source fallback for ${normalizedRelativePath} */\npublic class ${className} {}\n`;
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

const sanitizeTabSizedPath = (value) =>
  String(value || '')
    .replace(/%09-?\d+(?=\/|$)/gi, '')
    .replace(/%60t-?\d+(?=\/|$)/gi, '')
    .replace(/\t-?\d+(?=\/|$)/g, '')
    .replace(/`t-?\d+(?=\/|$)/g, '')
    .replace(/\/{2,}/g, '/');
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

  const missionVariantsFromGameFiles = normalizedRelativePath.replace(
    /^starsector[\\/]+starsector[\\/]+files[\\/]+saves[\\/]+missions[\\/]+variants(?:[\\/]+|$)/i,
    'starsector/starsector/data/variants/'
  );
  pushUnique(out, missionVariantsFromGameFiles);

  const missionVariantsFromRootFiles = normalizedRelativePath.replace(
    /^files[\\/]+saves[\\/]+missions[\\/]+variants(?:[\\/]+|$)/i,
    'starsector/starsector/data/variants/'
  );
  pushUnique(out, missionVariantsFromRootFiles);

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
    "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cjrtnc.leaningtech.com; connect-src 'self' https://cjrtnc.leaningtech.com; img-src 'self' data: https://cjrtnc.leaningtech.com; style-src 'self' 'unsafe-inline' https://cjrtnc.leaningtech.com; frame-src 'self' https://cjrtnc.leaningtech.com"
  );
  
  // 🛡️ Additional security headers
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  
  let url = req.url.split('?')[0];
  if (url === '/.starsector_serial' && enableSyntheticSerialFile) {
    res.writeHead(204);
    res.end();
    return;
  }
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
        if (ext === '.java') {
          const javaFallbackPath = findJavaFallbackPath(normalizedRelativePath);
          if (javaFallbackPath) {
            fs.stat(javaFallbackPath, (javaErr, javaStats) => {
              if (!javaErr && javaStats.isFile()) {
                verbose('JAVA-FALLBACK:', req.url, '->', javaFallbackPath);
                serveFile(javaFallbackPath, javaStats, 'text/plain; charset=utf-8');
                return;
              }
              if (req.url === '/favicon.ico') {
                res.writeHead(204);
                res.end();
                return;
              }
              verbose('404:', req.url, '->', filePath, 'candidates=', filePathCandidates);
              res.writeHead(404, {'Content-Type': 'text/html'});
              res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
            });
            return;
          }
        }
        if (ext === '.variant') {
          const variantFallbackPath = findVariantFallbackPath(normalizedRelativePath);
          if (variantFallbackPath) {
            fs.stat(variantFallbackPath, (variantErr, variantStats) => {
              if (!variantErr && variantStats.isFile()) {
                verbose('VARIANT-FALLBACK:', req.url, '->', variantFallbackPath);
                serveFile(variantFallbackPath, variantStats, 'text/plain; charset=utf-8');
                return;
              }
              if (req.url === '/favicon.ico') {
                res.writeHead(204);
                res.end();
                return;
              }
              verbose('404:', req.url, '->', filePath, 'candidates=', filePathCandidates);
              res.writeHead(404, {'Content-Type': 'text/html'});
              res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
            });
            return;
          }
        }
        if (ext === '.ship') {
          const hullAliasFallbackPath = findHullAliasFallbackPath(normalizedRelativePath);
          if (hullAliasFallbackPath) {
            fs.stat(hullAliasFallbackPath, (hullErr, hullStats) => {
              if (!hullErr && hullStats.isFile()) {
                verbose('HULL-ALIAS-FALLBACK:', req.url, '->', hullAliasFallbackPath);
                serveFile(hullAliasFallbackPath, hullStats, 'text/plain; charset=utf-8');
                return;
              }
              if (req.url === '/favicon.ico') {
                res.writeHead(204);
                res.end();
                return;
              }
              verbose('404:', req.url, '->', filePath, 'candidates=', filePathCandidates);
              res.writeHead(404, {'Content-Type': 'text/html'});
              res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
            });
            return;
          }
        }
        if (req.url === '/favicon.ico') {
          res.writeHead(204);
          res.end();
          return;
        }
        if (ext === '.java' && enableSyntheticJavaFallback) {
          const body = buildSyntheticJavaSource(normalizedRelativePath);
          verbose('JAVA-SYNTHETIC:', req.url, '->', normalizedRelativePath);
          res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
          res.end(body);
          return;
        }
        if (
          !ext &&
          enableEmptyDirectoryFallback &&
          /starsector[\\/]+starsector[\\/]+data[\\/]+(?:scripts|weapons|shipsystems|hulls)(?:[\\/]|$)/i.test(
            normalizedRelativePath
          )
        ) {
          verbose('PATH->EMPTY:', req.url, '->', normalizedRelativePath);
          res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
          res.end('');
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

          if (enableEmptyDirectoryFallback) {
            verbose('DIR->EMPTY:', req.url, '->', filePath);
            res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8', 'Cache-Control': 'no-store' });
            res.end('');
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
