const http = require('http');
const fs = require('fs');
const path = require('path');

const root = process.cwd();
const port = 8080;

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
  let filePath = path.join(root, url === '/' ? 'STARSECTOR_V6J_FINAL_WORKING.html' : url);
  
  const ext = path.extname(filePath);
  let ctx = 'text/html';
  if(ext === '.js') ctx = 'text/javascript';
  if(ext === '.css') ctx = 'text/css';
  if(ext === '.wasm') ctx = 'application/wasm';
  if(ext === '.jar') ctx = 'application/java-archive';
  if(ext === '.json') ctx = 'application/json';
  
  fs.readFile(filePath, (err, content) => {
    if(err) {
      // Ignore favicon.ico 404s to keep console clean
      if (req.url === '/favicon.ico') {
        res.writeHead(204); // No content
        res.end();
        return;
      }
      console.log('404:', req.url, '->', filePath);
      res.writeHead(404, {'Content-Type': 'text/html'});
      res.end('<h1>404 Not Found</h1><p>' + filePath + '</p>');
    } else {
      res.writeHead(200, {'Content-Type': ctx});
      res.end(content);
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