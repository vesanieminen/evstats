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
import org.vaadin.lineawesome.LineAwesomeIcon;

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

    public SettingsDialog(SettingsState settingsState, ObjectMapperService mapperService, LocaleService localeService) {
        this.mapperService = mapperService;

        setHeaderTitle(T.tr("settings.title"));
        final var closeButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.setAriaLabel(T.tr("common.close"));
        getHeader().add(closeButton);

        final var generalH3 = new H3(T.tr("settings.general"));
        add(generalH3);

        Button themeButton = new Button(LineAwesomeIcon.MOON_SOLID.create());
        UI.getCurrent().getPage().executeJs("return document.documentElement.getAttribute('theme');")
                .then(String.class, darkMode -> {
                            if ("dark".equals(darkMode)) {
                                setThemeButtonMode(themeButton, LineAwesomeIcon.SUN_SOLID, T.tr("settings.theme.toLight"));
                            } else {
                                setThemeButtonMode(themeButton, LineAwesomeIcon.MOON_SOLID, T.tr("settings.theme.toDark"));
                            }
                        }
                );
        themeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeButton.addClickListener(e -> {
            final var ui = UI.getCurrent();
            ui.getPage().executeJs("return document.documentElement.getAttribute('theme');")
                    .then(String.class, darkMode -> {
                                if ("dark".equals(darkMode)) {
                                    ui.getPage().executeJs("document.documentElement.setAttribute('theme', '');");
                                    setThemeButtonMode(themeButton, LineAwesomeIcon.MOON_SOLID, T.tr("settings.theme.toDark"));
                                } else {
                                    ui.getPage().executeJs("document.documentElement.setAttribute('theme', 'dark');");
                                    setThemeButtonMode(themeButton, LineAwesomeIcon.SUN_SOLID, T.tr("settings.theme.toLight"));
                                }
                            }
                    );
        });
        add(themeButton);

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

    private void setThemeButtonMode(Button theme, LineAwesomeIcon darkMode, String translation) {
        theme.setIcon(darkMode.create());
        theme.setText(translation);
        theme.setAriaLabel(translation);
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
