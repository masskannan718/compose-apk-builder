#!/usr/bin/env python3
import http.server
import json
import hmac
import hashlib
import os
import sys

PORT = 8002
SECRET_KEY = os.environ.get("PUYAL_DISPATCH_SECRET", "puyal_secret_vault_key_v53")

class DispatcherHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/dispatch":
            self.send_response(404)
            self.end_headers()
            return

        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length)
        auth_sig = self.headers.get("X-Puyal-Signature", "")

        # [Step 02]: HMAC-SHA256 Signature Verification
        computed_sig = hmac.new(SECRET_KEY.encode(), post_data, hashlib.sha256).hexdigest()
        if not hmac.compare_digest(computed_sig, auth_sig):
            self.send_response(403)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"status": "REJECTED", "reason": "Invalid Signature"}')
            return

        # [Step 03]: Fast Dispatch Handshake & Intent Parsing (<10ms)
        try:
            payload = json.loads(post_data.decode("utf-8"))
            filename = payload.get("filename", "unknown")
            route = "ROUTE_A_GDRIVE" if filename.endswith((".apk", ".zip", ".tar.gz", ".mp4")) else "ROUTE_B_MEGA"
            
            response = {
                "status": "ACCEPTED",
                "intent": route,
                "target": filename,
                "msg": "Payload signature verified. Ready for ephemeral dispatch."
            }
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response).encode("utf-8"))
        except Exception:
            self.send_response(400)
            self.end_headers()

    def log_message(self, format, *args):
        return

if __name__ == "__main__":
    server = http.server.HTTPServer(("127.0.0.1", PORT), DispatcherHandler)
    server.handle_request()
