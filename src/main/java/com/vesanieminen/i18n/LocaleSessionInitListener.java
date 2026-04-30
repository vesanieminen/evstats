package com.vesanieminen.i18n;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Sets the Vaadin session/UI locale from the {@code settings.locale} cookie at
 * the very beginning of each request, before any view or layout is
 * instantiated. Without this hook, layouts constructed early in the request
 * cycle would render in the browser's {@code Accept-Language} locale and only
 * flip on a second reload.
 */
@Component
public class LocaleSessionInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(e ->
                applyFromCookie(e.getRequest(), e.getSession()::setLocale));
        // Cookies set later (e.g. when the user toggles language and we
        // close+reload the session) need to take effect on the very next
        // request, even though the session has already been initialised.
        event.getSource().addUIInitListener(e -> {
            VaadinRequest req = VaadinRequest.getCurrent();
            applyFromCookie(req, locale -> {
                e.getUI().getSession().setLocale(locale);
                e.getUI().setLocale(locale);
            });
        });
    }

    private static void applyFromCookie(VaadinRequest request, java.util.function.Consumer<Locale> apply) {
        if (request == null) return;
        Locale resolved = readCookie(request);
        if (resolved == null) {
            resolved = EvStatsI18NProvider.resolve(request.getLocale());
        }
        apply.accept(resolved);
    }

    private static Locale readCookie(VaadinRequest request) {
        if (!(request instanceof HttpServletRequest http)) return null;
        Cookie[] cookies = http.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (LocaleService.COOKIE_NAME.equals(cookie.getName())) {
                String tag = cookie.getValue();
                if (tag != null && !tag.isBlank()) {
                    return EvStatsI18NProvider.resolve(Locale.forLanguageTag(tag));
                }
            }
        }
        return null;
    }
}
