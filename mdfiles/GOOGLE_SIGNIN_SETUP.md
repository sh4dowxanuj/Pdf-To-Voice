# Google Sign-In Setup Instructions

## Why "Continue with Google" doesn't work

The Google Sign-In functionality is not working because:

1. **Missing OAuth Client Configuration**: Your `google-services.json` file has an empty `oauth_client` array
2. **Missing Web Client ID**: Google Sign-In requires a Web Client ID to work properly
3. **Firebase Authentication not configured**: Google Sign-In provider is not enabled in Firebase Console

## How to Fix It

### Step 1: Configure Firebase Authentication

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `pdf-to-voice-app`
3. Navigate to **Authentication** → **Sign-in method**
4. Click on **Google** provider
5. Enable it and save

### Step 2: Add Web Client ID to Firebase

1. In Firebase Console, go to **Project Settings** (gear icon)
2. Click on **General** tab
3. Scroll down to **Your apps** section
4. Find your Android app and click on it
5. You should see a **Web Client ID** listed there
6. If not present, you need to add a Web application:
   - Click **Add app** → **Web**
   - Give it a name like "PDF to Voice Web"
   - Add authorized domains if needed
   - Download the new `google-services.json`

### Step 3: Update google-services.json

Replace your current `google-services.json` with the new one that includes the OAuth client configuration. It should look like this:

```json
{
  "project_info": {
    "project_number": "873295341604",
    "project_id": "pdf-to-voice-app",
    "storage_bucket": "pdf-to-voice-app.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:873295341604:android:e4aeda90725291e0e4d0b1",
        "android_client_info": {
          "package_name": "com.example.pdftovoice"
        }
      },
      "oauth_client": [
        {
          "client_id": "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
          "client_type": 3
        }
      ],
      "api_key": [
        {
          "current_key": "AIzaSyAxu1QsBAcmUYJeBmSpyTZftPW4aYV4A2U"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": [
            {
              "client_id": "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
              "client_type": 3
            }
          ]
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

### Step 4: Update AuthService with correct Web Client ID

After getting your Web Client ID from Firebase, update the `AuthService.kt` file to use your actual Web Client ID instead of the placeholder.

## Current Implementation Status

✅ **Code Implementation**: Google Sign-In code has been implemented
✅ **Dependencies**: All required dependencies are added
❌ **Firebase Configuration**: OAuth client not configured
❌ **Web Client ID**: Using placeholder ID

## Next Steps

1. Follow the steps above to configure Firebase
2. Update `google-services.json` with OAuth client configuration
3. Update the Web Client ID in `AuthService.kt`
4. Test the Google Sign-In functionality

## Testing

Once configured properly:
1. Tap "Continue with Google" button
2. Google Sign-In sheet should appear
3. Select Google account
4. App should authenticate and navigate to home screen

## Troubleshooting

- **Error: "10: Developer Error"** - Wrong or missing Web Client ID
- **Error: "Network Error"** - Check internet connection and Firebase project status
- **Button does nothing** - Check LogCat for detailed error messages
