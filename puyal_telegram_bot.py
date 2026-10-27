import os
import json
import time
import urllib.request
import puyal_unified_dispatch as dispatcher

TG_TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "")

def handle_telegram_polling():
    if not TG_TOKEN:
        print("[!] Telegram Bot Token not set. Bot Poller in standby mode.")
        return

    offset = 0
    print("[*] Puyal Two-Way Interactive Telegram Bot Online...")
    while True:
        try:
            url = f"https://api.telegram.org/bot{TG_TOKEN}/getUpdates?offset={offset}&timeout=10"
            req = urllib.request.urlopen(url, timeout=15)
            updates = json.loads(req.read().decode('utf-8'))
            for u in updates.get("result", []):
                offset = u["update_id"] + 1
                msg = u.get("message", {})
                chat_id = msg.get("chat", {}).get("id")
                text = msg.get("text", "").strip()

                if text == "/build":
                    dispatcher.execute_triage_and_dispatch("android_app.apk")
                    send_reply(chat_id, "🌪️ <b>Route A (Build) Triggered!</b> Dispatched to GitHub Actions VM.")
                elif text == "/backup":
                    dispatcher.execute_triage_and_dispatch("source_master.zip")
                    send_reply(chat_id, "🔐 <b>Route B (Crypt) Triggered!</b> Packaging Git to MEGA Crypt.")
                elif text == "/status":
                    send_reply(chat_id, "⚡ <b>Puyal v5.3 OS:</b> 100% ONLINE. Zero-Compute Mode Active.")
        except Exception:
            time.sleep(5)

def send_reply(chat_id, text):
    try:
        url = f"https://api.telegram.org/bot{TG_TOKEN}/sendMessage"
        data = json.dumps({"chat_id": chat_id, "text": text, "parse_mode": "HTML"}).encode('utf-8')
        req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
        urllib.request.urlopen(req, timeout=10)
    except Exception:
        pass

if __name__ == "__main__":
    handle_telegram_polling()
