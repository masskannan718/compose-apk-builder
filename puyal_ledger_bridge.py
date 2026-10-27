#!/usr/bin/env python3
import json
import sqlite3
import os
import sys
from datetime import datetime

LEDGER_FILE = "atomic_ledger.jsonl"
DB_FILE = "puyal_vault.db"
MAX_LINES = 500

# [Step 09]: Initialize Ultra-light SQLite Bridge
conn = sqlite3.connect(DB_FILE)
cur = conn.cursor()
cur.execute("""
CREATE TABLE IF NOT EXISTS transactions (
    task_id TEXT PRIMARY KEY,
    file_name TEXT,
    route TEXT,
    checksum TEXT,
    destination TEXT,
    status TEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
)
""")
conn.commit()

# Sync unindexed JSONL entries into SQLite
if os.path.exists(LEDGER_FILE):
    with open(LEDGER_FILE, "r") as f:
        lines = f.readlines()
    
    for line in lines:
        try:
            data = json.loads(line.strip())
            task_id = data.get("task_id")
            if task_id:
                cur.execute("""
                INSERT OR IGNORE INTO transactions (task_id, file_name, route, checksum, destination, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, (
                    task_id,
                    data.get("file", ""),
                    data.get("route", ""),
                    data.get("checksum", ""),
                    data.get("destination", ""),
                    data.get("status", "")
                ))
        except Exception:
            continue
    conn.commit()

    # [Step 08]: Zero-Footprint Log Rotator
    if len(lines) > MAX_LINES:
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        archive_name = "ledger_archive_" + ts + ".jsonl"
        with open(archive_name, "w") as af:
            af.writelines(lines[:-100])
        with open(LEDGER_FILE, "w") as lf:
            lf.writelines(lines[-100:])
        print(f"[✓] Log Rotator: Archived {len(lines) - 100} lines to {archive_name}")

conn.close()
print("[✓] SQLite Ledger Bridge synced seamlessly.")
