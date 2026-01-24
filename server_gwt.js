const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, 'starsector-gwt', 'target', 'starsector-gwt-0.97a-RC11');
const port = 8085;

const server = http.createServer((req, res) => {
  // 🛡️ Required for SharedArrayBuffer support in modern browsers
  res.setHeader('Cross-Origin-Opener-Policy', 'same-origin');
  res.setHeader('Cross-Origin-Embedder-Policy', 'require-corp');
  
  let url = req.url.split('?')[0];
  let filePath = path.join(root, url === '/' ? 'index.html' : url);
  
  const ext = path.extname(filePath);
  let ctx = 'text/html';
  if(ext === '.js') ctx = 'text/javascript';
  if(ext === '.css') ctx = 'text/css';
  if(ext === '.wasm') ctx = 'application/wasm';
  if(ext === '.jar') ctx = 'application/java-archive';
  if(ext === '.json') ctx = 'application/json';
  
  fs.readFile(filePath, (err, content) => {
    if(err) {
      if (req.url === '/favicon.ico') {
        res.writeHead(204);
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
  console.log('🎮 GWT Port server running at http://localhost:' + port);
});