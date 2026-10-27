import json

print("=== Google OAuth Client Creator ===")
client_id = input("👉 உங்களது Client ID-ஐ இங்கே டைப் செய்யவும்: ").strip()
client_secret = input("👉 உங்களது Client Secret-ஐ இங்கே டைப் செய்யவும்: ").strip()

data = {
    "installed": {
        "client_id": client_id,
        "project_id": "puyal-storage-bot",
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "client_secret": client_secret,
        "redirect_uris": ["urn:ietf:wg:oauth:2.0:oob", "http://localhost"]
    }
}

with open("client_secret.json", "w") as f:
    json.dump(data, f, indent=4)

print("\n✅ SUCCESS: 'client_secret.json' successfully created in ~/puyal-storage!")
