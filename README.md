# Vignan Event Booking

A real-time Android equipment booking app for college events, built with Firebase (Auth, Firestore, Storage, FCM) and modern Kotlin architecture.

## Contents
- Project Status
- App Links
- Local APK Outputs
- Key Features
- Tech Stack
- Quick Start
- End-to-End Runbook
- Documentation Index
- Notes

## Project Status
- Build: PASS (`:app:assembleDebug`)
- Install: PASS (`adb install -r app-debug.apk`)
- Firebase mode: real backend (no mock API)

## App Links
- Repository: https://github.com/kranthikiran885366/vignan-event-booking
- Main branch: https://github.com/kranthikiran885366/vignan-event-booking/tree/main
- Actions: https://github.com/kranthikiran885366/vignan-event-booking/actions
- Releases: https://github.com/kranthikiran885366/vignan-event-booking/releases
- Latest release APK: https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk
- Open issues: https://github.com/kranthikiran885366/vignan-event-booking/issues
- New bug report: https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=bug_report.md
- New feature request: https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=feature_request.md
- Pull requests: https://github.com/kranthikiran885366/vignan-event-booking/pulls

## Local APK Outputs
- Debug APK: app/build/outputs/apk/debug/app-debug.apk
- Release APK: app/build/outputs/apk/release/app-release.apk

## Key Features
- Authentication: Email/Password, Google, Anonymous
- Home: realtime equipment list, search, category filters, status chips
- Booking: transaction-safe reserve/cancel and realtime booking history
- Profile: Firestore-backed profile update and stats
- Notifications: FCM token sync and local booking notifications

## Tech Stack
- Kotlin, Android SDK 34, ViewBinding
- Firebase Auth, Firestore, Storage, Messaging, Analytics
- Hilt dependency injection
- Coil image loading
- Room dependencies included for local extension

## Quick Start
1. Install Android Studio, SDK 34, JDK 17.
2. Add app Firebase config file at app/google-services.json.
3. Enable Firebase providers: Email/Password, Google, Anonymous.
4. Create Firestore database and publish rules from firestore.rules.
5. Build and install:

```powershell
$env:GRADLE_USER_HOME="$PWD/.gradle-local"; ./gradlew.bat :app:assembleDebug --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## End-to-End Runbook
1. Configure Firebase app and place `app/google-services.json`.
2. Create Firestore database in Firebase Console.
3. Publish rules from `firestore.rules`.
4. Enable providers: Email/Password, Google, Anonymous.
5. Build and install debug APK.
6. Login and verify Home list, Bookings, and Profile updates.

## Quick Troubleshooting
- Home list empty: verify Firestore rules are published and `equipment` read/write is allowed for authenticated users.
- Permission denied: re-publish `firestore.rules` in Firebase Console.
- Google sign-in fails: verify SHA-1/SHA-256 in Firebase project settings.
- Release APK missing: check latest workflow/release links in `docs/APK_LINKS.md`.

## Documentation Index
- Setup guide: docs/SETUP.md
- Firebase setup: docs/FIREBASE_SETUP.md
- Detailed Firestore setup: docs/FIRESTORE_SETUP_DETAILED.md
- Architecture overview: docs/ARCHITECTURE.md
- APK links and release paths: docs/APK_LINKS.md
- Changelog: docs/CHANGELOG.md
- Firestore database fix guide: FIRESTORE_DATABASE_FIX.md
- Firestore index fix guide: FIRESTORE_INDEX_FIX.md
- Profile save fix guide: PROFILE_SAVE_FIX.md

## Notes
- Keep firestore.rules and app behavior aligned before release.
- Never commit secrets other than expected Firebase Android client config.

