import os
import shutil
import subprocess
import tempfile
from fastapi import FastAPI, UploadFile, File, HTTPException

app = FastAPI(title="Puyal Storage Engine v5.1")

REMOTE_NAME = "puyal_vault"
PARENT_FOLDER_ID = "132pNBbcfhwHgq-mPPZ2SAMrr3UP5F7l3"
CONFIG_FILE = "/data/data/com.termux/files/home/.config/rclone/rclone.conf"

@app.get("/")
def health():
    return {"status": "ONLINE", "blueprint": "v5.1"}

@app.post("/upload-to-drive")
async def upload_to_drive(file: UploadFile = File(...)):
    temp_dir = tempfile.mkdtemp()
    temp_path = os.path.join(temp_dir, os.path.basename(file.filename or "payload.txt"))
    try:
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # நீங்கள் ஏற்கனவே CLI-ல் வெற்றிகரமாக இயக்கிய அதே rclone copy கமாண்ட்
        cmd = [
            "rclone", "copy",
            temp_path,
            f"{REMOTE_NAME}:",
            "--config", CONFIG_FILE,
            "--drive-root-folder-id", PARENT_FOLDER_ID
        ]

        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            err = res.stderr.strip() or res.stdout.strip()
            raise HTTPException(status_code=500, detail=f"Rclone Failed: {err}")

        return {"status": "SUCCESS", "file_name": os.path.basename(temp_path)}

    except HTTPException as he:
        raise he
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        if file:
            await file.close()
        shutil.rmtree(temp_dir, ignore_errors=True)
