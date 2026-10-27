import os
from google_auth_oauthlib.flow import InstalledAppFlow

SCOPES = ['https://www.googleapis.com/auth/drive']
CLIENT_SECRET_FILE = 'client_secret.json'
TOKEN_FILE = 'token.json'

def generate_token():
    if not os.path.exists(CLIENT_SECRET_FILE):
        print("❌ ERROR: 'client_secret.json' not found! Please place your OAuth Client JSON file in ~/puyal-storage/")
        return
    
    # run_console முறையைப் பயன்படுத்துவது டெர்மக்ஸுக்கு மிகச்சரியாக வேலை செய்யும்
    flow = InstalledAppFlow.from_client_secrets_file(CLIENT_SECRET_FILE, SCOPES)
    flow.redirect_uri = 'urn:ietf:wg:oauth:2.0:oob'
    
    auth_url, _ = flow.authorization_url(prompt='consent')
    
    print("\n==================================================")
    print("🔗 கீழே உள்ள லிங்கை காப்பி செய்து உங்கள் போன் பிரவுசரில் (Chrome) ஓபன் செய்யவும்:")
    print("==================================================\n")
    print(auth_url)
    print("\n==================================================")
    
    code = input("🔑 பிரவுசரில் அனுமதி அளித்தபின் கிடைக்கும் கோடை (Authorization Code) இங்கே டைப் செய்யவும்: ").strip()
    
    flow.fetch_token(code=code)
    creds = flow.credentials
    
    with open(TOKEN_FILE, 'w') as token:
        token.write(creds.to_json())
    print("\n✅ SUCCESS: token.json created successfully! Now your storage is ready.\n")

if __name__ == '__main__':
    generate_token()
