import sys
import json
import os

BUILD_EXTENSIONS = {".gradle", ".java", ".kt", ".xml", ".kt", ".c", ".cpp"}
CRYPT_EXTENSIONS = {".db", ".sqlite", ".sql", ".enc", ".vault", ".key", ".txt", ".json"}

def classify_intent(filename):
    _, ext = os.path.splitext(filename.lower())
    if ext in BUILD_EXTENSIONS or "gradle" in filename:
        return {
            "file": filename,
            "target_route": "ROUTE_BUILD_RUNNER",
            "storage_destination": "puyal_drive:build_artifacts",
            "priority": "HIGH"
        }
    else:
        return {
            "file": filename,
            "target_route": "ROUTE_CRYPT_VAULT",
            "storage_destination": "puyal_crypt:secure_blobs",
            "priority": "STANDARD"
        }

if __name__ == "__main__":
    sample_a = classify_intent("build.gradle")
    sample_b = classify_intent("puyal_vault.db")
    print(json.dumps({"classifier": "puyal_intent_classifier", "test_a": sample_a, "test_b": sample_b}))
