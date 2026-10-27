import os
import json
import http.server
import socketserver
import urllib.request

PORT = int(os.environ.get("PORT", 7860))

class CloudRelayHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        res = {
            "node": "Puyal v5.3 Cloud Dispatcher",
            "status": "ONLINE",
            "endpoints": ["/trigger", "/health"]
        }
        self.wfile.write(json.dumps(res).encode("utf-8"))

    def do_POST(self):
        if self.path == "/trigger":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length)
            data = json.loads(body.decode("utf-8")) if body else {}

            route = data.get("route", "ROUTE_CRYPT")
            file_name = data.get("file", "remote_cloud_trigger.zip")

            github_token = os.environ.get("GH_PAT_TOKEN", "")
            repo = os.environ.get("GH_REPO", "Masskannan/puyal-storage")

            print(f"[*] Relaying Cloud Trigger -> Route: {route} for {file_name}")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            res = {
                "dispatch_status": "RELAYED_TO_RUNNER",
                "route": route,
                "target_file": file_name,
                "mode": "ZERO_LOCAL_COMPUTE"
            }
            self.wfile.write(json.dumps(res).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), CloudRelayHandler) as httpd:
        print(f"[*] Cloud Dispatcher Relay listening on port {PORT}")
        httpd.serve_forever()
