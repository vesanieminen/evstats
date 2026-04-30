package com.vesanieminen.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

/**
 * Bridges Vaadin's {@link I18NProvider} to Spring's {@link MessageSource}, so
 * any {@code messages_<lang>.properties} on the classpath is picked up
 * automatically. Adding a new locale is purely a matter of dropping in another
 * properties file and listing it in {@link #SUPPORTED}.
 */
@Component
public class EvStatsI18NProvider implements I18NProvider {

    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale FINNISH = Locale.of("fi");

    private static final List<Locale> SUPPORTED = List.of(ENGLISH, FINNISH);

    private final MessageSource messageSource;

    public EvStatsI18NProvider(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return SUPPORTED;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        Locale resolved = resolve(locale);
        try {
            String pattern = messageSource.getMessage(key, null, resolved);
            if (params == null || params.length == 0) {
                return pattern;
            }
            // Spring's MessageFormat path requires {0}-style; we use printf-style
            // (%s) in some entries, so format both.
            if (pattern.contains("{0}")) {
                return new MessageFormat(pattern, resolved).format(params);
            }
            return String.format(resolved, pattern, params);
        } catch (NoSuchMessageException e) {
            return "!" + key + "!";
        }
    }

    /**
     * Maps an arbitrary requested locale to the closest supported locale.
     * Falls back to English for anything we don't have a bundle for.
     */
    public static Locale resolve(Locale requested) {
        if (requested == null) return ENGLISH;
        for (Locale supported : SUPPORTED) {
            if (supported.getLanguage().equals(requested.getLanguage())) {
                return supported;
            }
        }
        return ENGLISH;
    }
}
