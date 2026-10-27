import os
import sys
import json
import urllib.request
import urllib.error

def dispatch_event(event_type, client_payload):
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    repo = os.environ.get("GITHUB_REPOSITORY", "Masskannan/puyal-storage")
    
    if not token:
        # லோக்கல் Git கன்ஃபிக் அல்லது டோக்கன் ஃபைலிலிருந்து படித்தல்
        token_path = os.path.expanduser("~/.gh_token")
        if os.path.exists(token_path):
            with open(token_path, "r") as f:
                token = f.read().strip()
                
    url = f"https://api.github.com/repos/{repo}/dispatches"
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}" if token else "",
        "User-Agent": "Puyal-Dispatcher-v5.3"
    }
    
    payload = {
        "event_type": event_type,
        "client_payload": client_payload
    }
    
    req = urllib.request.Request(url, data=json.dumps(payload).encode(), headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status in (200, 204)
    except urllib.error.HTTPError as e:
        return False, e.code
    except Exception as e:
        return False, str(e)

if __name__ == "__main__":
    test_payload = {"source": "termux_local", "intent": "TEST_PIPELINE"}
    print(json.dumps({"engine": "puyal_event_dispatcher", "status": "COMPILED_READY"}))
