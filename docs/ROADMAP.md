# Development roadmap

## Milestone 1 — Android foundation (started)
- [x] Native Kotlin Android project structure
- [x] Jetpack Compose dashboard shell
- [x] Profile list UI and add-profile interaction (in-memory prototype only)
- [ ] Build and run on Android Studio/device

## Milestone 2 — Secure profile storage
- [ ] Define profile and field models
- [ ] Encrypt sensitive values with Android Keystore-backed AES-GCM
- [ ] Persist encrypted records with Room
- [ ] Add edit, delete, search, and profile selection
- [ ] Add explicit consent before inserting sensitive identifiers

## Milestone 3 — Document scanner
- [ ] CameraX capture and crop/rotate
- [ ] ML Kit Text Recognition
- [ ] Candidate field extraction
- [ ] Review/correct screen before saving
- [ ] Delete temporary images by default

## Milestone 4 — Smart keyboard
- [ ] Implement Android InputMethodService
- [ ] QWERTY, number, email, and phone layouts
- [ ] Read safe field hints from EditorInfo
- [ ] Insert user-selected text through InputConnection.commitText()
- [ ] Never log typed content or capture passwords, OTPs, or payment data

## Milestone 5 — Website autofill and compatibility
- [ ] Support only permitted, technically compatible form fields
- [ ] Test on Chrome Android and selected portals
- [ ] Provide manual field mapping when field detection is uncertain
- [ ] Keep CAPTCHA, OTP, login, and security checks manual
- [ ] Never auto-submit a form

## Later
- [ ] Chrome extension
- [ ] Optional encrypted cloud sync (only after explicit product decision)
