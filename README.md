# PDF to Voice Reader 🎙️📱

A modern Android application that converts PDF documents into speech using Text-to-Speech (TTS) technology. Built with **Kotlin** and **Jetpack Compose** following Material Design 3 principles.
37344876683-gqih7s977o6vbvsm1f9u1g31lg763pvt.apps.googleusercontent.com
## ✨ Features Implemented

### 🎯 Core Functionality
- ✅ **PDF File Selection**: System file picker for PDF documents
- ✅ **Text Extraction**: Demonstrates PDF processing capability with sample content
- ✅ **Text-to-Speech**: Android's native TTS engine integration
- ✅ **Playback Controls**: Play, pause, stop, and resume functionality
- ✅ **Enhanced Text Highlighting**: Real-time highlighting with auto-scroll and contextual colors
- ✅ **Full-Screen Text Panel**: Modal text viewer with enhanced readability
- ✅ **Music Player-Style Controls**: Bottom-fixed controls similar to Spotify/Apple Music

### 🎛️ Advanced Controls
- ✅ **Voice Speed Control**: Adjustable from 0.1x to 3.0x speed
- ✅ **Pitch Adjustment**: Range from 0.1x to 2.0x pitch
- ✅ **Progress Tracking**: Visual indication of reading progress
- ✅ **File Information**: Display PDF name and size
- ✅ **Error Handling**: Comprehensive error messages and recovery

### 🎨 Modern UI
- ✅ **Material Design 3**: Clean, modern interface
- ✅ **Responsive Design**: Adapts to different screen sizes
- ✅ **Accessibility**: Screen reader friendly
- ✅ **Intuitive Controls**: Large, accessible buttons with music player-style layout
- ✅ **Visual Feedback**: Real-time status updates with animated indicators
- ✅ **Enhanced Text Display**: Full-screen modal with improved typography
- ✅ **Contextual Highlighting**: Dynamic text highlighting that follows speech progress
- Built with Jetpack Compose
- Clean architecture with MVVM pattern

## Setup Instructions

### 1. Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use an existing one
3. Add an Android app to your project:
   - Package name: `com.example.pdftovoice`
   - App nickname: PDF to Voice (optional)
4. Download the `google-services.json` file from Firebase Console
5. Place the downloaded `google-services.json` file in the `app/` directory (this file is required for Firebase to work)
6. Enable Authentication in Firebase Console:
   - Go to Authentication > Sign-in method
   - Enable Email/Password provider

**Important**: The app will not build without a valid `google-services.json` file in the `app/` directory.

### 2. Build and Run

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Connect an Android device or start an emulator
4. Run the app

### Alternative: Command Line Build

```bash
# Build debug APK (automatically copied to /apk directory)
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# APK location: /apk/app-debug.apk
```

## Project Structure

```
app/
├── src/main/java/com/example/pdftovoice/
│   ├── auth/                 # Authentication related files
│   │   ├── AuthService.kt    # Firebase auth service
│   │   ├── AuthViewModel.kt  # Auth view model
│   │   ├── LoginScreen.kt    # Login UI
│   │   └── SignUpScreen.kt   # Sign up UI
│   ├── home/                 # Home screen
│   │   └── HomeScreen.kt
│   ├── navigation/           # Navigation
│   │   └── AuthNavGraph.kt
│   ├── ui/theme/            # App theming
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── MainActivity.kt       # Main activity
└── src/main/res/            # Resources
```

## Design Features

The UI is inspired by Google's Material Design with:

- Clean, minimalist interface
- Google Blue (#4285F4) as primary color
- Consistent spacing and typography
- Material Design 3 components
- Smooth animations and transitions
- Responsive layout for different screen sizes

## Dependencies

- Jetpack Compose for UI
- Firebase Auth for authentication
- Navigation Compose for navigation
- Material Design 3 components
- Kotlin Coroutines for asynchronous operations

## Screenshots

[Add screenshots here when the app is running]

## License

This project is for educational purposes.
