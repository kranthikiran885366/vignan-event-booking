# 🔧 Firestore Database Not Found - Fix Guide

## Problem
When you try to reserve equipment or view bookings, you see:

> ❌ **Firestore database is not created.**
> 
> Fix: Open Firebase Console → Firestore Database → Click 'Create Database'...

## Why This Happens

Firebase projects are created **without** Firestore database by default.

The app tried to:
1. Load equipment list from Firestore
2. Save your booking to Firestore
3. Show your profile details from Firestore

But the database doesn't exist yet, so operations fail.

## Quick Fix (2 minutes)

### Step 1: Open Firebase Console
- Go to [Firebase Console](https://console.firebase.google.com)
- Click your project name

### Step 2: Create Firestore Database
1. In left sidebar, click **Build** 
2. Select **Firestore Database**
3. Click **Create Database** button
4. Choose location (default is fine)
5. Click **"Start in test mode"** (easiest for development)
6. Click **Create**

✅ Database is now created!

### Step 3: Apply Security Rules
1. Still in Firestore Console, click **Rules** tab
2. **Delete all existing rules**
3. **Paste the entire content** from [`firestore.rules`](firestore.rules) file in this project
4. Click **Publish**

✅ Rules are now applied!

### Step 4: Restart App
1. Close the app on your phone
2. Reopen it
3. You should see 10+ equipment items now

## Database Sections Explained

This project uses 3 Firestore **collections**:

| Collection | Purpose | Auto-Created? |
|-----------|---------|---------------|
| `equipment` | 10 sample equipment items | ✅ Yes, on first app launch |
| `bookings` | Your equipment reservations | ✅ Auto-documents on reserve |
| `users` | User profiles (name, student ID, email) | ✅ Auto-created on login |

## Testing Checklist

After setup, verify everything works:

- [ ] Login with email/password
- [ ] See 10+ equipment items on Home screen
- [ ] Search/filter equipment
- [ ] Set booking date and click Reserve
- [ ] See booking appear in Bookings tab with full details
- [ ] Edit Profile shows Name and Student ID
- [ ] Can see stored booking history across app sessions

## Still Getting Errors?

### "Permission denied" Error

The security rules didn't publish correctly.

**Fix:**
1. Firestore Console → Rules tab
2. Copy all text from [`firestore.rules`](firestore.rules)
3. Paste into editor
4. **Publish** button must turn green
5. Restart app

### "Equipment list still empty"

Seeding might need time.

**Fix:**
1. Restart app by fully closing it
2. Wait 5-10 seconds
3. If still empty, go to Firestore Console → Data tab
4. Manually create `equipment` collection
5. Add a test document with `id: 1`, `name: "Test"`, `quantity: 5`

## Files Reference

| File | Purpose |
|------|---------|
| [`firestore.rules`](firestore.rules) | Security rules to paste in Firestore Console |
| [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md) | Quick Firebase setup |
| [`docs/FIRESTORE_SETUP_DETAILED.md`](docs/FIRESTORE_SETUP_DETAILED.md) | Detailed step-by-step guide with images |

## Still Stuck?

Check these:
1. Are you using the correct Firebase project?
2. Did you paste rules correctly and click Publish?
3. Is your device connected and app running the latest APK?
4. Try full restart: Close app, restart phone, reopen app

If all else fails, see the **Detailed Setup Guide** in [`docs/FIRESTORE_SETUP_DETAILED.md`](docs/FIRESTORE_SETUP_DETAILED.md).
