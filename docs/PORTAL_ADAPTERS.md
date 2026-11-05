# Government Exam Portal Adapter Plan

Architecture supports many portal families, but each adapter must be implemented and regression-tested independently.

| Portal family | Typical flow | Rollout |
|---|---|---|
| SSC | Registration/OTR, profile, exam application, uploads, preview | Planned |
| UPSC | Account, common application profile, exam-specific form, uploads | Planned |
| Railway/RRB | Registration, recruitment application, qualification, preferences | Planned |
| NTA | Registration, exam details, qualification, uploads | Planned |
| State PSC/recruitment | State profile, domicile/category, qualification, post preferences | Planned |
| Police/Defence | Personal details, education, eligibility, additional declarations | Planned |

## Adapter checklist
- Confirm official portal host and current form version.
- Use dummy test data only; never commit real candidate documents or credentials.
- Identify labels, accessible names, required indicators, date formats, dropdown options and conditional sections.
- Map controls to canonical CandidateField keys.
- Add explicit conditional-required rules and allowed options.
- Mark CAPTCHA, OTP, password, payment, declaration and submit controls manual-only.
- Add DOM fixtures and regression tests.
- Record last-tested date and known limitations.
- Re-test after portal UI changes.

## Matching precedence
1. Exact portal adapter selector + explicit canonical key.
2. Exact accessible label/name/placeholder alias.
3. Conservative normalized label match.
4. Otherwise unresolved; ask operator.

Never map by field position alone or fuzzy-select ambiguous dropdowns. Never fill hidden or disabled controls.
