import os
import glob
import subprocess
import json

def sanitize_ephemeral_workspace(target_pattern="/tmp/puyal_payload_*"):
    purged_count = 0
    for target in glob.glob(target_pattern):
        try:
            subprocess.run(["shred", "-u", "-z", target], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            purged_count += 1
        except Exception:
            if os.path.exists(target):
                os.remove(target)
                purged_count += 1
    return {
        "engine": "AUTO_SANITIZER_v2",
        "pattern": target_pattern,
        "purged_ephemeral_count": purged_count,
        "status": "SANITIZED_CLEAN"
    }

if __name__ == "__main__":
    res = sanitize_ephemeral_workspace()
    print(json.dumps(res))
