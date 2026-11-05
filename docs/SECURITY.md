# Privacy and security requirements

- Local-first: no remote profile storage, analytics, or personal-data transmission in the initial release.
- Sensitive profile values must be encrypted at rest using keys protected by Android Keystore before persistent storage is enabled.
- Do not store passwords, OTPs, payment credentials, or keystroke history.
- Do not capture or log text typed into other apps.
- Ask for explicit confirmation before inserting sensitive identifiers.
- OCR output is untrusted until the user reviews and confirms it.
- Do not automatically submit forms.
- CAPTCHA, OTP, authentication, and portal security controls must remain under the user's control; do not bypass them.
- The keyboard is opt-in and must be enabled by the user in Android system settings.
