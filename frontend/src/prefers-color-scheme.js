// Theme preference: "system" | "light" | "dark". When the user picks Light or
// Dark explicitly via the settings dialog, it overrides the OS preference and
// is persisted in localStorage. "system" (or absent) follows the OS and updates
// live when the OS preference changes.
const PREFERENCE_KEY = "theme.preference";

function readPreference() {
    try {
        const raw = localStorage.getItem(PREFERENCE_KEY);
        return raw === "light" || raw === "dark" ? raw : "system";
    } catch (e) {
        return "system";
    }
}

function effectiveDark(preference) {
    if (preference === "dark") return true;
    if (preference === "light") return false;
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

window.applyTheme = () => {
    const dark = effectiveDark(readPreference());
    document.documentElement.setAttribute("theme", dark ? "dark" : "");
};

window
    .matchMedia("(prefers-color-scheme: dark)")
    .addEventListener("change", window.applyTheme);

window.applyTheme();
