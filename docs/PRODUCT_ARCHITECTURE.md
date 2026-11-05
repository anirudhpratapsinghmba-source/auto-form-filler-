# CSC Smart Form Filler — Product Architecture

## Product scope
- Platforms: Android document companion + desktop Chrome extension.
- Inputs: PDF and image files (JPG/JPEG/PNG); camera capture is an Android intake path.
- Mode: review-first assisted filling. No silent writes, automatic declarations, payments, or final submission.
- Coverage: exam-agnostic architecture; portal adapters are implemented and tested independently for SSC, UPSC, Railway/RRB, NTA, state recruitment and admissions.

## End-to-end workflow
1. Operator creates a candidate case and selects one or more documents.
2. Intake records document type, source, page count and a local case identifier.
3. OCR runs on-device. PDF pages are rendered locally before OCR; images use on-device OCR.
4. Classifier proposes a type: 10th/12th/graduation marksheet, photo, signature, caste, domicile, identity, disability or other.
5. Extractor proposes canonical field/value pairs with source document/page and OCR confidence.
6. Normalizer standardizes dates, whitespace, phone digits and marks while retaining original text.
7. Reconciliation compares repeated values across documents. Conflicts require operator choice; never silently choose a source.
8. Candidate profile stores values encrypted locally with provenance and verification status.
9. Desktop inspector detects visible controls, required indicators, labels, constraints, options and current section.
10. Portal adapter maps controls to canonical fields using explicit rules and conservative label matching.
11. Review shows portal field, proposed value, source document/page, confidence, validation and operator action.
12. Operator approves selected values; extension fills supported controls. CAPTCHA, OTP, login, payment, declaration and final submit remain manual.

## Canonical data groups
Identity: name, name-as-per-10th, DOB, gender, nationality, marital status.
Family: father and mother names.
Contact: mobile, email.
Address: address, state, district, PIN, domicile state.
Reservation/accessibility: category, disability status and percentage.
Class 10: board, roll, passing year, marks.
Class 12: board, roll, passing year, stream, marks.
Higher education: university, course, passing year, percentage/CGPA.
Assets: photo and signature references.
Certificate references: caste and domicile certificate numbers; identity numbers are separately protected and excluded from suggestions by default.

## Required-field intelligence
A field can be required through HTML required/aria-required attributes, visible required markers, adapter rules or portal validation feedback. These signals differ:
- DOM required flag: strong structural signal.
- Portal adapter rule: known requirement for a specific form/version.
- Visual asterisk/label: heuristic requiring operator confirmation.
- Server validation: report returned after an operator-controlled step; do not trigger repeated submissions.

Unknown required fields are unresolved. Do not invent values. Conditional requirements (category, gender, disability, qualification, post, state) are evaluated only when the operator chooses the relevant option.

## Portal adapter contract
Each adapter defines stable portal identifier and host/path patterns; form/version and last-tested date; canonical mappings and aliases; conditional required rules; date/number/option/file constraints; unsupported controls and manual-only steps; fixture pages and regression tests.

Adapters are versioned and fail closed when page structure changes. A shared field catalog is not proof of portal compatibility.

## Privacy and operator control
All parsing and candidate data processing are local in the first release. Encrypt persisted candidate values and document-derived data at rest. No cloud sync, analytics, remote OCR or broad browser host permissions without separate security review. Do not persist raw documents longer than the operator-selected retention period. Never store passwords, OTPs, payment credentials or session cookies. Mask identity numbers and exclude them from suggestions unless explicitly revealed and approved.

Never bypass CAPTCHA, OTP, login, anti-bot controls or portal restrictions. Never automate payment, declaration acceptance or final submission. Never infer category or eligibility from name, appearance or unrelated documents. Operator and candidate verify originals before filling and final submission.
