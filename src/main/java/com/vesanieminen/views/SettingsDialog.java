package com.vesanieminen.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vesanieminen.services.ObjectMapperService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.vaadin.lineawesome.LineAwesomeIcon;

@Slf4j
@SpringComponent
@RouteScope
public class SettingsDialog extends Dialog {

    private final NumberField marginField;
    private final ObjectMapperService mapperService;
    private final Checkbox vatCheckbox;
    public static final String margin = "settings.margin";
    public static final String vat = "settings.vat";

    public SettingsDialog(SettingsState settingsState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        setHeaderTitle("Settings");
        final var closeButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        final var generalH3 = new H3("General");
        add(generalH3);

        Button themeButton = new Button(LineAwesomeIcon.MOON_SOLID.create());
        UI.getCurrent().getPage().executeJs("return document.documentElement.getAttribute('theme');")
                .then(String.class, darkMode -> {
                            if ("dark".equals(darkMode)) {
                                setThemeButtonMode(themeButton, LineAwesomeIcon.SUN_SOLID, "Switch to light mode");
                                // Initialize MutationObserver for dark theme overlay propagation
                                UI.getCurrent().getPage().executeJs("""
                                    if (!window._themeObserver) {
                                        window._themeObserver = new MutationObserver((mutations) => {
                                            const theme = document.documentElement.getAttribute('theme');
                                            if (theme === 'dark') {
                                                document.querySelectorAll('vaadin-combo-box-overlay, vaadin-time-picker-overlay, vaadin-date-picker-overlay').forEach(el => {
                                                    if (!el.hasAttribute('theme')) el.setAttribute('theme', 'dark');
                                                });
                                            }
                                        });
                                        window._themeObserver.observe(document.body, { childList: true, subtree: true });
                                    }
                                """);
                            } else {
                                setThemeButtonMode(themeButton, LineAwesomeIcon.MOON_SOLID, "Switch to dark mode");
                            }
                        }
                );
        themeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeButton.addClickListener(e -> {
            final var ui = UI.getCurrent();
            ui.getPage().executeJs("return document.documentElement.getAttribute('theme');")
                    .then(String.class, darkMode -> {
                                if ("dark".equals(darkMode)) {
                                    ui.getPage().executeJs("""
                                        document.documentElement.setAttribute('theme', '');
                                        // Propagate theme to all overlay elements
                                        document.querySelectorAll('vaadin-combo-box-overlay, vaadin-time-picker-overlay, vaadin-date-picker-overlay').forEach(el => el.removeAttribute('theme'));
                                    """);
                                    setThemeButtonMode(themeButton, LineAwesomeIcon.MOON_SOLID, "Switch to dark mode");
                                } else {
                                    ui.getPage().executeJs("""
                                        document.documentElement.setAttribute('theme', 'dark');
                                        // Propagate theme to all overlay elements
                                        document.querySelectorAll('vaadin-combo-box-overlay, vaadin-time-picker-overlay, vaadin-date-picker-overlay').forEach(el => el.setAttribute('theme', 'dark'));
                                        // Set up MutationObserver to propagate theme to new overlays
                                        if (!window._themeObserver) {
                                            window._themeObserver = new MutationObserver((mutations) => {
                                                const theme = document.documentElement.getAttribute('theme');
                                                if (theme === 'dark') {
                                                    document.querySelectorAll('vaadin-combo-box-overlay, vaadin-time-picker-overlay, vaadin-date-picker-overlay').forEach(el => {
                                                        if (!el.hasAttribute('theme')) el.setAttribute('theme', 'dark');
                                                    });
                                                }
                                            });
                                            window._themeObserver.observe(document.body, { childList: true, subtree: true });
                                        }
                                    """);
                                    setThemeButtonMode(themeButton, LineAwesomeIcon.SUN_SOLID, "Switch to light mode");
                                }
                            }
                    );
        });
        add(themeButton);

        final var electricityCosts = new H3("Electricity costs");
        add(electricityCosts);

        marginField = new NumberField("Margin");
        marginField.setId(margin);
        marginField.setHelperText("Spot electricity contract margin e.g. 0.45 c/kWh");
        marginField.setWidthFull();
        add(marginField);

        vatCheckbox = new Checkbox("VAT");
        vatCheckbox.setId(vat);
        vatCheckbox.setHelperText("Calculate VAT in the costs?");
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
