# Auto Form Filler

Android-first smart form filling app. Create reusable local profiles now; document scanning and a custom Android keyboard are planned next.

## Current implementation

- Native Kotlin + Jetpack Compose + Material 3 Android app.
- Multiple profile creation, editing, listing, and deletion.
- Room database for persistent on-device profile records.
- Profile fields: profile name, full name, date of birth, father's name, mobile, email, and address.
- AES-256-GCM encryption using a non-exportable Android Keystore key. Room stores encrypted profile payloads, not plaintext field values.
- GitHub Actions workflow builds the debug APK on pushes and pull requests.

## Planned next

1. CameraX document capture and on-device ML Kit OCR, with manual review before saving.
2. Android Smart Keyboard using InputMethodService and InputConnection.commitText().
3. Context-aware field suggestions and compatibility testing across supported forms.
4. Optional browser extension in a later phase.

## Privacy and safety

- Initial release is local-only; no cloud sync, analytics, or profile upload.
- Do not store passwords, OTPs, or payment credentials.
- CAPTCHA, OTP, sign-in, and other portal security checks remain manual.
- Never auto-submit a form.
- OCR results must be reviewed by the user before saving.
- Aadhaar/PAN or other highly sensitive identifiers should be optional, separately consented, and never automatically suggested.

## Open in Android Studio

Clone this repository and open the project folder in Android Studio. Use JDK 17 and allow Gradle sync. The project currently uses Gradle 8.9 with Android Gradle Plugin 8.7.3.

The debug APK build can also be run from a machine with Gradle 8.9 and Android SDK 35 installed:

```bash
gradle :app:assembleDebug
```

> Build status is not considered verified until the GitHub Actions workflow completes successfully.
