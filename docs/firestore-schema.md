# Firestore schema

All collections exist in BOTH Firebase projects (mypet360-test and
mypet360-prod). In TEST they contain seed/fake data. In PROD they contain
real user data.

Both databases are Firestore Native, Standard edition, in europe-west1.

---

## users/{uid}

| Field | Type | Notes |
|---|---|---|
| uid | String | Firebase Auth UID — document ID |
| displayName | String | user's full name |
| email | String | from Google Sign-In |
| photoURL | String | Firebase Storage path to profile photo |
| role | String | "owner" \| "walker" \| "both" |
| fcmToken | String | push notification token — updated on each login |
| consentGranted | Boolean | GDPR UMP consent — must be true before any tracking or AdMob fires |
| createdAt | Timestamp | when the account was created |
| locale | String | "sv-SE" for Swedish users |

---

## walkers/{uid}

| Field | Type | Notes |
|---|---|---|
| uid | String | same UID as users/{uid} — a walker has docs in both |
| bio | String | short description of the walker |
| experienceYears | Int | years of experience with dogs |
| serviceTypes | Array | ["walk", "board", "groom", "sit"] — what they offer |
| serviceArea | GeoPoint | walker's home/base location |
| geohash | String | geohash of serviceArea — used for radius queries |
| radiusKm | Int | how far they are willing to travel |
| hourlyRateSEK | Int | rate in Swedish Krona — always integer, never float |
| available | Boolean | true if currently accepting bookings |
| verified | Boolean | set by admin only — verified walkers shown first |
| rating | Double | average of all reviews (recomputed by Cloud Function) |
| reviewCount | Int | total number of reviews received |

---

## pets/{ownerId}/{petId}

| Field | Type | Notes |
|---|---|---|
| petId | String | auto-generated Firestore document ID |
| name | String | pet's name |
| breed | String | dog breed |
| ageYears | Int | age in years |
| weightKg | Double | weight in kilograms |
| photoURL | String | Firebase Storage path |
| healthCondition | String | any medical conditions the walker must know about |
| notes | String | anything else the owner wants to tell the walker |
| activityLevel | String | "low" \| "medium" \| "high" |
| vaccinationStatus | String | "up_to_date" \| "overdue" \| "unknown" |
| vaccinationExpiryDate | Timestamp | when vaccinations expire |
| insuranceDetails | String | insurance provider and policy number |
| insuranceDocURLs | Array | Storage paths to uploaded insurance documents |

---

## bookings/{bookingId}

| Field | Type | Notes |
|---|---|---|
| bookingId | String | auto-generated Firestore document ID |
| ownerId | String | UID of the dog owner |
| walkerId | String | UID of the walker/sitter |
| petId | String | reference to pets/{ownerId}/{petId} |
| serviceType | String | "walk" \| "board" \| "groom" \| "sit" |
| status | String | "pending" → "confirmed" → "active" → "completed" \| "cancelled" |
| scheduledAt | Timestamp | when the service is booked for |
| durationMins | Int | length of the service in minutes |
| meetingPoint | GeoPoint | where owner and walker meet |
| totalSEK | Int | total cost in SEK — always integer |
| trackingProgress | Double | 0.0 to 1.0 — updated by walker app during service |
| currentGeoPoint | GeoPoint | walker's live GPS — updated every 30s while active |

---

## chats/{bookingId}/messages/{msgId}

| Field | Type | Notes |
|---|---|---|
| msgId | String | auto-generated Firestore document ID |
| uid | String | UID of who sent the message |
| text | String | message content |
| createdAt | Timestamp | when sent |
| geoPoint | GeoPoint | optional — set when user shares their location |
| type | String | "text" \| "location" \| "auto" (auto = milestone from Cloud Function) |
| milestoneKey | String | optional — e.g. "walk_started", "halfway", "done" |

---

## reviews/{reviewId}

| Field | Type | Notes |
|---|---|---|
| reviewId | String | auto-generated Firestore document ID |
| bookingId | String | which booking this review is for |
| fromUid | String | UID of the reviewer |
| toUid | String | UID of the person being reviewed |
| rating | Int | 1 to 5 — always integer |
| comment | String | optional written review |
| createdAt | Timestamp | when submitted |

A Cloud Function recomputes walkers/{uid}.rating and reviewCount every time
a new review document is created.

---

## ads/{adId}

| Field | Type | Notes |
|---|---|---|
| adId | String | auto-generated Firestore document ID |
| type | String | "brand" \| "product" \| "adsense" |
| imageURL | String | Storage path to ad creative |
| linkURL | String | where the ad links to |
| placement | String | which screen slot this ad appears in |
| active | Boolean | only active ads are shown |
| priority | Int | higher number shown first; brand ads always before AdSense fallback |
| startsAt | Timestamp | campaign start date |
| endsAt | Timestamp | campaign end date |
