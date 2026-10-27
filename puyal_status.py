#!/usr/bin/env python3
import sqlite3
import os

DB_FILE = "puyal_vault.db"
LEDGER_FILE = "atomic_ledger.jsonl"

print("========================================================")
print("       PUYAL v5.3 : PIPELINE STATUS & LEDGER STREAM     ")
print("========================================================")

if os.path.exists(DB_FILE):
    conn = sqlite3.connect(DB_FILE)
    cur = conn.cursor()
    cur.execute("SELECT task_id, file_name, route, status, timestamp FROM transactions ORDER BY timestamp DESC LIMIT 5")
    rows = cur.fetchall()
    if rows:
        header = "%-22s | %-20s | %-18s | %-12s" % ("TASK ID", "FILE", "ROUTE", "STATUS")
        print(header)
        print("-" * 80)
        for r in rows:
            line = "%-22s | %-20s | %-18s | %-12s" % (str(r[0]), str(r[1]), str(r[2]), str(r[3]))
            print(line)
    else:
        print("No transactions recorded in database yet.")
    conn.close()
else:
    print("[!] Local database not found. Showing latest raw ledger:")
    if os.path.exists(LEDGER_FILE):
        with open(LEDGER_FILE, "r") as f:
            lines = f.readlines()[-5:]
            for l in lines:
                print(l.strip())
    else:
        print("Ledger is empty.")
print("========================================================")
