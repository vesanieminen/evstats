package com.vesanieminen.views;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.services.ObjectMapperService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@Slf4j
public class SettingsView extends Main {

    private final NumberField marginField;
    private final ObjectMapperService mapperService;
    private final Checkbox vatCheckbox;
    public static final String margin = "settings.margin";
    public static final String vat = "settings.vat";

    public SettingsView(SettingsState settingsState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );

        final var electricityCosts = new H3("Electricity costs");
        add(electricityCosts);

        marginField = new NumberField("Margin");
        marginField.setId(margin);
        marginField.setHelperText("Spot electricity contract margin e.g. 0.45 c/kWh");
        marginField.addClassNames(LumoUtility.MaxWidth.SCREEN_SMALL);
        add(marginField);

        vatCheckbox = new Checkbox("VAT");
        vatCheckbox.setId(vat);
        vatCheckbox.setHelperText("Calculate VAT in the costs?");
        add(vatCheckbox);

        final var binder = new Binder<Settings>();
        binder.bind(marginField, Settings::getMargin, Settings::setMargin);
        binder.bind(vatCheckbox, Settings::isVat, Settings::setVat);
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
        boolean vat;
    }

    @VaadinSessionScope
    @Component
    @Getter
    public static class SettingsState {
        Settings settings = new Settings(null, true);
    }


}
