# Architecture Overview

## Active UI Flow
- SplashActivity -> LoginActivity/RegisterActivity/MainActivity
- MainActivity hosts HomeFragment, BookingsFragment, ProfileFragment
- EquipmentDetailActivity and BookingSuccessActivity for detailed flow

## State + Data
- EquipmentViewModel owns UI state and realtime subscriptions
- Firestore snapshot listeners update:
  - equipment list
  - bookings history for current user
- Firestore transactions guarantee stock consistency for reserve/cancel

## Services
- Firebase Auth: login/register/session
- Firestore: equipment, bookings, users
- Storage: image URL retrieval from imagePath
- FCM: notification receive and token update

## Validation
- Shared validation utility for email, text sanitization, booking date window
- Screen-level strict checks before network operations
