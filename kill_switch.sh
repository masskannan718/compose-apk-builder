#!/bin/bash
echo "[!] ENGAGING PUYAL v5.3 ZERO-TRUST EMERGENCY KILL-SWITCH..."
fuser -k 8765/tcp >/dev/null 2>&1 || true
pkill -f "puyal_web_server.py" || true
pkill -f "puyal_telegram_bot.py" || true
echo "[-] Local Daemons Terminated."
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
echo "{\"event\": \"EMERGENCY_KILL_SWITCH_ENGAGED\", \"timestamp\": \"${TIMESTAMP}\"}" >> atomic_ledger.jsonl
echo "[✓] PUYAL SYSTEM SAFELY FROZEN. All active endpoints closed."
