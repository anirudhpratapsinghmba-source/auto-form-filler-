# Development roadmap

## Milestone 1 — Android foundation and local profiles
- [x] Kotlin + Jetpack Compose Android app shell
- [x] Multiple local profiles with create/edit/delete
- [x] Room persistence
- [x] Android Keystore AES-GCM encryption for profile payloads
- [x] GitHub Actions debug build workflow

## Milestone 2 — Android document import and review
- [x] Pick document image using Android system photo picker
- [x] On-device ML Kit OCR for Latin and Devanagari
- [x] Editable review screen before saving candidate details
- [x] Show extracted source text to support manual verification
- [ ] CameraX live capture, crop, rotate, and document quality guidance
- [ ] Better document-type-specific extraction and confidence indicators
- [ ] Secure temporary image lifecycle and explicit document retention controls

## Milestone 3 — Desktop Chrome extension MVP
- [x] Manifest V3 extension scaffold
- [x] Scan visible standard fields on the active tab
- [x] Candidate value review in popup before filling
- [x] Explicit operator click required to fill
- [x] Exclude password, hidden, file, and submit controls
- [ ] Better label/placeholder and Hindi field mapping
- [ ] Portal-specific adapters with compatibility tests
- [ ] Secure Android-to-desktop candidate transfer, designed separately

## Milestone 4 — Android Smart Keyboard
- [ ] Android InputMethodService and keyboard settings entry
- [ ] QWERTY, numeric, email, and phone layouts
- [ ] Candidate picker and explicit field insertion using InputConnection.commitText()
- [ ] No clipboard dependency and no keystroke logging

## Milestone 5 — CSC operator product hardening
- [ ] Multi-candidate workflow and candidate search
- [ ] Photo/signature/document attachment management
- [ ] Field-level source traceability and OCR confidence
- [ ] Operator audit trail without recording sensitive typed content
- [ ] Local backup/export with encryption and consent
- [ ] Accessibility, usability, security, and portal compatibility testing

## Guardrails
- CAPTCHA, OTP, login, and portal security checks remain manual.
- Never auto-submit forms or claim universal portal compatibility.
- Candidate data is personal information: collect only what is necessary, obtain consent, restrict access, and define retention/deletion procedures.
