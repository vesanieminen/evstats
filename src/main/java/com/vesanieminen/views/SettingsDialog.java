package com.vesanieminen.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vesanieminen.i18n.EvStatsI18NProvider;
import com.vesanieminen.i18n.LocaleService;
import com.vesanieminen.i18n.T;
import com.vesanieminen.services.ObjectMapperService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Slf4j
@SpringComponent
@RouteScope
public class SettingsDialog extends Dialog {

    private final NumberField marginField;
    private final ObjectMapperService mapperService;
    private final Checkbox vatCheckbox;
    public static final String margin = "settings.margin";
    public static final String vat = "settings.vat";
    private static final String THEME_PREFERENCE_KEY = "theme.preference";
    private static final String DENSITY_PREFERENCE_KEY = "density.preference";

    public enum ThemePreference {
        SYSTEM("system"),
        LIGHT("light"),
        DARK("dark");

        private final String storageValue;

        ThemePreference(String storageValue) {
            this.storageValue = storageValue;
        }

        public String storageValue() {
            return storageValue;
        }

        public static ThemePreference fromStorage(String raw) {
            if (raw == null) return SYSTEM;
            return switch (raw) {
                case "light" -> LIGHT;
                case "dark" -> DARK;
                default -> SYSTEM;
            };
        }
    }

    public enum DensityPreference {
        AUTO("auto"),
        COMPACT("compact"),
        COMFORTABLE("comfortable");

        private final String storageValue;

        DensityPreference(String storageValue) {
            this.storageValue = storageValue;
        }

        public String storageValue() {
            return storageValue;
        }

        public static DensityPreference fromStorage(String raw) {
            if (raw == null) return AUTO;
            return switch (raw) {
                case "compact" -> COMPACT;
                case "comfortable" -> COMFORTABLE;
                default -> AUTO;
            };
        }
    }

    public SettingsDialog(SettingsState settingsState, ObjectMapperService mapperService, LocaleService localeService) {
        this.mapperService = mapperService;

        setHeaderTitle(T.tr("settings.title"));
        final var closeButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.setAriaLabel(T.tr("common.close"));
        getHeader().add(closeButton);

        final var generalH3 = new H3(T.tr("settings.general"));
        add(generalH3);

        final Select<ThemePreference> themeSelect = new Select<>();
        themeSelect.setLabel(T.tr("settings.theme.label"));
        themeSelect.setHelperText(T.tr("settings.theme.helper"));
        themeSelect.setId("settings-theme");
        themeSelect.setItems(ThemePreference.SYSTEM, ThemePreference.LIGHT, ThemePreference.DARK);
        themeSelect.setItemLabelGenerator(p -> switch (p) {
            case SYSTEM -> T.tr("settings.theme.system");
            case LIGHT -> T.tr("settings.theme.light");
            case DARK -> T.tr("settings.theme.dark");
        });
        themeSelect.setValue(ThemePreference.SYSTEM);
        themeSelect.setWidthFull();
        WebStorage.getItem(THEME_PREFERENCE_KEY, raw -> themeSelect.setValue(ThemePreference.fromStorage(raw)));
        themeSelect.addValueChangeListener(e -> {
            if (!e.isFromClient() || e.getValue() == null) {
                return;
            }
            ThemePreference picked = e.getValue();
            WebStorage.setItem(THEME_PREFERENCE_KEY, picked.storageValue());
            // Re-evaluate the document theme attribute now. applyTheme() reads
            // localStorage so it picks up the value just persisted above.
            UI.getCurrent().getPage().executeJs("window.applyTheme && window.applyTheme();");
        });
        add(themeSelect);

        final Select<DensityPreference> densitySelect = new Select<>();
        densitySelect.setLabel(T.tr("settings.density.label"));
        densitySelect.setHelperText(T.tr("settings.density.helper"));
        densitySelect.setId("settings-density");
        densitySelect.setItems(DensityPreference.AUTO, DensityPreference.COMPACT, DensityPreference.COMFORTABLE);
        densitySelect.setItemLabelGenerator(p -> switch (p) {
            case AUTO -> T.tr("settings.density.auto");
            case COMPACT -> T.tr("settings.density.compact");
            case COMFORTABLE -> T.tr("settings.density.comfortable");
        });
        densitySelect.setValue(DensityPreference.AUTO);
        densitySelect.setWidthFull();
        WebStorage.getItem(DENSITY_PREFERENCE_KEY, raw -> densitySelect.setValue(DensityPreference.fromStorage(raw)));
        densitySelect.addValueChangeListener(e -> {
            if (!e.isFromClient() || e.getValue() == null) {
                return;
            }
            DensityPreference picked = e.getValue();
            WebStorage.setItem(DENSITY_PREFERENCE_KEY, picked.storageValue());
            UI.getCurrent().getPage().executeJs("window.applyDensity && window.applyDensity();");
        });
        add(densitySelect);

        final Select<Locale> languageSelect = new Select<>();
        languageSelect.setLabel(T.tr("settings.language"));
        languageSelect.setHelperText(T.tr("settings.language.helper"));
        languageSelect.setId("settings-language");
        languageSelect.setItems(List.of(EvStatsI18NProvider.ENGLISH, EvStatsI18NProvider.FINNISH));
        languageSelect.setItemLabelGenerator(locale -> {
            if (EvStatsI18NProvider.FINNISH.getLanguage().equals(locale.getLanguage())) {
                return T.tr("settings.language.fi");
            }
            return T.tr("settings.language.en");
        });
        languageSelect.setValue(EvStatsI18NProvider.resolve(UI.getCurrent().getLocale()));
        languageSelect.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null) {
                close();
                localeService.setLocale(UI.getCurrent(), e.getValue());
            }
        });
        languageSelect.setWidthFull();
        add(languageSelect);

        final var electricityCosts = new H3(T.tr("settings.electricity"));
        add(electricityCosts);

        marginField = new NumberField(T.tr("settings.margin"));
        marginField.setId(margin);
        marginField.setHelperText(T.tr("settings.margin.helper"));
        marginField.setWidthFull();
        add(marginField);

        vatCheckbox = new Checkbox(T.tr("settings.vat"));
        vatCheckbox.setId(vat);
        vatCheckbox.setHelperText(T.tr("settings.vat.helper"));
        add(vatCheckbox);

        final var binder = new Binder<Settings>();
        binder.bind(marginField, Settings::getMargin, Settings::setMargin);
        binder.bind(vatCheckbox, Settings::getVat, Settings::setVat);
        binder.setBean(settingsState.settings);

        readFieldValues();

        marginField.addValueChangeListener(item -> mapperService.saveFieldValue(marginField));
        vatCheckbox.addValueChangeListener(item -> mapperService.saveFieldValue(vatCheckbox));
    }

    public void readFieldValues() {
        WebStorage.getItem(marginField.getId().orElseThrow(), item -> mapperService.readValue(item, marginField));
        WebStorage.getItem(vatCheckbox.getId().orElseThrow(), item -> mapperService.readValue(item, vatCheckbox));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Settings {
        Double margin;
        Boolean vat;
    }

    @VaadinSessionScope
    @Component
    @Getter
    public static class SettingsState {
        Settings settings = new Settings(null, null);
    }

}
