package com.vesanieminen.i18n;

import com.vaadin.flow.component.UI;

import java.util.Locale;

/**
 * Short alias for retrieving a translated string. Intended for use inside view
 * code where {@code UI.getCurrent()} is reliably non-null. For background
 * threads or non-UI contexts, inject {@link EvStatsI18NProvider} directly.
 *
 * <pre>{@code
 * label.setText(T.tr("nav.charging"));
 * notification.setText(T.tr("charging.image.couldNotRead", value));
 * }</pre>
 */
public final class T {

    private T() {
    }

    public static String tr(String key, Object... params) {
        UI ui = UI.getCurrent();
        Locale locale = ui != null ? ui.getLocale() : EvStatsI18NProvider.ENGLISH;
        if (ui == null) {
            // Off-thread or test path; we don't have a way to reach the
            // I18NProvider without Spring context, so return the key.
            return "!" + key + "!";
        }
        return ui.getTranslation(key, locale, params);
    }
}
