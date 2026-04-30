package com.vesanieminen.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Persists user-chosen locale via a cookie. The actual <em>read</em> of the
 * cookie happens in {@link LocaleSessionInitListener} so that session and UI
 * locale are correct from the very first render — see that class for the
 * rationale on not using {@code WebStorage}.
 */
@Component
@VaadinSessionScope
public class LocaleService {

    public static final String COOKIE_NAME = "settings.locale";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365; // 1 year

    /**
     * Persist the chosen locale in a cookie and reload the page. The cookie
     * is sent with the next request, so the post-reload UI is constructed in
     * the new locale from the start.
     */
    public void setLocale(UI ui, Locale locale) {
        Locale resolved = EvStatsI18NProvider.resolve(locale);
        ui.setLocale(resolved);
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setLocale(resolved);
        }
        // Persist the choice in a cookie. Closing the session ensures that
        // @PreserveOnRefresh views (notably ChargingView) don't keep the old
        // English MainLayout instance after reload — without close(), Vaadin
        // would match the next request to the existing UI and skip layout
        // reconstruction.
        ui.getPage().executeJs(
                "document.cookie = $0 + '=' + encodeURIComponent($1) + "
                        + "'; path=/; max-age=' + $2 + '; SameSite=Lax';"
                        + "location.reload();",
                COOKIE_NAME, resolved.toLanguageTag(), COOKIE_MAX_AGE_SECONDS);
        if (session != null) {
            session.close();
        }
    }
}
