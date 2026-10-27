import time
import subprocess
import urllib.request

def is_alive():
    try:
        urllib.request.urlopen("http://127.0.0.1:8001/", timeout=2)
        return True
    except Exception:
        return False

while True:
    if not is_alive():
        with open("server.log", "a") as log_f:
            subprocess.Popen(
                ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"],
                stdout=log_f,
                stderr=subprocess.STDOUT
            )
    time.sleep(10)
