# Development roadmap

## Milestone 1 — Android foundation and local profiles
- [x] Kotlin + Jetpack Compose Android app shell
- [x] Multiple local profiles with create/edit/delete
- [x] Room persistence
- [x] Android Keystore AES-GCM encryption for profile payloads
- [x] GitHub Actions debug build workflow
- [ ] Confirm successful CI build and test on a physical Android device

## Milestone 2 — Document scanner and OCR
- [ ] CameraX capture
- [ ] Crop and rotate image
- [ ] On-device ML Kit text recognition
- [ ] Candidate field extraction
- [ ] User review and correction before saving
- [ ] Delete temporary image by default

## Milestone 3 — Smart Keyboard
- [ ] Android InputMethodService and keyboard settings entry
- [ ] QWERTY, numeric, email, and phone layouts
- [ ] Profile picker and explicit field insertion
- [ ] Use InputConnection.commitText(); do not use clipboard as the primary insertion mechanism
- [ ] Backspace, cursor, multiline, and keyboard switching
- [ ] Never record keystrokes or typed-text history

## Milestone 4 — Context-aware suggestions
- [ ] Use EditorInfo input type and available field hints
- [ ] User-confirmed field mappings
- [ ] Avoid guessing when confidence is low
- [ ] Manual confirmation before sensitive field insertion

## Milestone 5 — Compatibility and hardening
- [ ] Test standard Android fields, Chrome Android, and supported WebViews
- [ ] Test offline operation, encrypted persistence, and key invalidation handling
- [ ] Test unsupported/custom fields and keyboard fallback
- [ ] Review backup, logging, and privacy behavior

## Milestone 6 — Future desktop support
- [ ] Chrome extension
- [ ] Optional, opt-in cloud sync only after a separate security review
