# CallGuard

> **Keep this GitHub repo PRIVATE.** It contains `callguard.jks`, the signing key both apps share (needed so Device IDs stay stable across builds). Never add your private license key (`PRIVATE_KEY_keep_secret.txt`) to the repo.

Offline Android call filter. You create rules like "starts with 020"; the phone silences or blocks matching incoming calls. No internet permission, no account, no server.

## Build with GitHub (no Android Studio needed)

1. Create a new GitHub repo and push the contents of this folder to the `main` branch.
2. Open the **Actions** tab. The "Build APK" workflow runs automatically (or press **Run workflow**).
3. When it finishes, open the run and download the **CallGuard-debug-apk** artifact (a zip containing `app-debug.apk`).
4. Copy the APK to your phone and install it (allow "install unknown apps" when asked).
5. Open CallGuard, tap **Turn on** and choose CallGuard as your call screening app.

The workflow runs the unit tests first. Test results are attached as the **test-report** artifact.

## Open in Android Studio

Open the folder in Android Studio (Ladybug or newer). It will create the Gradle wrapper for you.

## Requirements

Android 10 (API 29) or newer. Android 10 is where the call-screening role was introduced.

## Project layout

- `domain/` pure Kotlin: `PhoneNumberNormalizer`, `BlockingEngine`, models (unit tested)
- `data/` Room database, preferences
- `service/CallGuardScreeningService` Android call screening entry point
- `ui/` Jetpack Compose screens and ViewModel

## Notes for a Play Store release

- Build a signed release with your own keystore (not included).
- Privacy policy: the app collects nothing and has no INTERNET permission.
- `READ_CONTACTS` is optional and used only for "Never block my contacts".
- A "recent calls" shortcut for creating rules needs `READ_CALL_LOG`, a restricted permission on Play, so it is left out of v1.

## Licensing

- CallGuard is locked until an activation code for that phone is entered. It contains only the public key (`license/LicenseConfig.kt`).
- Codes are made with the separate **CallGuard Keys** project, which holds the private key. Build and keep it separately.
- Set your WhatsApp number in `LicenseConfig.kt` (`SELLER_WHATSAPP`, digits only) to show a "Get my code on WhatsApp" button.
- Device ID uses Android's `ANDROID_ID`: stable across restarts, updates and reinstalls; changes on factory reset or if the app is signed with a different key (this is why `callguard.jks` is in the repo, so keep the repo private).
- If you ever replace the key pair, update `PUBLIC_KEY_B64` and the known-vector unit test.
