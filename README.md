 <div align="center">

<br/>

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/Firebase-Backend-FFCA28?style=for-the-badge&logo=firebase&logoColor=black"/>
<img src="https://img.shields.io/badge/SDK-34-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Build-Passing-00C853?style=for-the-badge&logo=github-actions&logoColor=white"/>

<br/><br/>

# 📅 Vignan Event Booking

### Real-time equipment reservation for college events — built fast, built right.

*Firebase · Kotlin · Hilt · Firestore · FCM*

<br/>

[**⬇ Download APK**](https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk) &nbsp;·&nbsp;
[**📖 Docs**](#-documentation) &nbsp;·&nbsp;
[**🐛 Report Bug**](https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=bug_report.md) &nbsp;·&nbsp;
[**✨ Request Feature**](https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=feature_request.md)

</div>

---

## 🗂 Table of Contents

- [Project Status](#-project-status)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Firebase Setup](#-firebase-setup)
- [End-to-End Runbook](#-end-to-end-runbook)
- [APK Outputs](#-apk-outputs)
- [Troubleshooting](#-troubleshooting)
- [Documentation](#-documentation)
- [Contributing](#-contributing)
- [Security](#-security)

---

## ✅ Project Status

| Check | Status |
|---|---|
| `:app:assembleDebug` | ✅ **PASS** |
| `adb install -r app-debug.apk` | ✅ **PASS** |
| Firebase backend | 🔥 **Live** (no mock API) |
| CI/CD | [![Actions](https://img.shields.io/github/actions/workflow/status/kranthikiran885366/vignan-event-booking/build.yml?label=CI&style=flat-square)](https://github.com/kranthikiran885366/vignan-event-booking/actions) |
| Latest Release | [![Release](https://img.shields.io/github/v/release/kranthikiran885366/vignan-event-booking?style=flat-square&color=7F52FF)](https://github.com/kranthikiran885366/vignan-event-booking/releases) |
| Open Issues | [![Issues](https://img.shields.io/github/issues/kranthikiran885366/vignan-event-booking?style=flat-square)](https://github.com/kranthikiran885366/vignan-event-booking/issues) |
| Pull Requests | [![PRs](https://img.shields.io/github/issues-pr/kranthikiran885366/vignan-event-booking?style=flat-square)](https://github.com/kranthikiran885366/vignan-event-booking/pulls) |

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🔐 Authentication
- Email / Password sign-in
- Google OAuth sign-in
- Anonymous (guest) access
- Persistent session management

</td>
<td width="50%">

### 🏠 Home Screen
- Real-time equipment list via Firestore
- Search & text filtering
- Category-based filters
- Live availability status chips

</td>
</tr>
<tr>
<td width="50%">

### 📋 Booking Engine
- Transaction-safe `reserve` & `cancel`
- Real-time booking history
- Conflict detection & prevention
- Per-user booking limits

</td>
<td width="50%">

### 👤 Profile
- Firestore-backed profile updates
- Booking stats & history overview
- Avatar management via Firebase Storage
- Instant save with error feedback

</td>
</tr>
<tr>
<td width="50%">

### 🔔 Notifications
- FCM token sync on login
- Local booking confirmation alerts
- Push notification routing
- Background message handling

</td>
<td width="50%">

### ⚙️ Developer Experience
- Hilt dependency injection throughout
- ViewBinding (no `findViewById`)
- Coil for image loading
- Room stubs for offline extension

</td>
</tr>
</table>

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Min / Target SDK | 21 / 34 |
| UI | XML Layouts + ViewBinding |
| Auth | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Push | Firebase Cloud Messaging (FCM) |
| Analytics | Firebase Analytics |
| DI | Hilt (Dagger) |
| Image Loading | Coil |
| Local DB (stub) | Room |
| Build | Gradle (Kotlin DSL) |

---

## 🏗 Architecture

```
app/
├── di/                  # Hilt modules (Firebase, Repo bindings)
├── data/
│   ├── model/           # Kotlin data classes (Equipment, Booking, User)
│   ├── repository/      # Firestore + Auth repository layer
│   └── local/           # Room DAOs (offline extension ready)
├── ui/
│   ├── auth/            # Login, Register, Google sign-in fragments
│   ├── home/            # Equipment list, search, filters
│   ├── booking/         # Reserve / cancel / history
│   └── profile/         # Profile edit & stats
├── service/
│   └── FCMService.kt    # Push notification handling
└── util/                # Extensions, constants, helpers
```

> **Pattern:** MVVM · Repository · Hilt DI · Firestore real-time listeners

---

## ⚡ Quick Start

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog+ |
| JDK | 17 |
| Android SDK | 34 |
| ADB | Any recent |

### 1 — Clone & open

```bash
git clone https://github.com/kranthikiran885366/vignan-event-booking.git
cd vignan-event-booking
# Open in Android Studio → File → Open
```

### 2 — Add Firebase config

```
app/
└── google-services.json   ← place your Firebase config here
```

> Download from **Firebase Console → Project Settings → Your Apps → google-services.json**

### 3 — Build & install

```powershell
# Windows (PowerShell)
$env:GRADLE_USER_HOME="$PWD/.gradle-local"
./gradlew.bat :app:assembleDebug --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
# macOS / Linux
./gradlew :app:assembleDebug --no-daemon
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔥 Firebase Setup

### Step 1 — Enable Authentication providers

In **Firebase Console → Authentication → Sign-in method**, enable:

- [x] Email / Password
- [x] Google
- [x] Anonymous

### Step 2 — Create Firestore database

**Firebase Console → Firestore Database → Create database**
- Start in **production mode**
- Choose your region (e.g., `asia-south1`)

### Step 3 — Publish Firestore rules

```bash
firebase deploy --only firestore:rules
# or manually paste firestore.rules in Firebase Console → Rules tab
```

### Step 4 — Add SHA fingerprints (Google Sign-In)

```bash
# Debug fingerprint
./gradlew signingReport

# Add both SHA-1 and SHA-256 to:
# Firebase Console → Project Settings → Your Apps → Add fingerprint
```

---

## 📋 End-to-End Runbook

```
1. Place google-services.json in /app
2. Create Firestore database
3. Publish firestore.rules
4. Enable Auth providers (Email, Google, Anonymous)
5. Add SHA-1 + SHA-256 to Firebase project settings
6. Build debug APK → install via ADB
7. Login → verify Home equipment list populates
8. Reserve an item → check Bookings tab updates in real-time
9. Edit profile → confirm Firestore save & stats refresh
```

---

## 📦 APK Outputs

| Variant | Path | Link |
|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | Built locally |
| Release | `app/build/outputs/apk/release/app-release.apk` | Built locally |
| Latest Release | GitHub Releases | [⬇ Download](https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk) |

---

## 🔧 Troubleshooting

<details>
<summary><strong>🔴 Home list is empty</strong></summary>

- Verify Firestore rules are **published** (not just saved)
- Confirm the `equipment` collection exists in Firestore
- Check that the signed-in user satisfies your read rules
- See [`docs/FIRESTORE_SETUP_DETAILED.md`](docs/FIRESTORE_SETUP_DETAILED.md)

</details>

<details>
<summary><strong>🔴 Permission denied errors</strong></summary>

- Re-publish `firestore.rules` via Firebase Console or CLI
- Double-check the rules allow `read`/`write` for authenticated users
- See [`FIRESTORE_DATABASE_FIX.md`](FIRESTORE_DATABASE_FIX.md)

</details>

<details>
<summary><strong>🔴 Google Sign-In fails</strong></summary>

- Add both **SHA-1** and **SHA-256** fingerprints in Firebase Console
- Re-download `google-services.json` after adding fingerprints
- Make sure Google sign-in is **enabled** in Firebase Auth

</details>

<details>
<summary><strong>🔴 Firestore index errors</strong></summary>

- Click the index creation link in the Logcat error message
- Or follow [`FIRESTORE_INDEX_FIX.md`](FIRESTORE_INDEX_FIX.md) for manual steps

</details>

<details>
<summary><strong>🔴 Profile save fails</strong></summary>

- Check Firestore `users` collection write rules
- See [`PROFILE_SAVE_FIX.md`](PROFILE_SAVE_FIX.md) for detailed fix

</details>

<details>
<summary><strong>🔴 Release APK missing from GitHub</strong></summary>

- Check [GitHub Actions](https://github.com/kranthikiran885366/vignan-event-booking/actions) for build logs
- See [`docs/APK_LINKS.md`](docs/APK_LINKS.md) for all download paths

</details>

---

## 📚 Documentation

| Doc | Description |
|---|---|
| [`docs/SETUP.md`](docs/SETUP.md) | Full local setup guide |
| [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md) | Firebase project configuration |
| [`docs/FIRESTORE_SETUP_DETAILED.md`](docs/FIRESTORE_SETUP_DETAILED.md) | Firestore collections, rules, indexes |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | App architecture & design patterns |
| [`docs/APK_LINKS.md`](docs/APK_LINKS.md) | APK download links & release paths |
| [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | Version history |
| [`FIRESTORE_DATABASE_FIX.md`](FIRESTORE_DATABASE_FIX.md) | Fix database permission errors |
| [`FIRESTORE_INDEX_FIX.md`](FIRESTORE_INDEX_FIX.md) | Fix composite index errors |
| [`PROFILE_SAVE_FIX.md`](PROFILE_SAVE_FIX.md) | Fix profile save failures |

---

## 🤝 Contributing

Contributions are welcome!

1. **Fork** the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to your fork: `git push origin feat/your-feature`
5. Open a **Pull Request** → [New PR](https://github.com/kranthikiran885366/vignan-event-booking/pulls)

For bugs → [Open an issue](https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=bug_report.md)
For features → [Request a feature](https://github.com/kranthikiran885366/vignan-event-booking/issues/new?template=feature_request.md)

---

## 🔒 Security

- **Never commit** `google-services.json` secrets or signing keystores to version control
- `google-services.json` contains only the public Firebase Android client config — it is safe to include in the app bundle but keep it out of public forks if your project rules are permissive
- Firestore security rules are the primary access control layer — always validate them before releasing

---

<div align="center">

<br/>

Made with ❤️ for **Vignan University**

[![GitHub Stars](https://img.shields.io/github/stars/kranthikiran885366/vignan-event-booking?style=social)](https://github.com/kranthikiran885366/vignan-event-booking)
[![GitHub Forks](https://img.shields.io/github/forks/kranthikiran885366/vignan-event-booking?style=social)](https://github.com/kranthikiran885366/vignan-event-booking/fork)

</div>
