const http = require('http');
const fs = require('fs');
const path = require('path');

const root = process.cwd();
const port = 8888;

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
  res.setHeader('X-Frame-Options', 'DENY');
  
  let url = req.url.split('?')[0];
  let filePath = path.join(root, url === '/' ? 'launch.html' : url);
  
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
  
  fs.stat(filePath, (err, stats) => {
    if(err) {
      if (req.url === '/favicon.ico') {
        res.writeHead(204);
        res.end();
        return;
      }
      console.log('404:', req.url, '->', filePath);
      res.writeHead(404, {'Content-Type': 'text/html'});
      res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
      return;
    }

    if (stats.isDirectory()) {
      console.log('DIR:', req.url, '->', filePath);
      res.writeHead(404, {'Content-Type': 'text/html'});
      res.end('<h1>404 Not Found (Directory)</h1>');
      return;
    }

    // Range Support
    const range = req.headers.range;
    const fileSize = stats.size;

    if (range) {
      const parts = range.replace(/bytes=/, "").split("-");
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(filePath, {start, end});
      const head = {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': ctx,
      };
      res.writeHead(206, head);
      file.pipe(res);
      // console.log('206:', req.url); // Too spammy for large files
    } else {
      const head = {
        'Content-Length': fileSize,
        'Content-Type': ctx,
        'Accept-Ranges': 'bytes', // Advertise support
      };
      res.writeHead(200, head);
      fs.createReadStream(filePath).pipe(res);
      console.log('200:', req.url, '(', ctx, ')');
    }
  });
});

server.listen(port, '0.0.0.0', () => {
  console.log('🎮 Game server running!');
  console.log('📍 URL: http://localhost:' + port);
  console.log('🛡️ Security headers enabled for SharedArrayBuffer');
  console.log('📂 Root directory:', root);
  console.log('✨ Press Ctrl+C to stop');
});