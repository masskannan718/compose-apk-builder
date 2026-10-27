import http.server
import socketserver
import json
import os
import puyal_unified_dispatch as dispatcher

PORT = 8765
WEB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "web_ui")

class PuyalHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def do_POST(self):
        if self.path == "/api/dispatch":
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            payload = json.loads(post_data.decode('utf-8'))
            
            target = payload.get("target", "manual_trigger.zip")
            success, result = dispatcher.execute_triage_and_dispatch(sample_target=target)
            
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
        print(f"[*] Puyal Web UI Console active at http://localhost:{PORT}")
        httpd.serve_forever()
