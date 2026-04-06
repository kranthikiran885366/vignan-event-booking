# Setup Guide

## Prerequisites
- Android Studio (latest stable)
- Android SDK 34
- JDK 17
- Firebase project

## Firebase Config
1. Add Android app in Firebase console with package name:
   - com.example.myapplication
2. Download google-services.json.
3. Place file here:
   - app/google-services.json
4. Add SHA-1 and SHA-256 fingerprints for debug/release keys.

## Build
```powershell
$env:GRADLE_USER_HOME="$PWD/.gradle-local"; ./gradlew.bat :app:assembleDebug --no-daemon
```

## Install Debug APK
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Release Build
```powershell
$env:GRADLE_USER_HOME="$PWD/.gradle-local"; ./gradlew.bat :app:assembleRelease --no-daemon
```

## Troubleshooting
- If auth fails, verify SHA keys in Firebase console.
- If Firestore empty, create equipment collection documents.
- If image load fails, verify storage paths/rules.
