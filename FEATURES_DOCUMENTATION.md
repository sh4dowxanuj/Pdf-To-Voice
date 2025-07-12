# PDF to Voice Reader

A modern Android application that converts PDF documents into speech using Text-to-Speech (TTS) technology. Built with Kotlin and Jetpack Compose.

## 🎯 Features

### Core Functionality
- **PDF File Selection**: Choose any PDF from device storage using system file picker
- **Text Extraction**: Extract text from multi-page PDFs using Apache PDFBox
- **Text-to-Speech**: High-quality voice playback using Android's native TTS engine
- **Playback Controls**: Play, pause, stop, and resume functionality
- **Real-time Highlighting**: Shows currently reading segment

### Advanced Features
- **Voice Customization**: Adjustable speech speed (0.1x - 3.0x) and pitch (0.1x - 2.0x)
- **Progress Tracking**: Visual indication of reading progress
- **File Information**: Display PDF name and size
- **Error Handling**: Comprehensive error messages for various scenarios
- **Responsive UI**: Clean, modern Material Design 3 interface

### Accessibility
- **Offline Support**: No internet required for core functionality
- **Large Text Support**: Scrollable text display
- **Voice Feedback**: Audio confirmation of actions
- **Simple Navigation**: Intuitive button layout

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with StateFlow
- **PDF Processing**: Apache PDFBox Android
- **TTS Engine**: Android TextToSpeech API
- **Permissions**: Android runtime permissions

### Project Structure
```
app/src/main/java/com/example/pdftovoice/
├── pdf/
│   └── PdfProcessor.kt          # PDF text extraction
├── tts/
│   └── TtsManager.kt           # Text-to-Speech management
├── viewmodel/
│   └── PdfToVoiceViewModel.kt  # State management
├── ui/
│   ├── screens/
│   │   └── PdfToVoiceScreen.kt # Main UI screen
│   └── theme/                   # App theming
├── utils/
│   ├── FileUtils.kt            # File operations
│   └── PermissionUtils.kt      # Permission handling
└── MainActivity.kt             # Entry point
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 24+ (Android 7.0)
- Device with TTS engine installed

### Installation
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on device or emulator

### Building APK
```bash
./gradlew assembleDebug
```
APK will be generated in `/apk` directory.

## 📱 Usage

### Basic Usage
1. **Launch App**: Open PDF to Voice Reader
2. **Select PDF**: Tap "Choose PDF" and select a document
3. **Play**: Press the play button to start reading
4. **Control**: Use pause/stop buttons as needed

### Advanced Controls
1. **Access Settings**: Tap the settings (gear) icon
2. **Adjust Speed**: Use slider to change speech rate
3. **Modify Pitch**: Adjust voice pitch for comfort
4. **Track Progress**: Monitor current reading position

### Supported File Types
- PDF documents (.pdf)
- Multi-page PDFs
- Text-based PDFs (scanned images with OCR not supported)

## 🔧 Technical Details

### PDF Processing
- Uses Apache PDFBox for reliable text extraction
- Handles multi-page documents efficiently
- Supports password-protected PDFs (with limitations)

### Text-to-Speech
- Leverages Android's built-in TTS engine
- Segments text into sentences for better control
- Provides real-time playback status

### Performance
- Asynchronous PDF processing
- Memory-efficient text handling
- Background TTS processing

### Error Handling
- File validation before processing
- TTS initialization checks
- User-friendly error messages
- Graceful failure recovery

## 🛡️ Permissions

### Required Permissions
- **READ_EXTERNAL_STORAGE**: Access PDF files (Android < 13)
- **READ_MEDIA_IMAGES/VIDEO/AUDIO**: Access media files (Android 13+)

### Optional Permissions
- **INTERNET**: For potential future features (currently unused)

## 🎨 Design Philosophy

### User Experience
- **Accessibility First**: Designed for users with visual impairments
- **Simple Interface**: Clean, uncluttered design
- **Immediate Feedback**: Real-time status updates
- **Error Recovery**: Clear guidance when issues occur

### Material Design 3
- Modern color schemes and typography
- Responsive layouts for different screen sizes
- Consistent interaction patterns
- Smooth animations and transitions

## 🔄 Future Enhancements

### Planned Features
- **Bookmark Support**: Save reading positions
- **Multiple Voice Options**: Choose different TTS voices
- **Reading History**: Track recently read documents
- **Text Highlighting**: Visual indication of current text
- **Export Audio**: Save reading as audio files

### Potential Improvements
- **OCR Support**: Read scanned PDFs
- **Cloud Integration**: Access cloud-stored PDFs
- **Reading Lists**: Organize documents
- **Dark Mode**: Additional theme options

## 🐛 Known Issues

### Current Limitations
- Scanned PDFs (images) not supported without OCR
- Very large PDFs may take time to process
- TTS quality depends on device engine

### Workarounds
- Use text-based PDFs for best results
- Break large documents into smaller sections
- Install high-quality TTS voices from Play Store

## 📞 Support

### Troubleshooting
1. **No Speech**: Check TTS engine installation
2. **File Access**: Verify storage permissions
3. **Poor Quality**: Install better TTS voices
4. **App Crashes**: Restart and check file format

### Common Solutions
- Restart app if TTS fails to initialize
- Grant all requested permissions
- Ensure PDF contains selectable text
- Check device storage space

## 📄 License

This project is open source. Feel free to use, modify, and distribute according to your needs.

## 🙏 Acknowledgments

- Apache PDFBox team for excellent PDF processing
- Android TTS team for speech synthesis
- Material Design team for UI guidelines
- Jetpack Compose team for modern UI framework

---

**Built with ❤️ for accessibility and ease of use**
