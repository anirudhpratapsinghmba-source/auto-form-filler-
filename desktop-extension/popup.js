const $ = (id) => document.getElementById(id);
let activeTabId = null;

async function getActiveTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id || !/^https?:/.test(tab.url || "")) throw new Error("Open a normal http/https form page first.");
  activeTabId = tab.id;
  return tab;
}

$("scan").addEventListener("click", async () => {
  $("pageStatus").textContent = "Checking visible form fields…";
  $("detected").textContent = "";
  try {
    const tab = await getActiveTab();
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: () => Array.from(document.querySelectorAll("input,textarea,select"))
        .filter(el => !el.disabled && el.type !== "hidden" && el.type !== "password" && el.type !== "file")
        .map(el => ({
          label: (el.labels?.[0]?.innerText || el.getAttribute("aria-label") || el.placeholder || el.name || el.id || el.type || "Unlabelled field").trim(),
          type: el.tagName.toLowerCase() === "select" ? "select" : (el.type || el.tagName.toLowerCase())
        })).slice(0, 80)
    });
    $("pageStatus").textContent = result.length ? result.length + " visible fields detected. Review candidate details below." : "No supported visible fields found on this page.";
    const ul = document.createElement("ul");
    result.slice(0, 12).forEach(item => { const li = document.createElement("li"); li.textContent = item.label + " (" + item.type + ")"; ul.appendChild(li); });
    $("detected").appendChild(ul);
  } catch (error) {
    $("pageStatus").textContent = error.message || "Could not inspect this page.";
  }
});

$("candidate").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("pageStatus").textContent = "Filling only fields that can be matched safely…";
  try {
    const tab = await getActiveTab();
    const values = Object.fromEntries(new FormData(event.currentTarget).entries());
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      args: [values],
      func: (candidate) => {
        const fields = Array.from(document.querySelectorAll("input,textarea"))
          .filter(el => !el.disabled && el.type !== "hidden" && el.type !== "password" && el.type !== "file" && !["submit","button","reset","checkbox","radio"].includes(el.type));
        const labelFor = (el) => [
          el.labels?.[0]?.innerText, el.getAttribute("aria-label"), el.placeholder, el.name, el.id
        ].filter(Boolean).join(" ").toLowerCase();
        const rules = [
          ["fullName", /full.?name|applicant.?name|candidate.?name|student.?name|^name$/],
          ["dob", /date.?of.?birth|\bdob\b|birth.?date/],
          ["fatherName", /father|guardian.?name/],
          ["mobile", /mobile|phone|contact.?number/],
          ["email", /e.?mail/],
          ["address", /address|residential/]
        ];
        let filled = 0;
        const used = new Set();
        for (const [key, pattern] of rules) {
          const value = String(candidate[key] || "").trim();
          if (!value) continue;
          const field = fields.find(el => !used.has(el) && pattern.test(labelFor(el)));
          if (!field) continue;
          const setter = Object.getOwnPropertyDescriptor(
            field.tagName === "TEXTAREA" ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype, "value"
          )?.set;
          if (setter) setter.call(field, value); else field.value = value;
          field.dispatchEvent(new Event("input", { bubbles: true }));
          field.dispatchEvent(new Event("change", { bubbles: true }));
          used.add(field);
          filled++;
        }
        return { filled, total: fields.length };
      }
    });
    $("pageStatus").textContent = result.filled + " field(s) filled. Please verify the page manually; nothing was submitted.";
  } catch (error) {
    $("pageStatus").textContent = error.message || "Could not fill this page.";
  }
});
