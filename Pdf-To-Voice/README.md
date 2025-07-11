# 📱 PDF to Voice - Android Login/Signup App

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/your-username/pdf-to-voice)
[![Firebase](https://img.shields.io/badge/database-Firebase_Firestore-orange)](https://firebase.google.com/docs/firestore)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-blue)](https://kotlinlang.org/)
[![Material Design](https://img.shields.io/badge/UI-Material_Design_3-purple)](https://m3.material.io/)

A modern Android application with secure user authentication and **external Firebase Firestore database storage**. Features a beautiful Material Design UI and scalable cloud backend.

## 🌟 Features

### 🔐 **Authentication System**
- **Dual Login**: Users can log in with either email or username
- **Secure Registration**: Email validation and password strength requirements  
- **Password Security**: SHA-256 hashing before cloud storage
- **Input Validation**: Real-time form validation with user feedback

### 🌐 **External Database**
- **Firebase Firestore**: Scalable NoSQL cloud database
- **Real-time Sync**: Live data updates across devices
- **Auto-scaling**: Handles millions of users automatically
- **Global CDN**: Fast access from anywhere in the world

### 🎨 **Modern UI/UX**
- **Material Design 3**: Latest design system components
- **Responsive Layout**: Works on all screen sizes
- **Smooth Animations**: Beautiful transitions between screens
- **Custom Icons**: Vector-based iconography

### 🏗️ **Architecture**
- **MVVM Pattern**: Model-View-ViewModel architecture
- **Repository Pattern**: Clean separation of data sources
- **Coroutines**: Asynchronous programming for smooth UI
- **LiveData**: Reactive UI updates
- **ViewBinding**: Type-safe view access

## 🔧 Tech Stack

- **Language**: Kotlin
- **Min SDK**: 21 (Android 5.0+)
- **Target SDK**: 34 (Android 14)
- **Database**: Firebase Firestore
- **Architecture**: MVVM + Repository Pattern
- **UI**: Material Design 3
- **Async**: Kotlin Coroutines
- **Navigation**: Navigation Component

## 🚀 Quick Start

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 11 or higher
- Firebase account (free tier available)

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/pdf-to-voice.git
cd pdf-to-voice
```

### 2. Set Up Firebase
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project
3. Add Android app with package name: `com.example.pdftovoice`
4. Download `google-services.json`
5. Replace `app/google-services.json` with your downloaded file

### 3. Enable Firestore Database
1. In Firebase Console → "Firestore Database"
2. Click "Create database" → Choose "Production mode"
3. Select your preferred location

### 4. Build and Run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or open in Android Studio and click the Run button.

## 📁 Project Structure

The project follows the standard Android project structure:

```
Pdf-To-Voice/
├── app/
│   ├── build.gradle (App-level build configuration)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/pdftovoice/
│       │   │   └── MainActivity.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── layout/
│       │       ├── mipmap-*/
│       │       ├── values/
│       │       └── xml/
│       ├── test/ (Unit tests)
│       └── androidTest/ (Instrumented tests)
├── build.gradle (Project-level build configuration)
├── gradle.properties
├── settings.gradle
└── gradle/wrapper/
```

## Getting Started

1. Open this project in Android Studio
2. Wait for Gradle sync to complete
3. Replace the placeholder icon files in `app/src/main/res/mipmap-*/` with actual PNG icons
4. Build and run the app

## SDK Configuration

The project is configured with:
- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34 (Android 14)

## Dependencies

The project includes the following dependencies:
- AndroidX Core KTX
- AppCompat
- Material Design Components
- ConstraintLayout
- JUnit (for testing)
- Espresso (for UI testing)

## Notes

- The project uses Kotlin as the primary programming language
- Material 3 theme is configured
- Basic MainActivity with "Hello World" layout is included
- Placeholder launcher icons are provided (replace with actual icons)
- Standard Android backup and data extraction rules are included
