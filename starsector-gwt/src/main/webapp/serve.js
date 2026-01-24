const http = require('http');
const fs = require('fs');
const path = require('path');

const port = 8088;

http.createServer((req, res) => {
    let url = req.url.split('?')[0];
    if (url === '/') url = '/index.html';
    const filePath = path.join(__dirname, url);
    
    const ext = path.extname(filePath);
    const mimes = {
        '.html': 'text/html',
        '.js': 'text/javascript',
        '.css': 'text/css',
        '.json': 'application/json',
        '.jar': 'application/java-archive',
        '.wasm': 'application/wasm',
        '.gif': 'image/gif'
    };

    fs.readFile(filePath, (err, content) => {
        if (err) {
            res.writeHead(404);
            res.end('404');
        } else {
            res.writeHead(200, { 
                'Content-Type': mimes[ext] || 'application/octet-stream',
                'Cross-Origin-Opener-Policy': 'same-origin',
                'Cross-Origin-Embedder-Policy': 'require-corp'
            });
            res.end(content, 'utf-8');
        }
    });
}).listen(port, '0.0.0.0', () => {
    console.log(`Server up on ${port}`);
});
