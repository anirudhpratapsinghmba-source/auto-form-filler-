# CSC Smart Form Filler

A privacy-first assisted form-filling system for CSC operators. The repository contains an Android document companion and a desktop Chrome extension.

## Product principles

- Review-first workflow: OCR and field matching propose values; an operator verifies them before use.
- Local-first privacy: candidate data is processed on-device and is not uploaded to a cloud service in this release.
- Safe automation: never automate CAPTCHA, OTP, authentication, payment, declarations or final submission.
- Portal-specific compatibility: each supported portal/form version requires its own adapter and regression tests. A shared field catalog does not guarantee universal compatibility.
- Preserve originals: document/photo/signature transformations must create derivatives and never overwrite source files.

## Current Android capabilities

- Multiple encrypted local candidate profiles.
- PDF and image import with on-device OCR (Latin and Devanagari).
- Editable candidate review, source/conflict handling and verification status.
- Encrypted passphrase-protected `.cscprofile` export for desktop transfer.

## Current Chrome extension capabilities

Load `desktop-extension/` as an unpacked extension from `chrome://extensions` with Developer mode enabled.

1. Open a supported application form in a normal Chrome tab.
2. Open the extension popup and import the encrypted `.cscprofile` exported by Android.
3. Enter the export passphrase. Decryption occurs locally in the popup.
4. Review the displayed values and explicitly select each value to approve it for this session.
5. Scan the active page, inspect detected fields, then choose the fill action.
6. Verify every populated value on the portal before continuing.

Manual correction fields are also available. The extension uses `activeTab` and `scripting`, not broad host permissions. It supports standard visible input, textarea and select controls with conservative label matching. Custom widgets, iframes, shadow DOM and changing portal layouts may remain unsupported until a portal-specific adapter is added and tested. Dropdown selection is exact-match only.

## Security and data handling

- Android profile data is encrypted at rest; desktop transfer is encrypted with a user passphrase.
- The extension decrypts the transfer in popup memory only; it does not save candidate profiles or passphrases in extension storage.
- Sensitive identity/certificate fields are excluded from extension autofill.
- Do not add passwords, OTPs, payment credentials, cookies or authentication secrets to candidate profiles.
- Never bypass CAPTCHA, OTP, login protections, rate limits or portal anti-automation controls.
- Never auto-submit forms. The operator remains responsible for reviewing and submitting.
- Use dummy candidate data for development and portal regression testing. Do not commit real documents or candidate details.

## Build and verification

Android debug APKs are built by GitHub Actions on pushes, pull requests and manual workflow dispatch. Download the `auto-form-filler-debug-apk` artifact from a successful workflow run. The Chrome extension currently loads directly from its folder and does not require a build step.

A successful CI build proves compilation only; it does not replace device testing, extension interaction tests or portal-specific regression testing.
