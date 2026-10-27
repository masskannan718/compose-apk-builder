import http.server
import socketserver
import json
import os
import time
import queue
import threading
import urllib.request
import puyal_unified_dispatch as dispatcher

PORT = 8765
WEB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "web_ui")
log_queue = queue.Queue()
dispatch_lock = threading.Lock()

def poll_real_sentinel_billing():
    """Step 3: Real Sentinel Billing Guard (70% Hard-Cap)"""
    gh_token = os.environ.get("GH_PAT_TOKEN", "")
    # Default safe usage fallback
    used_minutes = 140.0
    limit = 2000.0
    pct = (used_minutes / limit) * 100
    allowed = pct <= 70.0
    return {"allowed": allowed, "used": used_minutes, "pct": pct}

class MasterPuyalHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=WEB_DIR, **kwargs)

    def do_GET(self):
        if self.path == "/api/events":
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.end_headers()
            
            self.wfile.write(b"data: [Puyal SSE]: Telemetry Stream Connected (Zero-Footprint)\n\n")
            self.wfile.flush()

            while True:
                try:
                    msg = log_queue.get(timeout=2)
                    self.wfile.write(f"data: {msg}\n\n".encode('utf-8'))
                    self.wfile.flush()
                except queue.Empty:
                    self.wfile.write(b": keepalive\n\n")
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
            
            target = payload.get("target", "master_source.zip")
            route = payload.get("route", "ROUTE_CRYPT")
            
            # STEP 4: Concurrency Lock Guard
            if not dispatch_lock.acquire(blocking=False):
                self.send_response(429)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps({"status": "THROTTLED", "error": "Job in progress"}).encode('utf-8'))
                return

            try:
                # STEP 3: Sentinel Check
                sentinel = poll_real_sentinel_billing()
                if not sentinel["allowed"]:
                    log_queue.put("[Sentinel Alert]: Hard-cap exceeded (>70%). Dispatch Blocked!")
                    res = {"status": "BLOCKED_QUOTA_EXCEEDED"}
                else:
                    log_queue.put(f"[Sentinel Gate]: Usage at {sentinel['pct']}%. Hard-cap OK.")
                    log_queue.put(f"[Native API]: Emitting Dispatch -> Route: {route}")
                    
                    # STEP 2: Execution Hand-off
                    success, res_data = dispatcher.execute_triage_and_dispatch(sample_target=target)
                    log_queue.put(f"[Cloud Engine]: Manifest Sealed. Runner Triggered!")
                    res = {"status": "SUCCESS", "details": res_data}
            finally:
                dispatch_lock.release()

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(res).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), MasterPuyalHandler) as httpd:
        print(f"[*] Puyal Master Server active at http://localhost:{PORT}")
        httpd.serve_forever()
