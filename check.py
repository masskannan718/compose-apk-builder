import os
import time
import subprocess
import urllib.request
import json

print("\n🔍 1. சர்வர் இயங்குகிறதா எனப் பார்க்கிறது...")
try:
    urllib.request.urlopen("http://127.0.0.1:8001/", timeout=2)
    print("✅ சர்வர் ஏற்கனவே ஆன்லைனில் உள்ளது!")
except Exception:
    print("⚠️ சர்வர் ஆஃப் ஆகியுள்ளது! இப்போதே ஆன் செய்யப்படுகிறது...")
    log_file = open("server.log", "w")
    subprocess.Popen(
        ["nohup", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"],
        stdout=log_file,
        stderr=subprocess.STDOUT
    )
    time.sleep(4)

# 2. டெஸ்ட் ஃபைல் உருவாக்குதல்
test_file = "puyal_live_check.txt"
with open(test_file, "w") as f:
    f.write("Puyal 5TB Check at " + str(time.ctime()) + "\n")

print("📤 2. ஃபைல் அப்லோட் செய்யப்படுகிறது...")
res = subprocess.run(
    ["curl", "-s", "-X", "POST", "http://127.0.0.1:8001/upload-to-drive", "-F", "file=@" + test_file],
    capture_output=True,
    text=True
)

try:
    data = json.loads(res.stdout)
    task_id = data.get("task_id")
    if not task_id:
        raise ValueError("Task ID missing")
    print("✅ கியூவில் ஏறிவிட்டது! Task ID:", task_id)
except Exception:
    print("❌ அப்லோட் எரர்:", res.stdout)
    if os.path.exists("server.log"):
        with open("server.log", "r") as lf:
            print("📋 Server Log:\n", lf.read()[-400:])
    if os.path.exists(test_file):
        os.remove(test_file)
    exit(1)

# 3. 5TB டிரைவில் ஏறும் வரை காத்திருந்து சரிபார்த்தல்
print("⏳ 3. கூகுள் டிரைவ் அப்லோட் நிலை சரிபார்க்கப்படுகிறது...")
for i in range(12):
    time.sleep(2)
    st_res = urllib.request.urlopen("http://127.0.0.1:8001/task-status/" + task_id).read().decode()
    st_data = json.loads(st_res)
    status = st_data.get("status")
    err_msg = st_data.get("error")
    print(f"   நிலை [{i+1}]: {status}")
    
    if status == "completed":
        break
    elif status in ["failed", "error"]:
        print("❌ டிரைவ் எரர்:", err_msg)
        if os.path.exists(test_file):
            os.remove(test_file)
        exit(1)

# 4. Rclone மூலம் நேரடி உறுதிப்படுத்தல்
remote_check = subprocess.run(
    ["rclone", "ls", "puyal_vault:PuyalUploads/Files"],
    capture_output=True,
    text=True
).stdout

if task_id in remote_check:
    print("\n🎯 100% SUCCESS! 5TB டிரைவில் ஃபைல் பத்திரமாக ஏறிவிட்டது!")
    for line in remote_check.splitlines():
        if task_id in line:
            print("   📄 Drive File:", line.strip())
else:
    print("\n⚠️ டிரைவில் இன்னும் ஃபைல் தெரியவில்லை.")

if os.path.exists(test_file):
    os.remove(test_file)
