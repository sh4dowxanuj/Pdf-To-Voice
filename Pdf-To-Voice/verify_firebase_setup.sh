#!/bin/bash
# Firebase Setup Verification Script

echo "🔥 Firebase Database Setup Verification"
echo "========================================"
echo ""

# Check if google-services.json exists
if [ -f "app/google-services.json" ]; then
    echo "✅ google-services.json file found"
    
    # Check if it's the demo file or real file
    if grep -q "pdf-to-voice-demo" app/google-services.json; then
        echo "⚠️  WARNING: Using DEMO google-services.json file"
        echo "   → Replace with your real Firebase project file"
        echo ""
    else
        echo "✅ Real google-services.json file detected"
        echo ""
    fi
else
    echo "❌ google-services.json file not found!"
    echo "   → Download from Firebase Console"
    echo ""
fi

# Check build.gradle for Firebase dependencies
if grep -q "firebase-firestore" app/build.gradle; then
    echo "✅ Firebase Firestore dependency found"
else
    echo "❌ Firebase Firestore dependency missing"
fi

if grep -q "google-services" app/build.gradle; then
    echo "✅ Google Services plugin found"
else
    echo "❌ Google Services plugin missing"
fi

# Check AndroidManifest for internet permissions
if grep -q "android.permission.INTERNET" app/src/main/AndroidManifest.xml; then
    echo "✅ Internet permission found"
else
    echo "❌ Internet permission missing"
fi

# Check if Firebase is initialized
if [ -f "app/src/main/java/com/example/pdftovoice/PdfToVoiceApplication.kt" ]; then
    echo "✅ Firebase initialization class found"
else
    echo "❌ Firebase initialization class missing"
fi

echo ""
echo "🚀 Next Steps:"
echo "1. Replace demo google-services.json with your real file"
echo "2. Create Firebase project at https://console.firebase.google.com"
echo "3. Enable Firestore Database in Firebase Console"
echo "4. Configure security rules"
echo "5. Test with internet connection"
echo ""
echo "📖 See FIREBASE_SETUP.md for detailed instructions"
