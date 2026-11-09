const $ = (id) => document.getElementById(id);
let scannedTabId = null;
let lastScan = [];

const FIELD_RULES = [
  { key: "fullName", label: "Full name", pattern: /full.?name|applicant.?name|candidate.?name|student.?name|पूरा नाम|आवेदक का नाम|अभ्यर्थी का नाम|^name$/ },
  { key: "dateOfBirth", label: "Date of birth", pattern: /date.?of.?birth|\bdob\b|birth.?date|जन्म.?तिथि|जन्मतिथि/ },
  { key: "fatherName", label: "Father's name", pattern: /father.?name|पिता.?का.?नाम|पिता/ },
  { key: "motherName", label: "Mother's name", pattern: /mother|माता/ },
  { key: "mobile", label: "Mobile", pattern: /mobile|phone|contact.?number|मोबाइल|दूरभाष/ },
  { key: "email", label: "Email", pattern: /e.?mail|ई.?मेल/ },
  { key: "addressLine", label: "Address", pattern: /address|residential|पता|निवास/ },
  { key: "gender", label: "Gender", pattern: /gender|sex|लिंग/ },
  { key: "category", label: "Category", pattern: /category|reservation|वर्ग|श्रेणी/ },
  { key: "nationality", label: "Nationality", pattern: /nationality|राष्ट्रीयता/ },
  { key: "maritalStatus", label: "Marital status", pattern: /marital|वैवाहिक/ },
  { key: "state", label: "State", pattern: /state|राज्य/ },
  { key: "district", label: "District", pattern: /district|जिला/ },
  { key: "pinCode", label: "PIN code", pattern: /pin.?code|postal.?code|पिन/ },
  { key: "domicileState", label: "Domicile state", pattern: /domicile|मूल.?निवास/ },
  { key: "disabilityStatus", label: "Disability status", pattern: /disability|दिव्यांग/ },
  { key: "disabilityPercentage", label: "Disability percentage", pattern: /disability.?percentage|दिव्यांगता.?प्रतिशत/ },
  { key: "board10th", label: "10th board", pattern: /10(th)?.?(board|school)|matric.?board/ },
  { key: "roll10th", label: "10th roll number", pattern: /10(th)?.?roll|matric.?roll/ },
  { key: "passingYear10th", label: "10th passing year", pattern: /10(th)?.?(passing|pass).?year|matric.?year/ },
  { key: "marks10th", label: "10th marks", pattern: /10(th)?.?marks|secondary.?marks/ },
  { key: "board12th", label: "12th board", pattern: /12(th)?.?(board|school)|senior.?secondary.?board/ },
  { key: "roll12th", label: "12th roll number", pattern: /12(th)?.?roll/ },
  { key: "passingYear12th", label: "12th passing year", pattern: /12(th)?.?(passing|pass).?year/ },
  { key: "stream12th", label: "12th stream", pattern: /12(th)?.?stream|subject.?stream/ },
  { key: "marks12th", label: "12th marks", pattern: /12(th)?.?marks|senior.?secondary.?marks/ },
  { key: "graduationUniversity", label: "Graduation university", pattern: /university|विश्वविद्यालय/ },
  { key: "graduationCourse", label: "Graduation course", pattern: /graduation.?course|degree|qualification/ },
  { key: "graduationPassingYear", label: "Graduation passing year", pattern: /graduation.?passing.?year|degree.?passing.?year/ },
  { key: "graduationPercentage", label: "Graduation percentage", pattern: /graduation.?percentage|degree.?marks|cgpa/ }
];
let importedProfile = null;
let selectedImported = Object.create(null);

async function getActiveTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id || !/^https?:/.test(tab.url || "")) throw new Error("Open the application form in a normal website tab first.");
  return tab;
}

async function inspectPage() {
  const tab = await getActiveTab();
  scannedTabId = tab.id;
  const [{ result }] = await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    func: () => {
      const visible = (el) => {
        const style = getComputedStyle(el);
        const rect = el.getBoundingClientRect();
        return style.visibility !== "hidden" && style.display !== "none" && rect.width > 0 && rect.height > 0;
      };
      return Array.from(document.querySelectorAll("input,textarea,select"))
        .filter(el => visible(el) && !el.disabled && !["hidden","password","file","submit","button","reset"].includes((el.type || "").toLowerCase()))
        .map((el, index) => {
          const label = [
            el.labels ? Array.from(el.labels).map(x => x.innerText).join(" ") : "",
            el.getAttribute("aria-label"), el.getAttribute("aria-labelledby") ? document.getElementById(el.getAttribute("aria-labelledby"))?.innerText : "",
            el.getAttribute("placeholder"), el.getAttribute("name"), el.id,
            el.getAttribute("autocomplete")
          ].filter(Boolean).join(" ").replace(/\s+/g, " ").trim();
          const required = el.required || el.getAttribute("aria-required") === "true" || el.getAttribute("data-required") === "true";
          return {
            index, label: label || "Unlabelled field",
            id: el.id || "", name: el.getAttribute("name") || "",
            type: el.tagName.toLowerCase() === "select" ? "select" : (el.type || el.tagName.toLowerCase()),
            required, pattern: el.getAttribute("pattern") || "", maxLength: el.maxLength > 0 ? el.maxLength : null,
            minLength: el.minLength > 0 ? el.minLength : null, autocomplete: el.getAttribute("autocomplete") || "",
            options: el.tagName.toLowerCase() === "select" ? Array.from(el.options).filter(o => o.value).map(o => o.text.trim()).slice(0, 30) : []
          };
        }).slice(0, 150);
    }
  });
  return result;
}

function matchRule(field) {
  const text = [field.label, field.autocomplete, field.type].join(" ").toLowerCase();
  return FIELD_RULES.find(rule => rule.pattern.test(text)) || null;
}

function renderScan(fields) {
  lastScan = fields;
  const required = fields.filter(f => f.required);
  const mapped = fields.map(f => ({ ...f, rule: matchRule(f) }));
  const requiredMapped = required.filter(f => matchRule(f));
  const unknownRequired = required.filter(f => !matchRule(f));
  $("scanSummary").textContent = fields.length + " visible fields · " + required.length + " required · " + requiredMapped.length + " recognized required";
  $("detected").replaceChildren();
  const list = document.createElement("ul");
  mapped.forEach(field => {
    const li = document.createElement("li");
    const rule = field.rule ? " → " + field.rule.label : " → needs manual mapping";
    li.textContent = (field.required ? "Required · " : "Optional · ") + field.label + rule;
    list.appendChild(li);
  });
  $("detected").appendChild(list);
  $("requiredWarning").textContent = unknownRequired.length
    ? "Required fields needing manual information: " + unknownRequired.map(f => f.label).join(", ")
    : (required.length ? "All detected required fields have a known field type. Values still need validation." : "No required attributes detected. Some portals validate only after interaction.");
}

async function scan() {
  $("pageStatus").textContent = "Detecting fields and required rules…";
  try {
    const fields = await inspectPage();
    renderScan(fields);
    $("pageStatus").textContent = fields.length ? "Page scan complete. Review candidate details before filling." : "No visible standard form fields found.";
  } catch (error) {
    $("pageStatus").textContent = error.message || "Could not inspect this page.";
  }
}

function candidateValues() {
  const manual = Object.fromEntries(new FormData($("candidate")).entries());
  const values = { ...selectedImported };
  const manualMap = { fullName: "fullName", dob: "dateOfBirth", fatherName: "fatherName", mobile: "mobile", email: "email", address: "addressLine" };
  for (const [inputKey, fieldKey] of Object.entries(manualMap)) {
    const value = String(manual[inputKey] || "").trim();
    if (value) values[fieldKey] = value;
  }
  return values;
}

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(normalized + "=".repeat((4 - normalized.length % 4) % 4));
  return Uint8Array.from(binary, ch => ch.charCodeAt(0));
}

async function decryptProfilePackage(file, passphrase) {
  if (file.size > 2 * 1024 * 1024) throw new Error("Profile file is unexpectedly large.");
  const envelope = JSON.parse(await file.text());
  if (envelope.format !== "csc-smart-profile" || envelope.version !== 1 ||
      envelope.kdf !== "PBKDF2-HMAC-SHA256" || envelope.cipher !== "AES-256-GCM" ||
      envelope.iterations !== 310000) throw new Error("Unsupported or invalid encrypted profile format.");
  const material = await crypto.subtle.importKey("raw", new TextEncoder().encode(passphrase), "PBKDF2", false, ["deriveKey"]);
  const key = await crypto.subtle.deriveKey(
    { name: "PBKDF2", salt: decodeBase64Url(envelope.salt), iterations: envelope.iterations, hash: "SHA-256" },
    material, { name: "AES-GCM", length: 256 }, false, ["decrypt"]
  );
  let plaintext;
  try {
    plaintext = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: decodeBase64Url(envelope.iv), tagLength: 128 },
      key, decodeBase64Url(envelope.ciphertext)
    );
  } catch (_) {
    throw new Error("Could not decrypt. Check the passphrase and selected file.");
  }
  const payload = JSON.parse(new TextDecoder().decode(plaintext));
  if (payload.format !== "csc-smart-profile" || payload.version !== 1 || !Array.isArray(payload.fields)) {
    throw new Error("The decrypted file is not a supported candidate profile.");
  }
  return payload;
}

function renderImportedProfile(payload) {
  importedProfile = payload;
  selectedImported = Object.create(null);
  $("profileName").textContent = payload.profileName || "Candidate";
  $("importedFields").replaceChildren();
  let usable = 0;
  payload.fields.forEach(item => {
    if (!item || typeof item.key !== "string" || typeof item.value !== "string" || !item.value.trim() || item.sensitive) return;
    const rule = FIELD_RULES.find(entry => entry.key === item.key);
    const row = document.createElement("label");
    row.className = "importedField";
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.addEventListener("change", () => {
      if (checkbox.checked) selectedImported[item.key] = item.value;
      else delete selectedImported[item.key];
    });
    const details = document.createElement("span");
    const title = document.createElement("strong");
    title.textContent = item.label || rule?.label || item.key;
    const value = document.createElement("small");
    value.textContent = item.value;
    const state = document.createElement("small");
    state.className = "fieldStatus";
    state.textContent = item.status === "OPERATOR_CONFIRMED" ? "Previously confirmed · verify again" : "Unverified · check original document";
    details.append(title, value, state);
    row.append(checkbox, details);
    $("importedFields").appendChild(row);
    usable++;
  });
  $("importedSection").hidden = false;
  $("profileStatus").textContent = usable
    ? usable + " non-sensitive fields decrypted. Select only values checked against original documents."
    : "Profile decrypted, but no supported non-sensitive fields were found.";
}

$("importProfile").addEventListener("click", async () => {
  const file = $("profileFile").files?.[0];
  const passphraseInput = $("profilePassphrase");
  $("profileStatus").textContent = "Decrypting locally…";
  try {
    if (!file || !passphraseInput.value) throw new Error("Choose the encrypted profile and enter its passphrase.");
    if (passphraseInput.value.length < 12 || passphraseInput.value.length > 128) {
      throw new Error("Passphrase must be between 12 and 128 characters.");
    }
    const payload = await decryptProfilePackage(file, passphraseInput.value);
    renderImportedProfile(payload);
  } catch (error) {
    $("profileStatus").textContent = error.message || "Could not import this profile.";
    importedProfile = null;
    selectedImported = Object.create(null);
    $("importedSection").hidden = true;
  } finally {
    passphraseInput.value = "";
  }
});

function validateValue(rule, value, field) {
  if (!value) return "No candidate value supplied";
  if (field.maxLength && value.length > field.maxLength) return "Too long for this field";
  if (field.minLength && value.length < field.minLength) return "Too short for this field";
  if (field.type === "email" && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return "Invalid email format";
  if (field.type === "tel" && value.replace(/\D/g, "").length < 7) return "Phone number looks incomplete";
  if (field.pattern) {
    try { if (!(new RegExp("^(?:" + field.pattern + ")$")).test(value)) return "Does not match portal format"; }
    catch (_) { /* Ignore malformed portal pattern; operator must verify. */ }
  }
  return "";
}

$("scan").addEventListener("click", scan);
$("candidate").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("pageStatus").textContent = "Checking required fields and candidate values…";
  try {
    const tab = await getActiveTab();
    if (tab.id !== scannedTabId || !lastScan.length) await scan();
    const values = candidateValues();
    const mapping = lastScan.map(field => ({ field, rule: matchRule(field) })).filter(x => x.rule);
    const plan = [];
    const missingRequired = lastScan
      .filter(field => field.required && !matchRule(field))
      .map(field => field.label + " (manual mapping required)");
    const invalid = [];
    for (const item of mapping) {
      const value = String(values[item.rule.key] || "").trim();
      if (!value) {
        if (item.field.required) missingRequired.push(item.field.label);
        continue;
      }
      const problem = validateValue(item.rule, value, item.field);
      if (problem) { invalid.push(item.field.label + ": " + problem); continue; }
      plan.push({
        key: item.rule.key, value, type: item.field.type,
        expected: { label: item.field.label, id: item.field.id, name: item.field.name },
        options: item.field.options
      });
    }
    if (missingRequired.length || invalid.length) {
      $("pageStatus").textContent = [
        missingRequired.length ? "Missing required candidate data: " + missingRequired.join(", ") : "",
        invalid.length ? "Please correct: " + invalid.join("; ") : ""
      ].filter(Boolean).join(" ");
      return;
    }
    if (!plan.length) {
      $("pageStatus").textContent = "No safe field matches with non-empty values. Add candidate details or check the portal labels.";
      return;
    }
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      args: [plan],
      func: (fillPlan) => {
        const visibleFields = Array.from(document.querySelectorAll("input,textarea,select"))
          .filter(el => {
            const s = getComputedStyle(el), r = el.getBoundingClientRect();
            return s.visibility !== "hidden" && s.display !== "none" && r.width > 0 && r.height > 0 &&
              !el.disabled && !["hidden","password","file","submit","button","reset"].includes((el.type || "").toLowerCase());
          }).slice(0, 150);
        let filled = 0;
        const skipped = [];
        for (const item of fillPlan) {
          const matches = visibleFields.filter(candidate => {
            const candidateLabel = [
              candidate.labels ? Array.from(candidate.labels).map(x => x.innerText).join(" ") : "",
              candidate.getAttribute("aria-label"),
              candidate.getAttribute("aria-labelledby") ? document.getElementById(candidate.getAttribute("aria-labelledby"))?.innerText : "",
              candidate.getAttribute("placeholder"), candidate.getAttribute("name"), candidate.id,
              candidate.getAttribute("autocomplete")
            ].filter(Boolean).join(" ").replace(/\s+/g, " ").trim() || "Unlabelled field";
            return candidateLabel === item.expected.label &&
              (candidate.id || "") === item.expected.id &&
              (candidate.getAttribute("name") || "") === item.expected.name;
          });
          if (matches.length !== 1) {
            skipped.push(item.key + " (field is missing or ambiguous; scan again)");
            continue;
          }
          const el = matches[0];
          if (el.disabled) { skipped.push(item.key + " (field is disabled)"); continue; }
          if (el.tagName.toLowerCase() === "select") {
            const target = item.value.toLowerCase().trim();
            const option = Array.from(el.options).find(o =>
              o.value && (o.text.trim().toLowerCase() === target || o.value.trim().toLowerCase() === target)
            );
            if (!option) { skipped.push(item.key + " (select option not exact)"); continue; }
            el.value = option.value;
          } else {
            const proto = el.tagName === "TEXTAREA" ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
            const setter = Object.getOwnPropertyDescriptor(proto, "value")?.set;
            let value = item.value;
            if (el.type === "date" && /^\d{2}[/-]\d{2}[/-]\d{4}$/.test(value)) {
              const [d,m,y] = value.split(/[/-]/); value = y + "-" + m + "-" + d;
            }
            if (setter) setter.call(el, value); else el.value = value;
          }
          el.dispatchEvent(new Event("input", { bubbles: true }));
          el.dispatchEvent(new Event("change", { bubbles: true }));
          filled++;
        }
        return { filled, skipped };
      }
    });
    $("pageStatus").textContent = result.filled + " field(s) filled. " +
      (result.skipped.length ? "Not filled: " + result.skipped.join(", ") + ". " : "") +
      "Review portal validation messages and every value. Nothing was submitted.";
  } catch (error) {
    $("pageStatus").textContent = error.message || "Could not fill this page.";
  }
});

scan();
