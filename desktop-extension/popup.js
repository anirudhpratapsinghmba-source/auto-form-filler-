const $ = (id) => document.getElementById(id);
let scannedTabId = null;
let lastScan = [];

const FIELD_RULES = [
  { key: "fullName", label: "Full name", pattern: /full.?name|applicant.?name|candidate.?name|student.?name|पूरा नाम|आवेदक का नाम|अभ्यर्थी का नाम|^name$/ },
  { key: "dob", label: "Date of birth", pattern: /date.?of.?birth|\bdob\b|birth.?date|जन्म.?तिथि|जन्मतिथि/ },
  { key: "fatherName", label: "Father's name", pattern: /father|पिता|अभिभावक/ },
  { key: "mobile", label: "Mobile", pattern: /mobile|phone|contact.?number|मोबाइल|दूरभाष/ },
  { key: "email", label: "Email", pattern: /e.?mail|ई.?मेल/ },
  { key: "address", label: "Address", pattern: /address|residential|पता|निवास/ }
];

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
            index, label: label || "Unlabelled field", type: el.tagName.toLowerCase() === "select" ? "select" : (el.type || el.tagName.toLowerCase()),
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
  return Object.fromEntries(new FormData($("candidate")).entries());
}

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
    const missingRequired = [];
    const invalid = [];
    for (const item of mapping) {
      const value = String(values[item.rule.key] || "").trim();
      if (!value) {
        if (item.field.required) missingRequired.push(item.field.label);
        continue;
      }
      const problem = validateValue(item.rule, value, item.field);
      if (problem) { invalid.push(item.field.label + ": " + problem); continue; }
      plan.push({ index: item.field.index, key: item.rule.key, value, type: item.field.type, options: item.field.options });
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
          const el = visibleFields[item.index];
          if (!el || el.disabled) { skipped.push(item.key); continue; }
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
