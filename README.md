# Auto Form Filler

Android-first smart form filling app with local profiles, document OCR, and a custom keyboard.

## Project status

This repository is being initialized as a native Android application. The first milestone is the Android foundation and secure local profile manager. Camera OCR and the smart keyboard will be added in subsequent milestones.

## Planned stack

- Kotlin + Jetpack Compose + Material 3
- MVVM architecture
- Room local database
- Android Keystore-backed encryption for sensitive profile data
- CameraX + ML Kit Text Recognition (planned)
- Android InputMethodService smart keyboard (planned)

## Safety and privacy

- Local-first: no cloud upload or analytics in the initial release.
- Do not store or suggest passwords, OTPs, or payment credentials.
- CAPTCHA, OTP, authentication, and portal security checks remain manual.
- Never automatically submit a form.
- OCR results must be reviewed by the user before saving.

## Open in Android Studio

Open this repository as a project in Android Studio, allow Gradle sync, then run the `app` configuration on an Android device or emulator.
