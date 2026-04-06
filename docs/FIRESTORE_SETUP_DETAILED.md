# Firebase Firestore Setup Guide

## App Links
- Repository: https://github.com/kranthikiran885366/vignan-event-booking
- Actions: https://github.com/kranthikiran885366/vignan-event-booking/actions
- Releases: https://github.com/kranthikiran885366/vignan-event-booking/releases
- Latest release APK: https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk
- Full links index: docs/APK_LINKS.md

## Prerequisites
- Firebase project created in [Firebase Console](https://console.firebase.google.com)
- Android app registered in Firebase project with package `com.example.myapplication`
- `google-services.json` downloaded and placed in `app/` directory

## Step 1: Create Firestore Database

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. In left sidebar: **Build** → **Firestore Database**
4. Click **Create Database**
5. Choose location (default is fine)
6. **Security Rules**: Select **"Start in test mode"** (we'll update rules next)
7. Click **Create**

Your database is now created with path: `gs://project-id-xxxxx.appspot.com/`

## Step 2: Apply Security Rules

1. In Firestore Console, go to **Rules** tab
2. Replace **ALL** existing rules with the content from `firestore.rules` in this project root:

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    // Default: deny all access
    match /{document=**} {
      allow read, write: if false;
    }

    // Equipment catalog
    match /equipment/{equipmentId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null;
      allow delete: if request.auth.token.admin == true;
    }

    // Allow authenticated users to work with their bookings
    match /bookings/{bookingId} {
      allow read: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        request.auth.token.admin == true
      );
      allow create: if request.auth != null;
      allow update: if request.auth != null && (
        resource.data.userId == request.auth.uid ||
        request.auth.token.admin == true
      );
    }

    // Allow users to manage only their own profile
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

3. Click **Publish** to apply rules

## Step 3: Seed Initial Equipment Data

First app launch will automatically seed 10 equipment items if collection is empty.

To manually seed later:
1. Go to Firestore Console → **Data**
2. Create collection: `equipment`
3. Add sample documents with fields:
   - `id`: int (e.g., 1)
   - `name`: string (e.g., "Projector")
   - `category`: string (e.g., "Visual")
   - `quantity`: int (e.g., 5)
   - `location`: string (e.g., "Hall A")
   - `imagePath`: string (optional)

Or let the app auto-seed on first launch.

## Step 4: Configure Authentication

1. Firebase Console → **Authentication**
2. Enable sign-in methods:
   - **Email/Password** ✓
   - **Google** ✓
   - **Anonymous** ✓
3. For Google sign-in, add OAuth consent screen

## Step 5: Configure Cloud Storage (Optional for Images)

1. Firebase Console → **Storage**
2. Click **Get Started**
3. Choose location and create
4. Upload equipment images to `equipment/` folder with names matching `imagePath` in Firestore documents

## Collections Schema

### equipment
```json
{
  "id": 1,
  "name": "Projector",
  "category": "Visual",
  "quantity": 5,
  "location": "Hall A",
  "imagePath": "equipment/projector.jpg",
  "description": "High-lumen projector"
}
```

### bookings
```json
{
  "bookingId": "auto-generated",
  "equipmentId": 1,
  "equipmentName": "Projector",
  "userId": "firebase-user-id",
  "userFullName": "John Doe",
  "studentId": "22BCE001",
  "userEmail": "john@college.edu",
  "date": "2025-08-01",
  "location": "Hall A",
  "category": "Visual",
  "status": "CONFIRMED",
  "createdAt": "server-timestamp"
}
```

### users
```json
{
  "userId": "firebase-user-id",
  "email": "john@college.edu",
  "fullName": "John Doe",
  "studentId": "22BCE001",
  "photoUrl": "https://...",
  "fcmToken": "device-token",
  "createdAt": "server-timestamp"
}
```

## Troubleshooting

### Error: "Firestore default database is not created"
- The "(default)" database doesn't exist in Firebase Console
- **Fix**: Go to Firebase Console → Firestore Database → Click "Create Database"

### Error: "Permission denied" on read/write
- Security rules are blocking access
- **Fix**: Check rules are published correctly and user is authenticated

### Error: "Collection not found"
- The collection doesn't exist yet
- **Fix**: First app launch will auto-seed equipment collection from FirestoreSeeder

## Build Command (with local Gradle cache)

```powershell
$env:GRADLE_USER_HOME="$PWD\.gradle-local"; ./gradlew.bat :app:assembleDebug --no-daemon
```

## Verification Steps

1. **Login** with valid email/password
2. **Home** screen shows 10+ equipment items (seeded)
3. **Filter/Search** works
4. **Reserve** equipment → Check Bookings tab shows full booking details with Name, Student ID, Email
5. **Profile** → Edit shows Name and Student ID saved to Firestore

If these all work, Firestore is properly configured!
