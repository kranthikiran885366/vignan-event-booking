# Architecture Overview

## Module Map
- App entry and navigation: `MainActivity`, `SplashActivity`, auth activities
- UI layer: fragments in `ui/fragment`, adapters in `ui/`
- State layer: `EquipmentViewModel`, `EquipmentUiState`
- Data layer: Firebase services and repository helpers
- Utility layer: validation and seeding helpers

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

## Runtime Sequence
1. App initializes Firebase and attempts equipment seed if collection is empty.
2. Home subscribes to realtime equipment updates.
3. User actions (search/filter/reserve) update ViewModel state.
4. Reserve/cancel runs in Firestore transaction to keep quantity consistent.
5. Bookings/Profile screens react to realtime updates from shared state.

## Services
- Firebase Auth: login/register/session
- Firestore: equipment, bookings, users
- Storage: image URL retrieval from imagePath
- FCM: notification receive and token update

## Validation
- Shared validation utility for email, text sanitization, booking date window
- Screen-level strict checks before network operations
