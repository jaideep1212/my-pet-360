# MyPet360

## What This Is
Marketplace Flutter app connecting dog owners with dog walkers/sitters
in Malmö, Sweden. Android-first, iOS in Phase 2, website in Phase 3.
Zero commission. Revenue from AdMob + direct brand ads.

## Tech Stack
- Flutter 3.44 (stable) · Dart 3.x
- Firebase: Firestore, Auth (Google Sign-In), Storage, Functions, FCM, Crashlytics, Analytics
- Google Maps Flutter plugin for geo search and live tracking
- GCP europe-west1 region (all Firebase services)
- GitHub Actions CI/CD → Firebase App Distribution (dev) → Play Store (prod)

## Package ID
- Android: com.mypet360.app
- iOS (Phase 2): com.mypet360.app
- Dart package name: mypet360 (imports: package:mypet360/...)

## Firestore Collections
users/{uid}
  uid, displayName, email, photoURL, role (owner|walker|both)
  fcmToken, consentGranted, createdAt, locale

walkers/{uid}
  uid, bio, experienceYears
  serviceTypes: [walk, board, groom, sit]
  serviceArea (GeoPoint), geohash, radiusKm
  hourlyRateSEK (int), available (bool), verified (bool)
  rating (double), reviewCount (int)

pets/{ownerId}/{petId}
  petId, name, breed, ageYears, weightKg, photoURL
  healthCondition, notes, activityLevel (low|medium|high)
  vaccinationStatus, vaccinationExpiryDate
  insuranceDetails, insuranceDocURLs

bookings/{bookingId}
  bookingId, ownerId, walkerId, petId
  serviceType (walk|board|groom|sit)
  status: pending → confirmed → active → completed | cancelled
  scheduledAt (timestamp), durationMins
  meetingPoint (GeoPoint), totalSEK (int)
  trackingProgress (double 0.0–1.0), currentGeoPoint

chats/{bookingId}/messages/{msgId}
  msgId, uid, text, createdAt (timestamp)
  geoPoint (optional), type (text|location|auto), milestoneKey (optional)

reviews/{reviewId}
  reviewId, bookingId, fromUid, toUid
  rating (int 1–5), comment, createdAt

ads/{adId}
  adId, type (brand|product), imageURL, linkURL
  placement, active (bool), priority (int)
  startsAt, endsAt (timestamps)

## Folder Structure
lib/
  features/
    auth/          ← login, register, onboarding
    profile/       ← owner profile, walker profile, pet profiles
    search/        ← explore screen, filters, map
    booking/       ← provider detail, booking form, confirmation
    tracking/      ← live tracking screen
    chat/          ← chat interface
    bookings/      ← bookings history
  core/
    firebase/      ← firebase.dart, firestore helpers
    models/        ← all data models as Dart classes
    theme/         ← colors, text styles
    widgets/       ← shared UI components

## Colors
Primary:    #2D6A4F (forest green)
Accent:     #F4A261 (warm orange)
Background: #FAFAF8
Text:       #1A1A1A

## Rules
- GDPR: UMP consent before any tracking or AdMob
- All money in SEK as integers (no floats)
- Dates: Europe/Stockholm timezone
- Mobile-first, Material 3 design
- Never hardcode coordinates — always use device GPS
- The mypaw360.zip UI is the visual reference for all screens