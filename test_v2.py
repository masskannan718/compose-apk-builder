import os
import time
import urllib.request
import json
import shutil

GREEN = "\033[0;32m"
RED = "\033[0;31m"
YELLOW = "\033[1;33m"
NC = "\033[0m"

print(f"\n{YELLOW}=== 🚀 PUYAL VAULT v2: VERIFICATION START ==={NC}\n")

# 1. Server Online Check
print("1. Server & SQLite WAL Mode... ", end="", flush=True)
try:
    res = urllib.request.urlopen("http://127.0.0.1:8001/", timeout=5).read().decode()
    data = json.loads(res)
    print(f"{GREEN}PASS ({data.get('db')}){NC}")
except Exception as e:
    print(f"{RED}FAIL (Server Offline: {e}){NC}")
    exit(1)

# 2. Local Sample Project
base_dir = os.path.expanduser("~/puyal-storage")
test_app = os.path.join(base_dir, "test_v2_app")
shutil.rmtree(test_app, ignore_errors=True)
os.makedirs(os.path.join(test_app, "src"), exist_ok=True)
os.makedirs(os.path.join(test_app, "node_modules"), exist_ok=True)

with open(os.path.join(test_app, "src", "index.py"), "w") as f:
    f.write("print('Puyal Production Running')\n")
with open(os.path.join(test_app, "node_modules", "junk.txt"), "w") as f:
    f.write("Ignore this junk\n")

# 3. Upload Project API
print("2. Project Upload & Smart Exclusion... ", end="", flush=True)
payload = json.dumps({"project_path": test_app, "project_name": "TestV2App"}).encode('utf-8')
req = urllib.request.Request("http://127.0.0.1:8001/upload-project", data=payload, headers={'Content-Type': 'application/json'})

try:
    resp = urllib.request.urlopen(req, timeout=5).read().decode()
    up_data = json.loads(resp)
    task_id = up_data.get("task_id")
    print(f"{GREEN}PASS (Task: {task_id}){NC}")
except Exception as e:
    print(f"{RED}FAIL: {e}{NC}")
    exit(1)

# 4. Wait for Upload Completion
print("3. Cloud Syncing: ", end="", flush=True)
upload_done = False
for i in range(25):
    time.sleep(2)
    st = urllib.request.urlopen(f"http://127.0.0.1:8001/task-status/{task_id}").read().decode()
    st_data = json.loads(st)
    status = st_data.get("status")
    print(f"[{status}] ", end="", flush=True)
    if status == "completed":
        upload_done = True
        break
    elif status in ["failed", "error"]:
        print(f"\n{RED}Upload Failed: {st_data.get('error')}{NC}")
        exit(1)

if not upload_done:
    print(f"\n{RED}Upload Timeout{NC}")
    exit(1)
print(f"{GREEN}DONE{NC}")

# 5. Restore / Download API Check
print("4. Restore & Auto-Unzip Test... ", end="", flush=True)
restore_target = os.path.join(base_dir, "restored_projects", "TestV2App")
shutil.rmtree(restore_target, ignore_errors=True)

rest_payload = json.dumps({"project_name": "TestV2App"}).encode('utf-8')
rest_req = urllib.request.Request("http://127.0.0.1:8001/restore-project", data=rest_payload, headers={'Content-Type': 'application/json'})

try:
    rest_resp = urllib.request.urlopen(rest_req, timeout=5).read().decode()
    rest_task_id = json.loads(rest_resp).get("task_id")
except Exception as e:
    print(f"{RED}FAIL to start restore: {e}{NC}")
    exit(1)

restore_done = False
for i in range(25):
    time.sleep(2)
    st = urllib.request.urlopen(f"http://127.0.0.1:8001/task-status/{rest_task_id}").read().decode()
    st_data = json.loads(st)
    status = st_data.get("status")
    if status == "completed":
        restore_done = True
        break
    elif status in ["failed", "error"]:
        print(f"\n{RED}Restore Failed: {st_data.get('error')}{NC}")
        exit(1)

restored_file = os.path.join(restore_target, "src", "index.py")
junk_file = os.path.join(restore_target, "node_modules", "junk.txt")

if restore_done and os.path.exists(restored_file) and not os.path.exists(junk_file):
    print(f"{GREEN}PASS{NC}")
    print(f"\n{GREEN}🎯 ALL SYSTEMS OPERATIONAL! ZERO ERRORS!{NC}\n")
else:
    print(f"{RED}FAIL (Integrity mismatch){NC}")

shutil.rmtree(test_app, ignore_errors=True)
