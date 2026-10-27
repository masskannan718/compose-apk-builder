import os
import json

FREE_TIER_TOTAL_MINUTES = 2000
HARD_CAP_PERCENTAGE = 70.0
HARD_CAP_MINUTES = (FREE_TIER_TOTAL_MINUTES * HARD_CAP_PERCENTAGE) / 100.0  # 1400 Mins

def evaluate_quota(used_minutes: float = 120.0):
    usage_pct = round((used_minutes / FREE_TIER_TOTAL_MINUTES) * 100, 2)
    dispatch_allowed = used_minutes <= HARD_CAP_MINUTES
    
    return {
        "used_minutes": used_minutes,
        "total_limit": FREE_TIER_TOTAL_MINUTES,
        "hard_cap_threshold": HARD_CAP_MINUTES,
        "usage_percentage": usage_pct,
        "dispatch_allowed": dispatch_allowed,
        "action": "PROCEED" if dispatch_allowed else "HALT_QUOTA_EXCEEDED"
    }

if __name__ == "__main__":
    status = evaluate_quota(used_minutes=140.0)
    print(f"  [✓] Quota Evaluation: {status['action']}")
    print(f"  [✓] Usage: {status['used_minutes']} / {status['total_limit']} mins ({status['usage_percentage']}%)")
    print(f"  [✓] Sentinel Gate Status: {'ARMED_AND_SAFE' if status['dispatch_allowed'] else 'BLOCKED'}")
