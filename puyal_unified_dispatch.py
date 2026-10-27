import sys
import json
import os
import puyal_cloud_sentinel as sentinel
import puyal_intent_classifier as classifier

def execute_triage_and_dispatch(sample_target="source_code_backup.zip"):
    print("[-] 1. Evaluating Sentinel Quota...")
    quota_status = sentinel.evaluate_quota(used_minutes=120)
    print(f"    Sentinel Status: {quota_status['action']} (Usage: {quota_status['usage_percentage']}%)")
    
    if not quota_status["dispatch_allowed"]:
        print("[!] Gatekeeper Triggered: 70% threshold exceeded. Halting dispatch.")
        return False, "QUOTA_EXCEEDED"

    print("[-] 2. Classifying Payload Intent...")
    route_meta = classifier.classify_intent(sample_target)
    print(f"    Payload Target: {route_meta['file']} -> Route: {route_meta['target_route']}")
    print(f"    Destination: {route_meta['storage_destination']}")

    print("[-] 3. Preparing Workflow Dispatch Payload...")
    dispatch_payload = {
        "event_type": "puyal_trigger",
        "client_payload": {
            "route": "ROUTE_CRYPT" if "CRYPT" in route_meta["target_route"] else "ROUTE_BUILD",
            "file": route_meta["file"],
            "priority": route_meta["priority"]
        }
    }
    
    with open("puyal_dispatch_manifest.json", "w") as f:
        json.dump(dispatch_payload, f, indent=2)
        
    print("    [✓] Dispatch Manifest Generated: puyal_dispatch_manifest.json")
    return True, dispatch_payload

if __name__ == "__main__":
    success, res = execute_triage_and_dispatch()
    if success:
        sys.exit(0)
    else:
        sys.exit(1)
