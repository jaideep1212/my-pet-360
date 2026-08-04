# UI reference and packages

---

## Screen blueprints

The file mypaw360.zip contains a Kotlin Android app whose screens are the
visual reference for MyPet360. **Do not use any of the Kotlin code** — treat
it purely as a design blueprint.

| Reference screen | Build it in |
|---|---|
| LoginScreen | features/auth/ |
| ExploreScreen | features/search/ |
| ProviderDetailScreen | features/booking/ |
| TrackingScreen | features/tracking/ |
| MessageBubble | features/chat/ |
| BookingsHistoryScreen | features/history/ |
| ClientProfileScreen | features/profile/ |
| ProviderProfileScreen | features/profile/ |

---

## Packages

None of these are installed yet — `pubspec.yaml` currently has only the
Flutter SDK, cupertino_icons and flutter_lints. Add them with
`flutter pub add <name>` so versions resolve correctly rather than being
pinned by hand.

| Package | Why we need it |
|---|---|
| firebase_core | Firebase initialisation — required before any other Firebase package |
| firebase_auth | Google Sign-In and session management |
| cloud_firestore | database reads and writes |
| firebase_storage | file uploads (photos, insurance documents) |
| firebase_analytics | event tracking — only after UMP consent |
| firebase_crashlytics | crash reporting — only after UMP consent |
| firebase_messaging | push notifications (FCM) |
| google_sign_in | Google OAuth flow |
| google_maps_flutter | interactive maps and walker pins |
| geolocator | device GPS access |
| geoflutterfire_plus | Firestore geo radius queries using geohash |
| intl | date formatting and sv-SE localisation |

### Things to watch out for

- **google_sign_in needs SHA-1 fingerprints registered in Firebase.** Without
  them it fails at runtime with `ApiException: 10`, an error that says nothing
  about the real cause. Three are needed eventually: debug keystore, release
  keystore, and Play App Signing.
- **google_maps_flutter needs a Maps API key** in AndroidManifest.xml, and the
  Maps SDK for Android enabled in both GCP projects.
- **Analytics and Crashlytics must not initialise before UMP consent** — see
  the GDPR rules in CLAUDE.md.
- Adding Firebase packages activates the `com.google.gms.google-services`
  Gradle plugin, which requires `android/app/google-services.json` to exist.
  After that point, local builds fail without it — it is gitignored, so keep
  a copy at C:\mypet360-keys\test\ and \prod\.
