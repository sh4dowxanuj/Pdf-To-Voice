# PDF to Voice Reader 🎙️📱

A modern Android application that converts PDF documents into speech using Text-to-Speech (TTS) technology. Built with **Kotlin** and **Jetpack Compose** following Material Design 3 principles.

## ✨ Features Implemented

### 🎯 Core Functionality
- ✅ **PDF File Selection**: System file picker for PDF documents
- ✅ **Text Extraction**: Demonstrates PDF processing capability with sample content
- ✅ **Text-to-Speech**: Android's native TTS engine integration
- ✅ **Playback Controls**: Play, pause, stop, and resume functionality
- ✅ **Enhanced Text Highlighting**: Real-time highlighting with auto-scroll and contextual colors
- ✅ **Full-Screen Text Panel**: Modal text viewer with enhanced readability
- ✅ **Music Player-Style Controls**: Bottom-fixed controls similar to Spotify/Apple Music
- ✅ **Real-Time Translation (Gemini + Fallback)**: Chunked on-device orchestrated translation with live progress
 - Automatic translation when target TTS language differs
 - Manual translation trigger with language code input (e.g. `es`, `fr`)
 - Provider fallback chain: Gemini → LibreTranslate
 - Partial result streaming & progress percentages
 - Original / Translated toggle without losing original text
 - Provider diagnostics (Gemini / Libre / Mixed / Skip)
 - Heuristic language auto-detect to avoid redundant translation

### 🎛️ Advanced Controls
- ✅ **Voice Speed Control**: Adjustable from 0.1x to 3.0x speed
- ✅ **Pitch Adjustment**: Range from 0.1x to 2.0x pitch
- ✅ **Progress Tracking**: Visual indication of reading progress
- ✅ **File Information**: Display PDF name and size
- ✅ **Error Handling**: Comprehensive error messages and recovery
 - ✅ **Secure API Key Handling**: Gemini key loaded from local (untracked) `local.properties` or environment
 - ✅ **Translation Diagnostics**: Provider name, partial indicator, and key availability in UI

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
 - Translation bar with live progress, provider badges, cancel & toggle

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

### 3. Gemini Translation Setup (Optional but Recommended)

Add your Gemini API key to enable high-quality translations:

1. Create (or edit) a `local.properties` file at the project root (same level as `settings.gradle`).
2. Add:

```
GEMINI_API_KEY=your_real_key_here
```

3. (Alternative) Export as environment variable before building:

```bash
export GEMINI_API_KEY=your_real_key_here
./gradlew assembleDebug
```

4. The build injects a `BuildConfig.GEMINI_API_KEY` field. The app UI will indicate if the key is missing (falls back to LibreTranslate).

Security Note: `local.properties` is ignored by Git—do NOT commit your real key.

### 4. Manual Translation Usage

While viewing a document in the full-screen reader:
1. Tap the Translate button in the top bar.
2. Enter a target language ISO code (e.g. `es`, `fr`, `de`, `hi`).
3. Press Translate — progress and partial text will appear.
4. Toggle between Original and Translated at any time.
5. Cancel mid-way if needed (partial content retained with a (partial) indicator until replaced or cleared).

Automatic translation also triggers when you change the TTS language to a new target.

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
- Gemini Generative Language API (HTTP JSON POST usage — no official SDK dependency)
- LibreTranslate public endpoint as fallback

## Screenshots

[Add screenshots here when the app is running]

## License

This project is for educational purposes.

---
### 🔍 Translation Internals Overview

The translation system streams large documents by splitting into ~4500 char chunks, translating sequentially. Each chunk prefers Gemini; on failure it falls back to LibreTranslate, marking the provider used. Partial aggregated text is published to state after every chunk with progressive percentage updates.

Heuristic language detection (very lightweight) attempts to skip translation if the document already matches the requested target (currently supports EN/ES/FR patterns). This avoids unnecessary API usage.

State fields exposed to UI:
- `originalExtractedText` – immutable source text
- `translatedText` – current (partial or complete) translation
- `activeTextSource` – ORIGINAL | TRANSLATED
- `translationProgress` – 0..100
- `translationProvider` – Gemini | Libre | Mixed | Skip
- `translationPartial` – true while streaming or after cancellation
- `isGeminiAvailable` – reflects presence of configured key

Planned Enhancements (optional):
- More robust language detection via compact model
- Offline translation caching
- Retry budget / exponential backoff per chunk
- User-facing provider selection

---
