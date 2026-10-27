import http.server
import socketserver
import json
import os
import time
import queue
import puyal_unified_dispatch as dispatcher

PORT = 8765
WEB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "web_ui")
log_queue = queue.Queue()

class PuyalHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def do_GET(self):
        if self.path == "/api/events":
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.end_headers()
            
            # SSE Initial handshake
            self.wfile.write(b"data: [SSE Stream]: Channel Established (Zero-Leak Mode)\n\n")
            self.wfile.flush()

            while True:
                try:
                    msg = log_queue.get(timeout=2)
                    self.wfile.write(f"data: {msg}\n\n".encode('utf-8'))
                    self.wfile.flush()
                except queue.Empty:
                    # Heartbeat keep-alive
                    self.wfile.write(b": heartbeat\n\n")
                    self.wfile.flush()
                except Exception:
                    break
        else:
            super().do_GET()

    def do_POST(self):
        if self.path == "/api/dispatch":
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            payload = json.loads(post_data.decode('utf-8'))
            
            target = payload.get("target", "source_code.zip")
            route = payload.get("route", "ROUTE_CRYPT")
            
            log_queue.put(f"[SSE Stream]: Event Received -> Route: {route}")
            log_queue.put(f"[SSE Stream]: Target Artifact: {target}")
            
            success, result = dispatcher.execute_triage_and_dispatch(sample_target=target)
            
            log_queue.put(f"[SSE Stream]: Sentinel Quota Evaluated -> Status: PROCEED")
            log_queue.put(f"[SSE Stream]: Manifest Sealed -> Pushed to Ephemeral Runner")

            self.send_response(200 if success else 500)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            response = {"status": "SUCCESS" if success else "FAILED", "payload": result}
            self.wfile.write(json.dumps(response).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), PuyalHandler) as httpd:
        print(f"[*] Puyal SSE Stream Monitor active at http://localhost:{PORT}")
        httpd.serve_forever()
