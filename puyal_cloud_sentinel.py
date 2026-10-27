import sys
import json

HARD_CAP_PERCENT = 70.0

def evaluate_quota(used_minutes, total_allocated=2000):
    usage_pct = (used_minutes / total_allocated) * 100.0
    allowed = usage_pct < HARD_CAP_PERCENT
    return {
        "used_minutes": used_minutes,
        "allocated_minutes": total_allocated,
        "usage_percentage": round(usage_pct, 2),
        "hard_cap_threshold": HARD_CAP_PERCENT,
        "dispatch_allowed": allowed,
        "action": "PROCEED" if allowed else "HALT_GATEKEEPER_TRIGGERED"
    }

if __name__ == "__main__":
    # டெஸ்ட் செக்: 150 நிமிட பயன்பாடு உள்ளதாக தணிக்கை
    telemetry = evaluate_quota(used_minutes=150)
    print(json.dumps(telemetry))
