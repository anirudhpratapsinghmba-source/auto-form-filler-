# CSC Smart Form Filler

A privacy-first assisted form-filling project for CSC operators. The existing Android app remains in place and this repository now contains the first desktop Chrome extension scaffold.

## Product direction

- Android companion: import a document image, extract text on-device, review/edit candidate details, and save an encrypted local profile.
- Desktop Chrome extension: inspect visible fields on the current tab, let the operator review candidate values, and fill only safely matched standard fields after an explicit click.
- Future: document categories, richer field extraction, photo/signature handling, portal-specific mappings, Android keyboard insertion, and an optional secure local bridge between Android and desktop.

## Android status

- Multiple encrypted local candidate profiles.
- Document image selection using Android's system photo picker.
- On-device ML Kit OCR for Latin and Devanagari scripts.
- Editable review fields before saving a candidate.
- OCR is assistive, not authoritative; verify against the source document.

## Desktop extension status

Load the `desktop-extension/` folder as an unpacked extension in Chrome at `chrome://extensions` with Developer mode enabled.

1. Open a supported form page.
2. Click the extension and choose **Detect form fields**.
3. Enter or review the candidate values in the popup.
4. Click **Fill reviewed fields** and verify every value on the page.

The first extension version uses Chrome's `activeTab` permission, does not request broad host access, excludes password/hidden/file/submit controls, and never submits a form. It matches common field labels heuristically, so it will not work on every portal or custom widget. CAPTCHA, OTP, sign-in, and portal security controls remain manual.

## Privacy and safety

- Candidate information stays on the Android device or in the extension popup session in this first version.
- No cloud sync, analytics, or remote profile transfer.
- Do not store passwords, OTPs, payment credentials, or authentication secrets.
- Highly sensitive identifiers such as Aadhaar/PAN require separate product, consent, legal, and security review before support is added.
- Never bypass CAPTCHA, OTP, login, rate limits, or portal anti-automation restrictions.
- Never auto-submit a form. The operator remains responsible for reviewing and submitting.

## Build

Android debug APK is built by GitHub Actions on pushes and manual workflow dispatch. The desktop extension is loaded directly in Chrome and does not require a build step.
