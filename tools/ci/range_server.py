#!/usr/bin/env python3
import argparse
import os
import re
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class RangeRequestHandler(SimpleHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def end_headers(self):
        self.send_header('Accept-Ranges', 'bytes')
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()

    def send_head(self):
        path = self.translate_path(self.path)
        if os.path.isdir(path):
            return super().send_head()
        try:
            f = open(path, 'rb')
        except OSError:
            self.send_error(404, 'File not found')
            return None
        try:
            fs = os.fstat(f.fileno())
            size = fs.st_size
            ctype = self.guess_type(path)
            range_header = self.headers.get('Range')
            if range_header:
                m = re.fullmatch(r'bytes=(\d*)-(\d*)', range_header.strip())
                if not m:
                    self.send_error(416, 'Invalid Range')
                    f.close()
                    return None
                first, last = m.groups()
                if first == '' and last == '':
                    self.send_error(416, 'Invalid Range')
                    f.close()
                    return None
                if first == '':
                    suffix = int(last)
                    start = max(0, size - suffix)
                    end = size - 1
                else:
                    start = int(first)
                    end = int(last) if last else size - 1
                if start >= size or start < 0 or end < start:
                    self.send_response(416)
                    self.send_header('Content-Range', f'bytes */{size}')
                    self.send_header('Content-Length', '0')
                    self.end_headers()
                    f.close()
                    return None
                end = min(end, size - 1)
                self.send_response(206)
                self.send_header('Content-type', ctype)
                self.send_header('Content-Range', f'bytes {start}-{end}/{size}')
                self.send_header('Content-Length', str(end - start + 1))
                self.send_header('Last-Modified', self.date_time_string(fs.st_mtime))
                self.end_headers()
                f.seek(start)
                self._range = (start, end)
                return f

            self.send_response(200)
            self.send_header('Content-type', ctype)
            self.send_header('Content-Length', str(size))
            self.send_header('Last-Modified', self.date_time_string(fs.st_mtime))
            self.end_headers()
            self._range = None
            return f
        except Exception:
            f.close()
            raise

    def copyfile(self, source, outputfile):
        rng = getattr(self, '_range', None)
        if not rng:
            return super().copyfile(source, outputfile)
        remaining = rng[1] - rng[0] + 1
        while remaining > 0:
            chunk = source.read(min(1024 * 1024, remaining))
            if not chunk:
                break
            outputfile.write(chunk)
            remaining -= len(chunk)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', required=True)
    ap.add_argument('--port', type=int, required=True)
    args = ap.parse_args()
    os.chdir(Path(args.root).resolve())
    server = ThreadingHTTPServer(('127.0.0.1', args.port), RangeRequestHandler)
    print(f'serving {args.root} on http://127.0.0.1:{args.port}', flush=True)
    server.serve_forever()
