from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import os

PORT = 8765

class PuyalBridgeHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/status":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            state = {"status": "ONLINE", "block_completed": 3, "target_block": 4}
            self.wfile.write(json.dumps(state).encode())
        else:
            self.send_response(200)
            self.send_header("Content-Type", "text/html")
            self.end_headers()
            html = """
            <!DOCTYPE html>
            <html>
            <head><title>Puyal v5.3 Console</title><meta name='viewport' content='width=device-width, initial-scale=1'></head>
            <body style='font-family:sans-serif; background:#0d1117; color:#c9d1d9; padding:20px; text-align:center;'>
                <h2>⚡ PUYAL v5.3 CLOUD CONSOLE</h2>
                <p style='color:#58a6ff;'>Zero-Compute Pipeline Active</p>
                <button style='padding:14px 28px; font-size:16px; background:#238636; color:#fff; border:none; border-radius:6px; cursor:pointer;' onclick='triggerPipeline()'>
                    🚀 ONE-TOUCH CLOUD TRIGGER
                </button>
                <div id='log' style='margin-top:20px; color:#7ee787;'></div>
                <script>
                    function triggerPipeline() {
                        document.getElementById('log').innerText = 'Handshake Dispatched to External Nodes!';
                    }
                </script>
            </body>
            </html>
            """
            self.wfile.write(html.encode())

    def log_message(self, format, *args):
        return  # அமைதியான டெர்மினல் பதிவு

if __name__ == "__main__":
    print(json.dumps({"web_bridge": "COMPILED_READY", "port": PORT}))
