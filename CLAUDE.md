# MyPet360 — CLAUDE.md

## How to work with me
- Before writing any code, tell me **what you plan to create**, list every
  file you will touch, and wait for me to say "go ahead".
- After each file, **stop and wait** for me to confirm I have read it.
- **Explain every file you create** as if I have never written Flutter before.
  Go section by section. Tell me what each block does in plain English.
- When you use a Flutter concept for the first time (Widget, StatelessWidget,
  BuildContext, StreamBuilder, etc.) define it in one sentence before using it.
- Never generate more than one screen or one module per conversation.
- Never write my Git commit messages. I write those myself.
- If I ask "why did you do it this way?", stop and explain before continuing.
- If there is more than one way to solve something, tell me the options and
  the tradeoff before picking one.
- After generating code, always tell me the exact command to run it on the
  emulator so I can see it working immediately.
- When you add a new Flutter package, explain what it does and why we need it.
- Point out anything I should watch out for or that could break later.

---

## Project overview
MyPet360 is a marketplace Flutter app connecting dog owners with dog
walkers and sitters in Malmö, Sweden. Android first (Phase 1), iOS
second (Phase 2), website third (Phase 3). Zero commission model.
Revenue from AdMob and direct pet brand ads.

---

## Environments
There are two completely separate environments. Never mix them.

### TEST
- Firebase project: MyPet360-test
- Flutter flavor: test
- Entry point: lib/main_test.dart
- Firebase config: lib/firebase_options_test.dart
- Android app ID: com.MyPet360.MyPet360.test
- App name on device: "MyPet360 TEST"
- App icon: visually distinct color from PROD so I can tell them apart
- Firestore data: fake seed data only — never real users
- AdMob: test ad unit IDs only
- FCM: test devices only
- Purpose: every merged feature lands here first automatically

### PROD
- Firebase project: MyPet360-prod
- Flutter flavor: prod
- Entry point: lib/main_prod.dart
- Firebase config: lib/firebase_options_prod.dart
- Android app ID: com.MyPet360.MyPet360
- App name on device: "MyPet360"
- App icon: standard MyPet360 branding
- Firestore data: real user data — treat with care
- AdMob: real ad unit IDs
- FCM: all users
- Purpose: deployed only after manual approval in GitHub

### Running locally
# TEST
flutter run --flavor test --target lib/main_test.dart

# PROD
flutter run --flavor prod --target lib/main_prod.dart

---

## Tech stack
- Flutter 3.44.0 (stable channel) · Dart 3.x
- Firebase: Firestore, Auth (Google Sign-In), Storage, Cloud Functions,
  FCM, Crashlytics, Analytics
- Google Maps Flutter plugin — geo search and live walker tracking
- GCP europe-west1 region for all Firebase services (EU data residency,
  required for GDPR compliance in Sweden)
- GitHub Actions CI/CD:
    PR to main      → pr_check.yml (gate — must pass to merge)
    Merge to main   → build.yml (auto TEST, manual-approved PROD)
    Play Store      → release.yml (manual trigger only)

---

## Branch strategy
- All feature branches are created FROM main
- All PRs go TO main
- No develop branch — main is the single integration branch
- Branch naming: feature/screen-name (e.g. feature/login, feature/search)
- main is protected: no direct pushes, PRs only, CI must pass

### Working sequence (do this every time)
1. git checkout main && git pull origin main
2. git checkout -b feature/your-feature-name
3. Build the feature (one screen or module at a time)
4. git add . && git commit -m "your message in plain English"
5. git push origin feature/your-feature-name
6. Open PR on GitHub: feature/... → main
7. Wait for CI to pass (pr_check.yml)
8. Merge on GitHub
9. build.yml auto-deploys to TEST, then waits for PROD approval

---

## CI/CD pipelines
Three workflow files live in .github/workflows/

### pr_check.yml — the merge gate
Triggers on: every PR to main
Jobs:
  analyse  → dart format check + flutter analyze --fatal-infos
  test     → flutter test --coverage
  all-checks-passed → summary job (this is the name registered in the
                       GitHub ruleset — never rename it)
Rule: PR cannot merge if any of these fail.

### build.yml — post-merge deployment
Triggers on: push to main (i.e. after a PR merges)
Jobs:
  build-test → builds TEST APK → Firebase App Distribution (test-testers group)
               uses: test environment secrets, TEST google-services.json
  build-prod → builds PROD App Bundle → Firebase App Distribution
               (prod-testers group) — requires manual approval in GitHub
               environment before it runs
               uses: prod environment secrets, PROD google-services.json

### release.yml — Play Store release
Triggers on: manual workflow_dispatch only (you trigger this)
Requires: PROD environment approval
Builds signed App Bundle → uploads to Play Store internal track
10% rollout first (userFraction: 0.1)

---

## GitHub secrets layout
Environment: test
  GOOGLE_SERVICES_JSON        TEST project google-services.json contents
  FIREBASE_ANDROID_APP_ID     TEST Android app ID (1:xxx:android:xxx)
  FIREBASE_SERVICE_ACCOUNT    TEST GCP service account JSON

Environment: prod
  GOOGLE_SERVICES_JSON        PROD project google-services.json contents
  FIREBASE_ANDROID_APP_ID     PROD Android app ID
  FIREBASE_SERVICE_ACCOUNT    PROD GCP service account JSON
  KEYSTORE_BASE64             Signing keystore base64 encoded
  KEYSTORE_PASSWORD           Keystore password
  KEY_ALIAS                   Key alias (MyPet360)
  KEY_PASSWORD                Key password
  PLAY_STORE_SERVICE_ACCOUNT  Play Store service account JSON

---

## Firestore collections
All collections exist in BOTH Firebase projects (TEST and PROD).
In TEST they contain seed/fake data. In PROD they contain real user data.

### users/{uid}
uid              String    Firebase Auth UID — document ID
displayName      String    user's full name
email            String    from Google Sign-In
photoURL         String    Firebase Storage path to profile photo
role             String    "owner" | "walker" | "both"
fcmToken         String    push notification token — updated on each login
consentGranted   Boolean   GDPR UMP consent — must be true before any
                           tracking or AdMob fires
createdAt        Timestamp when the account was created
locale           String    "sv-SE" for Swedish users

### walkers/{uid}
uid              String    same UID as users/{uid} — walker has docs in both
bio              String    short description of the walker
experienceYears  Int       years of experience with dogs
serviceTypes     Array     ["walk", "board", "groom", "sit"] — what they offer
serviceArea      GeoPoint  walker's home/base location
geohash          String    geohash of serviceArea — used for radius queries
radiusKm         Int       how far they are willing to travel
hourlyRateSEK    Int       rate in Swedish Krona — always integer, never float
available        Boolean   true if currently accepting bookings
verified         Boolean   set by admin only — verified walkers shown first
rating           Double    average of all reviews (recomputed by Cloud Function)
reviewCount      Int       total number of reviews received

### pets/{ownerId}/{petId}
petId            String    auto-generated Firestore document ID
name             String    pet's name
breed            String    dog breed
ageYears         Int       age in years
weightKg         Double    weight in kilograms
photoURL         String    Firebase Storage path
healthCondition  String    any medical conditions the walker must know about
notes            String    anything else the owner wants to tell the walker
activityLevel    String    "low" | "medium" | "high"
vaccinationStatus   String "up_to_date" | "overdue" | "unknown"
vaccinationExpiryDate Timestamp when vaccinations expire
insuranceDetails String    insurance provider and policy number
insuranceDocURLs Array     Storage paths to uploaded insurance documents

### bookings/{bookingId}
bookingId        String    auto-generated Firestore document ID
ownerId          String    UID of the dog owner
walkerId         String    UID of the walker/sitter
petId            String    reference to pets/{ownerId}/{petId}
serviceType      String    "walk" | "board" | "groom" | "sit"
status           String    "pending" → "confirmed" → "active" →
                           "completed" | "cancelled"
scheduledAt      Timestamp when the walk/service is booked for
durationMins     Int       length of the service in minutes
meetingPoint     GeoPoint  where owner and walker meet
totalSEK         Int       total cost in SEK — always integer
trackingProgress Double    0.0 to 1.0 — updated by walker app during service
currentGeoPoint  GeoPoint  walker's live GPS position — updated every 30s
                           during active status

### chats/{bookingId}/messages/{msgId}
msgId            String    auto-generated Firestore document ID
uid              String    UID of who sent the message
text             String    message content
createdAt        Timestamp when sent
geoPoint         GeoPoint  optional — set when user shares their location
type             String    "text" | "location" | "auto"
                           auto = milestone message sent by Cloud Function
milestoneKey     String    optional — e.g. "walk_started", "halfway", "done"

### reviews/{reviewId}
reviewId         String    auto-generated Firestore document ID
bookingId        String    which booking this review is for
fromUid          String    UID of the reviewer
toUid            String    UID of the person being reviewed
rating           Int       1 to 5 — always integer
comment          String    optional written review
createdAt        Timestamp when submitted
Note: a Cloud Function recomputes walkers/{uid}.rating and reviewCount
      every time a new review document is created.

### ads/{adId}
adId             String    auto-generated Firestore document ID
type             String    "brand" | "product" | "adsense"
imageURL         String    Storage path to ad creative
linkURL          String    where the ad links to
placement        String    which screen slot this ad appears in
active           Boolean   only active ads are shown
priority         Int       higher number = shown before lower priority ads
                           brand ads always shown before AdSense fallback
startsAt         Timestamp campaign start date
endsAt           Timestamp campaign end date

---

## Folder structure
lib/
  main_test.dart          entry point for TEST flavor
  main_prod.dart          entry point for PROD flavor
  firebase_options_test.dart   generated by flutterfire configure (TEST)
  firebase_options_prod.dart   generated by flutterfire configure (PROD)

  features/
    auth/                 login screen, Google Sign-In, UMP consent
    profile/              owner profile, walker profile, pet profile creation
    search/               explore screen, map, filters, walker cards
    booking/              provider detail, booking form, confirmation
    tracking/             live walker tracking screen
    chat/                 real-time chat interface
    history/              bookings history list
    admin/                admin-only screens (ad management, user verification)

  core/
    env/                  Environment enum (test | prod), config helpers
    firebase/             firebase.dart, firestore query helpers
    models/               Dart data classes for every Firestore collection
    theme/                colors, text styles, Material 3 theme
    widgets/              shared UI components used across features

---

## UI reference
The file mypaw360.zip contains a Kotlin Android app with screens to use
as visual reference for every screen in MyPet360. Do not use any of the
Kotlin code — only use the UI as a design blueprint. The screens to
reference are:
  LoginScreen          → features/auth/
  ExploreScreen        → features/search/
  ProviderDetailScreen → features/booking/
  TrackingScreen       → features/tracking/
  MessageBubble        → features/chat/
  BookingsHistoryScreen → features/history/
  ClientProfileScreen  → features/profile/
  ProviderProfileScreen → features/profile/

---

## Colour palette
Primary:     #2D6A4F   forest green — buttons, headers, active states
Accent:      #F4A261   warm orange — CTAs, highlights, TEST app icon tint
Background:  #FAFAF8   off-white — all screen backgrounds
Text:        #1A1A1A   near-black — all body text
Error:       #C0392B   red — validation errors, failed states
Success:     #27AE60   green — confirmed bookings, passed checks

---

## Rules — always follow these
GDPR
  - Google UMP consent must be shown before Firebase Analytics,
    Crashlytics, or AdMob initialise
  - consentGranted must be true in users/{uid} before any tracking fires
  - All user data stays in GCP europe-west1 (Belgium) — EU soil

Money
  - All amounts in Swedish Krona (SEK)
  - Always store as Int — never Double or String
  - Display format: "150 kr" — no decimals

Location
  - Never hardcode coordinates
  - Always use the device GPS via geolocator package
  - Geohash every GeoPoint stored in walkers collection (for radius queries)

Code quality
  - dart format must pass with zero changes
  - flutter analyze --fatal-infos must pass with zero issues
  - Every new function needs at least one unit test
  - Every new screen needs at least one widget test

Flutter patterns
  - Material 3 design throughout
  - Mobile-first, tested on Pixel 8 emulator (Android 14)
  - No hardcoded strings — all user-visible text goes in l10n
  - Use const constructors wherever possible
  - Never put business logic inside Widget build() methods

Environments
  - TEST and PROD Firebase projects must never be mixed
  - Never commit google-services.json — it is gitignored
  - Never commit .env files
  - Never commit keystore files

---

## Packages in use
firebase_core           Firebase initialisation
firebase_auth           Google Sign-In and session management
cloud_firestore         database reads and writes
firebase_storage        file uploads (photos, documents)
firebase_analytics      event tracking (after consent)
firebase_crashlytics    crash reporting (after consent)
firebase_messaging      push notifications (FCM)
google_sign_in          Google OAuth flow
google_maps_flutter     interactive maps and walker pins
geolocator              device GPS access
geoflutterfire_plus     Firestore geo radius queries using geohash
intl                    date formatting, localisation (sv-SE)

---

## What I am building — phase sequence
Phase 1 (now)     Android app — Malmö pilot
Phase 2 (later)   iOS — same Flutter codebase, add ATT consent + APNs
Phase 3 (later)   Website — Next.js 14 + TypeScript, same Firebase backend
