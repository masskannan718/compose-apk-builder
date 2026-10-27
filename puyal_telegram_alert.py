import os
import json
import urllib.request
import urllib.error

def send_delivery_alert(message, bot_token=None, chat_id=None):
    token = bot_token or os.environ.get("TELEGRAM_BOT_TOKEN")
    target_id = chat_id or os.environ.get("TELEGRAM_CHAT_ID")
    
    if not token or not target_id:
        return {"status": "MOCKED_LOGGED", "reason": "TOKEN_OR_CHAT_ID_NOT_SET", "message": message}
        
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    payload = {
        "chat_id": target_id,
        "text": f"⚡ [PUYAL v5.3 ALERT]\n{message}",
        "parse_mode": "Markdown"
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode(), headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return {"status": "DELIVERED", "code": resp.status}
    except Exception as e:
        return {"status": "DELIVERY_FAILED", "error": str(e)}

if __name__ == "__main__":
    res = send_delivery_alert("Pipeline verified: Build Complete & Encrypted Storage Synchronized.")
    print(json.dumps({"telegram_alert_engine": res["status"]}))
