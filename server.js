const http = require('http');
const fs = require('fs');
const path = require('path');

const root = process.cwd();
const port = 8888;
const jarRoot = process.env.JAR_ROOT ? path.resolve(process.env.JAR_ROOT) : null;
const assetRoot = process.env.ASSET_ROOT ? path.resolve(process.env.ASSET_ROOT) : null;

const server = http.createServer((req, res) => {
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
  res.setHeader('X-Frame-Options', 'SAMEORIGIN');
  
  let url = req.url.split('?')[0];
  const isJarRequest = jarRoot && (url === '/jars' || url.startsWith('/jars/'));
  let filePath = path.join(root, url === '/' ? 'STARSECTOR_V6J_FINAL_WORKING.html' : url);
  if (isJarRequest) {
    const jarPath = url.replace(/^\/jars\/?/, '');
    filePath = path.join(jarRoot, jarPath);
  }
  
  const ext = path.extname(filePath);
  let ctx = 'text/html';
  if(ext === '.js') ctx = 'text/javascript';
  if(ext === '.css') ctx = 'text/css';
  if(ext === '.wasm') ctx = 'application/wasm';
  if(ext === '.jar') ctx = 'application/java-archive';
  if(ext === '.json') ctx = 'application/json';
  
  const serveFile = (resolvedPath, stats) => {
    res.setHeader('Accept-Ranges', 'bytes');
    const range = req.headers.range;
    if (range) {
      const [startStr, endStr] = range.replace(/bytes=/, '').split('-');
      const start = Number.parseInt(startStr, 10);
      let end = endStr ? Number.parseInt(endStr, 10) : stats.size - 1;
      if (Number.isNaN(start) || Number.isNaN(end)) {
        res.writeHead(416, {
          'Content-Range': `bytes */${stats.size}`
        });
        res.end();
        console.log('416:', req.url, 'invalid range', range);
        return;
      }
      if (end >= stats.size) {
        end = stats.size - 1;
      }
      if (start > end || start >= stats.size) {
        res.writeHead(416, {
          'Content-Range': `bytes */${stats.size}`
        });
        res.end();
        console.log('416:', req.url, 'invalid range', range);
        return;
      }
      const chunkSize = (end - start) + 1;
      res.writeHead(206, {
        'Content-Type': ctx,
        'Content-Range': `bytes ${start}-${end}/${stats.size}`,
        'Content-Length': chunkSize
      });
      fs.createReadStream(resolvedPath, { start, end }).pipe(res);
      console.log('206:', req.url, '(', ctx, ')', `${start}-${end}`);
    } else {
      res.writeHead(200, {
        'Content-Type': ctx,
        'Content-Length': stats.size
      });
      fs.createReadStream(resolvedPath).pipe(res);
      console.log('200:', req.url, '(', ctx, ')');
    }
  };

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      if (req.url === '/favicon.ico') {
        res.writeHead(204);
        res.end();
        return;
      }
      if (assetRoot && !isJarRequest && url !== '/') {
        const assetPath = path.join(assetRoot, url.replace(/^\/+/, ''));
        fs.stat(assetPath, (assetErr, assetStats) => {
          if (assetErr || !assetStats.isFile()) {
            console.log('404:', req.url, '->', filePath);
            res.writeHead(404, {'Content-Type': 'text/html'});
            res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
            return;
          }
          serveFile(assetPath, assetStats);
        });
        return;
      }
      console.log('404:', req.url, '->', filePath);
      res.writeHead(404, {'Content-Type': 'text/html'});
      res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
      return;
    }

    serveFile(filePath, stats);
  });
});

server.listen(port, '0.0.0.0', () => {
  console.log('🎮 Game server running!');
  console.log('📍 URL: http://localhost:' + port);
  console.log('🛡️ Security headers enabled for SharedArrayBuffer');
  console.log('📂 Root directory:', root);
  console.log('✨ Press Ctrl+C to stop');
});
