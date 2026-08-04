# CI/CD, branch strategy and secrets

---

## Branch strategy
- All feature branches are created FROM main
- All PRs go TO main
- No develop branch — main is the single integration branch
- Branch naming: feature/screen-name (e.g. feature/login, feature/search)
- main is protected by the `protect-main` ruleset: PRs only, no force push,
  no deletion

### Working sequence
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

## The three workflows

### pr_check.yml — the merge gate
Triggers on: every PR to main

| Job | Does |
|---|---|
| analyse | dart format check + flutter analyze --fatal-infos |
| test | flutter test --coverage |
| all-checks-passed | summary job — never rename it |

⚠️ **`all-checks-passed` is not yet registered as a required status check**
in the `protect-main` ruleset. Until it is, a red PR can still be merged.
Register it in Settings → Rules → protect-main after the first PR runs.

Note this workflow does NOT build the Android app, so a broken Gradle
config passes the gate and only fails after merge, in build.yml.

### build.yml — post-merge deployment
Triggers on: push to main, or manual workflow_dispatch

| Job | Does |
|---|---|
| build-test | `--flavor dev --target lib/main_test.dart` → APK → Firebase App Distribution (test-testers) |
| build-prod | `--flavor prod --target lib/main_prod.dart` → AAB → Firebase App Distribution (prod-testers) |

- build-prod has `needs: build-test`, so TEST must succeed first
- build-prod uses `environment: prod`, which requires manual approval
- Artifact paths matter and differ by type:
  - APK: `build/app/outputs/flutter-apk/app-dev-release.apk`
  - AAB: `build/app/outputs/bundle/prodRelease/app-prod-release.aab`
  (Flutter flattens flavored APKs; Gradle nests AABs under `<flavor><BuildType>`)

### release.yml — Play Store release
Triggers on: manual workflow_dispatch only

Builds a signed App Bundle and uploads to the Play Store **internal** track.

⚠️ Two open issues:
- `userFraction: 0.1` (staged rollout) is a production-track feature and is
  likely rejected on the internal track. Decide: drop userFraction, or
  switch to `track: production`.
- The `release_notes` input is collected but never used — nothing passes it
  to the Play upload.

Requires a Google Play developer account, an app created in Play Console,
and one manual upload first (the API cannot create an app's first release).

---

## GitHub environments

| Environment | Protection |
|---|---|
| test | none |
| prod | required reviewer (jaideep1212) + deployment branch rule: `main` only |

The PROD approval gate lives in the environment settings, not in the YAML.
`environment: prod` alone does nothing if the environment has no rules.

---

## Secrets layout

Secrets are **environment-scoped**. Both environments use the same names
with different values — that is what keeps TEST and PROD separate. Always
pass `--env` when setting them; a repository-level secret of the same name
would be visible to both jobs and silently mix the environments.

### Environment: test (3)
| Secret | Source |
|---|---|
| GOOGLE_SERVICES_JSON | Firebase console, mypet360-test |
| FIREBASE_ANDROID_APP_ID | Firebase console → Project settings |
| FIREBASE_SERVICE_ACCOUNT | GCP IAM, role: Firebase App Distribution Admin |

### Environment: prod (7)
| Secret | Source |
|---|---|
| GOOGLE_SERVICES_JSON | Firebase console, mypet360-prod |
| FIREBASE_ANDROID_APP_ID | Firebase console → Project settings |
| FIREBASE_SERVICE_ACCOUNT | GCP IAM, role: Firebase App Distribution Admin |
| KEYSTORE_BASE64 | base64 of mypet360.keystore |
| KEYSTORE_PASSWORD | chosen at keytool -genkey |
| KEY_PASSWORD | same as above |
| KEY_ALIAS | mypet360 |

### Not yet created
| Secret | Needed by |
|---|---|
| PLAY_STORE_SERVICE_ACCOUNT | release.yml — requires a Play developer account |

---

## Signing

The keystore lives only in the KEYSTORE_BASE64 secret and on the developer
machine at C:\mypet360-keys\mypet360.keystore.

CI decodes it to `android/app/mypet360.keystore` and writes
`android/key.properties`, which `android/app/build.gradle.kts` reads. When
that file is absent (local machine, and the TEST job) the build falls back
to debug signing.

⚠️ Losing mypet360.keystore after publishing to Play means never being able
to update the app again. Back it up outside the repo.
