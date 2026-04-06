# Firebase Setup (Real Backend)

This app is configured for real Firebase services only.

## Quick Setup Checklist
- [ ] Create Firebase project and register Android app (`com.example.myapplication`)
- [ ] Add `app/google-services.json`
- [ ] Enable Auth providers (Email/Password, Google, Anonymous)
- [ ] Create Firestore database in Native mode
- [ ] Publish rules from `firestore.rules`
- [ ] Build, install, and verify Home/Bookings/Profile flows

## App Links
- Repository: https://github.com/kranthikiran885366/vignan-event-booking
- Actions: https://github.com/kranthikiran885366/vignan-event-booking/actions
- Releases: https://github.com/kranthikiran885366/vignan-event-booking/releases
- Latest release APK: https://github.com/kranthikiran885366/vignan-event-booking/releases/latest/download/app-release.apk
- Full links index: docs/APK_LINKS.md

## ⚠️ Important: Create Firestore Database First

If you see "Firestore default database is not created" error:

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. In left sidebar: **Build** → **Firestore Database**
4. Click **"Create Database"**
5. Choose your region (default is fine)
6. Click **"Start in test mode"**
7. Create the database

Then apply Firestore rules (see below).

## Setup Steps

### 1. Authentication
Firebase Console → **Authentication** → **Sign-in method**

Enable providers:
- ✅ Email/Password
- ✅ Google (add OAuth consent screen)
- ✅ Anonymous (for guest mode)

### 2. Firestore Database
- **Must** be Native mode (not Datastore)
- Create collections: `equipment`, `bookings`, `users`
- See **Collection Schemas** below

### 3. Apply Firestore Security Rules

Go to Firestore Console → **Rules** tab.

Replace **ALL** rules with from `firestore.rules`:

```text
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;
    }

    match /equipment/{equipmentId} {
      allow read: if request.auth != null;
      allow create, update: if request.auth != null;
      allow delete: if request.auth.token.admin == true;
    }

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

    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && request.auth.uid == userId;
      allow update: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

Click **Publish**.

### 4. Optional: Cloud Storage (for Equipment Images)

Firebase Console → **Storage** → **Get Started**

Upload images with paths like:
- `equipment/projector.jpg`
- `equipment/microphone.jpg`

## Collection Schemas

### equipment
```json
{
  "id": 1,
  "name": "Projector X1",
  "category": "Visual",
  "quantity": 4,
  "location": "Hall A",
  "imagePath": "equipment/projector.jpg",
  "bookingDate": "",
  "available": true
}
```

### bookings
```json
{
  "bookingId": "auto-generated",
  "equipmentId": 1,
  "equipmentName": "Projector X1",
  "userId": "firebase_uid",
  "userFullName": "John Doe",
  "studentId": "22BCE001",
  "userEmail": "john@college.edu",
  "date": "2026-04-10",
  "location": "Hall A",
  "category": "Visual",
  "status": "CONFIRMED",
  "createdAt": "server-timestamp"
}
```

### users
```json
{
  "_id_field_": "firebase_uid",
  "fullName": "Student Name",
  "studentId": "STU-2024-001",
  "email": "student@college.edu",
  "photoUrl": "https://...",
  "fcmToken": "device-token",
  "createdAt": "server-timestamp"
}
```

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| **Firestore default database not created** | Database doesn't exist in Firebase | Go to Firestore → Create Database (steps above) |
| **Permission denied** | Security rules too restrictive | Publish the rules from `firestore.rules` |
| **Equipment list empty** | No data seeded | First app launch auto-seeds 10 items |
| **Collection not found** | Waiting for auto-seed | Wait 5-10 seconds or refresh |

## More Details

See [**FIRESTORE_SETUP_DETAILED.md**](FIRESTORE_SETUP_DETAILED.md) for:
- Step-by-step images/screenshots
- Cloud Storage setup for images
- FCM Push Notifications
- Verification checklist

## Related Fix Guides
- `FIRESTORE_DATABASE_FIX.md`
- `FIRESTORE_INDEX_FIX.md`
- `PROFILE_SAVE_FIX.md`

