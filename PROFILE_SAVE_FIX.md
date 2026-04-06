# Profile Save "Document Not Found" Error - Fixed

## App Links
- Repository: https://github.com/kranthikiran885366/vignan-event-booking
- Releases: https://github.com/kranthikiran885366/vignan-event-booking/releases
- Latest APK: https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk
- Full links index: docs/APK_LINKS.md

## Problem
When you tried to save profile changes (Edit Profile), you saw:
> **Failed to save profile: Document not found**

This happened because the code used `.update()` which fails if the Firestore document doesn't exist.

## Root Cause
Three locations were using `.update()` which requires the document to already exist:

1. **ProfileFragment.kt** - `saveProfileChanges()` when editing profile
2. **RegisterActivity.kt** - `saveFcmToken()` after registration  
3. **LoginActivity.kt** - `saveFcmToken()` after login

## Solution ✅

Changed all three to use `.set(..., SetOptions.merge())` instead:

```kotlin
// BEFORE (fails if document doesn't exist)
firestore.collection("users").document(uid).update(updates)

// AFTER (creates document if missing, or merges with existing)
firestore.collection("users").document(uid).set(updates, SetOptions.merge())
```

**Why merge?**
- `.set()` alone **overwrites** the entire document (loses other fields)
- `.set(..., merge())` only **updates** the specified fields, keeps others intact

## Files Updated

1. **app/src/main/java/.../ui/fragment/ProfileFragment.kt**
   - `saveProfileChanges()`: Now uses `.set(updates, SetOptions.merge())`

2. **app/src/main/java/.../RegisterActivity.kt**
   - Added import: `com.google.firebase.firestore.SetOptions`
   - `saveFcmToken()`: Now uses `.set(mapOf(...), SetOptions.merge())`

3. **app/src/main/java/.../LoginActivity.kt**
   - Added import: `com.google.firebase.firestore.SetOptions`
   - `saveFcmToken()`: Now uses `.set(mapOf(...), SetOptions.merge())`

## Test It

1. **Edit Profile**: Go to Profile tab → Edit Profile button → Change Name → Save
   - ✅ Should now save without "Document not found" error
   - ✅ You'll see "Profile updated successfully"

2. **Register new account**: Complete registration
   - ✅ FCM token now saves properly

3. **Login**: Login with email/password
   - ✅ FCM token now saves properly

## Build Status

- Kotlin compile: **PASS**
- APK build: **PASS**
- Device install: **SUCCESS**
