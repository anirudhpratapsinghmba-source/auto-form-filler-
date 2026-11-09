# CSC Smart Form Filler — Chrome Extension

## Install for local testing
1. Download or clone this repository on the CSC computer.
2. Open `chrome://extensions` in Chrome.
3. Enable **Developer mode**.
4. Choose **Load unpacked** and select the `desktop-extension` folder.

## Transfer a candidate from Android
1. In the Android app, open the saved candidate and choose **Export**.
2. Set and confirm a strong passphrase (12–128 characters).
3. Save the `.cscprofile` encrypted export on the CSC computer using a trusted transfer method.
4. In the extension popup, choose that file and enter the same passphrase.
5. Select only candidate values you have re-checked against the original documents.
6. Open a supported exam form, scan its visible fields, and choose **Review & fill selected fields**.
7. Review every value on the portal. Complete login, password, OTP, CAPTCHA, payment, declarations and final submission manually.

## Privacy and limitations
- The export uses PBKDF2-HMAC-SHA256 (310,000 iterations) and AES-256-GCM. The passphrase is not stored.
- Decryption runs locally in the extension popup. Candidate details are held in popup memory only and are not written to extension storage or sent to a server.
- Sensitive identity and certificate numbers are excluded from autofill.
- The extension uses `activeTab` and `scripting`; it does not request broad host permissions.
- It supports ordinary visible HTML inputs, textareas and exact-match select options. Custom widgets, embedded frames and complex portal components may not work.
- It never fills password, hidden, file, CAPTCHA or submit controls and never submits a form.
- Delete the encrypted transfer file after importing it on the CSC computer. Anyone who obtains the file and guesses its passphrase may be able to decrypt it.
