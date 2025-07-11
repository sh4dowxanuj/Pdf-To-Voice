# Firebase Testing Commands

## Build and Install
./gradlew assembleDebug
./gradlew installDebug

## Check Logs for Firebase
adb logcat | grep -i firebase
adb logcat | grep -i firestore

## Test Network Connection
adb shell ping 8.8.8.8

## Clear App Data (for testing)
adb shell pm clear com.example.pdftovoice

## Monitor Firebase Operations
adb logcat | grep "FirebaseUserRepository"
