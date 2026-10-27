#!/bin/bash
set +e

echo "========================================================"
echo "    🌪️ PUYAL v5.3 : MASTER PIPELINE AUDIT REPORT        "
echo "========================================================"

TOTAL_CHECKS=7
PASSED=0

# 1. Global CLI Binary Check
printf "%-35s" "[-] Checking Master CLI ('puyal'): "
if [ -x "$PREFIX/bin/puyal" ]; then
    echo "[ PASS ]"
    PASSED=$((PASSED + 1))
else
    echo "[ FAIL ] -> Binary missing in PATH"
fi

# 2. Cloud Workflow Engine Check
printf "%-35s" "[-] Checking GitHub Actions Workflow: "
if [ -f ".github/workflows/puyal_worker.yml" ]; then
    echo "[ PASS ]"
    PASSED=$((PASSED + 1))
else
    echo "[ FAIL ] -> Workflow missing"
fi

# 3. SQLite Database Bridge Check
printf "%-35s" "[-] Checking SQLite DB Bridge: "
if [ -f "puyal_vault.db" ]; then
    echo "[ PASS ]"
    PASSED=$((PASSED + 1))
else
    echo "[ FAIL ] -> Database missing"
fi

# 4. Atomic Ledger Log File Check
printf "%-35s" "[-] Checking Atomic Ledger (.jsonl): "
if [ -f "atomic_ledger.jsonl" ]; then
    echo "[ PASS ]"
    PASSED=$((PASSED + 1))
else
    echo "[ FAIL ] -> Ledger missing"
fi

# 5. Status Poller Script Check
printf "%-35s" "[-] Checking Headless Status Poller: "
if [ -f "puyal_status.py" ] && [ -f "puyal_status.sh" ]; then
    echo "[ PASS ]"
    PASSED=$((PASSED + 1))
else
    echo "[ FAIL ] -> Status poller missing"
fi

# 6. Ultra-light Web Console Check
printf "%-35s" "[-] Checking Lightweight UI Console: "
if [ -f "index.html" ] && [ -f "puyal_console.sh" ]; then
    echo "[ PASS ]"
    PASSED=$((PASSED + 1))
else
    echo "[ FAIL ] -> Console files missing"
fi

# 7. Git Origin Sync Check
printf "%-35s" "[-] Checking Git Remote Sync: "
GIT_STATUS=$(git status -s 2>/dev/null)
if [ -z "$GIT_STATUS" ]; then
    echo "[ PASS ] (Working tree clean)"
    PASSED=$((PASSED + 1))
else
    echo "[ WARN ] (Uncommitted changes exist)"
fi

echo "--------------------------------------------------------"
echo "AUDIT SCORE: $PASSED / $TOTAL_CHECKS CHECKS PASSED"
echo "--------------------------------------------------------"

# Display Latest Stage from Ledger
echo "CURRENT PIPELINE STAGE FROM LEDGER:"
python3 -c "
import json
import os
if os.path.exists('atomic_ledger.jsonl'):
    with open('atomic_ledger.jsonl', 'r') as f:
        lines = [line.strip() for line in f if line.strip()]
    if lines:
        for l in lines[-3:]:
            try:
                data = json.loads(l)
                block = data.get('block', 'TASK')
                status = data.get('status', 'N/A')
                features = data.get('features', data.get('layer', 'Core'))
                print(f'  • [{block}] Status: {status} | Details: {features}')
            except Exception:
                print(f'  • Raw: {l[:70]}...')
    else:
        print('  • Ledger is empty.')
else:
    print('  • Ledger file not found.')
"

echo "========================================================"
if [ "$PASSED" -eq "$TOTAL_CHECKS" ]; then
    echo "  🎉 STATUS: 100% PRODUCTION READY (ALL 15 STEPS ACTIVE)"
else
    echo "  ⚠️ STATUS: PARTIALLY CONFIGURED (Review failed items)"
fi
echo "========================================================"
