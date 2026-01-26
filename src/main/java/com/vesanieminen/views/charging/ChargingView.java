package com.vesanieminen.views.charging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.Card;
import com.vesanieminen.components.DualRangeSlider;
import com.vesanieminen.components.Ping;
import com.vesanieminen.model.EVModel;
import com.vesanieminen.services.LiukuriService;
import com.vesanieminen.services.ObjectMapperService;
import com.vesanieminen.views.MainLayout;
import com.vesanieminen.views.SettingsDialog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;

@PageTitle("Charging tool")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "lataus", layout = MainLayout.class)
@RouteAlias(value = "charging", layout = MainLayout.class)
@Slf4j
@PreserveOnRefresh
public class ChargingView extends Main {

    // Vehicle section fields
    private final Select<EVModel> vehicleSelect;
    private final NumberField batteryCapacityField;
    private final NumberField consumptionField;
    private final Span currentSocValueSpan;
    private final Span currentRangeSpan;
    private final Span rangeAddedSpan;
    private final Span targetSocValueSpan;
    private final Span targetRangeSpan;
    private EVModel selectedModel = EVModel.PRESETS.get(0);

    // Charge level fields
    private final DualRangeSlider socSlider;
    private final Span addingKwhSpan;
    private final Span batteryCapacitySpan;

    // Charging speed fields
    private final Span powerValueSpan;
    private final Span amperesValueSpan;
    private final IntegerField phasesField;
    private final IntegerField voltageField;
    private final NumberField chargingLossField;
    private int currentAmps = 16;

    // Schedule fields
    private final DatePicker startDatePicker;
    private final TimePicker startTimePicker;
    private final DatePicker endDatePicker;
    private final TimePicker endTimePicker;
    private final Button calcEndButton;
    private final Button calcStartButton;

    // Summary fields
    private final Span durationValueSpan;
    private final Span energyConsumedValueSpan;
    private final Span addedToBatteryValueSpan;
    private final Span lostToHeatValueSpan;
    private final Span spotPriceValueSpan;
    private final Span totalCostValueSpan;
    private final Ping spotAveragePing;
    private final Ping electricityCostPing;

    // Services and state
    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private final LiukuriService liukuriService;
    private final ObjectMapperService mapperService;
    private final SettingsDialog.SettingsState settingsState;
    private final Binder<Charge> chargeBinder;
    private final PreservedState preservedState;

    // Vehicle selection expandable section
    private Div vehicleSelectionDiv;
    private boolean vehicleSelectionVisible = false;

    public ChargingView(PreservedState preservedState, LiukuriService liukuriService, ObjectMapperService mapperService, SettingsDialog.SettingsState settingsState) {
        this.preservedState = preservedState;
        this.liukuriService = liukuriService;
        this.mapperService = mapperService;
        this.settingsState = settingsState;

        loadSettingsFromStorage();

        addClassName("charging-view-container");

        // ===== VEHICLE & STATUS CARD =====
        Card vehicleCard = new Card();
        vehicleCard.addClassName("vehicle-card");

        // Vehicle visualization section
        Div vehicleSection = new Div();
        vehicleSection.addClassName("vehicle-section");

        // SVG Car
        Html carSvg = createCarSvg();
        vehicleSection.add(carSvg);

        // Vehicle name
        Span vehicleName = new Span(selectedModel.name());
        vehicleName.addClassName("vehicle-name");
        vehicleSection.add(vehicleName);

        // SOC Display (Current | Range + | Target)
        Div socDisplay = new Div();
        socDisplay.addClassName("soc-display");

        // Current column
        Div currentColumn = new Div();
        currentColumn.addClassName("soc-column");
        Span currentLabel = new Span("Current");
        currentLabel.addClassName("soc-label");
        currentSocValueSpan = new Span("20%");
        currentSocValueSpan.addClassName("soc-value");
        currentRangeSpan = new Span("103 km");
        currentRangeSpan.addClassName("soc-range");
        currentColumn.add(currentLabel, currentSocValueSpan, currentRangeSpan);

        // Range added column
        Div rangeColumn = new Div();
        rangeColumn.addClassName("soc-column");
        Span rangeLabel = new Span("Range +");
        rangeLabel.addClassName("soc-label");
        rangeAddedSpan = new Span("155 km");
        rangeAddedSpan.addClassNames("soc-value", "accent");
        Span addedLabel = new Span("added");
        addedLabel.addClassName("soc-range");
        rangeColumn.add(rangeLabel, rangeAddedSpan, addedLabel);

        // Target column
        Div targetColumn = new Div();
        targetColumn.addClassName("soc-column");
        Span targetLabel = new Span("Target");
        targetLabel.addClassName("soc-label");
        targetSocValueSpan = new Span("50%");
        targetSocValueSpan.addClassName("soc-value");
        targetRangeSpan = new Span("258 km");
        targetRangeSpan.addClassName("soc-range");
        targetColumn.add(targetLabel, targetSocValueSpan, targetRangeSpan);

        socDisplay.add(currentColumn, rangeColumn, targetColumn);
        vehicleSection.add(socDisplay);

        // Change Vehicle toggle
        Button changeVehicleBtn = new Button("Change Vehicle");
        changeVehicleBtn.addClassName("change-vehicle-toggle");
        changeVehicleBtn.setIcon(new Icon(VaadinIcon.CAR));
        Icon chevron = new Icon(VaadinIcon.CHEVRON_DOWN);
        changeVehicleBtn.setIconAfterText(true);
        changeVehicleBtn.addClickListener(e -> {
            vehicleSelectionVisible = !vehicleSelectionVisible;
            vehicleSelectionDiv.setVisible(vehicleSelectionVisible);
            chevron.getElement().setAttribute("icon", vehicleSelectionVisible ? "vaadin:chevron-up" : "vaadin:chevron-down");
        });

        // Vehicle selection section (hidden by default)
        vehicleSelectionDiv = new Div();
        vehicleSelectionDiv.addClassName("vehicle-selection");
        vehicleSelectionDiv.setVisible(false);

        // Custom vehicle fields - initialize before vehicleSelect listener
        Div customFieldsDiv = new Div();
        customFieldsDiv.addClassName("vehicle-custom-fields");

        batteryCapacityField = new NumberField("Battery Capacity (kWh)");
        batteryCapacityField.setId("batteryCapacityField");
        batteryCapacityField.setValue((double) selectedModel.capacity());
        batteryCapacityField.setStepButtonsVisible(true);
        batteryCapacityField.setVisible(false);

        consumptionField = new NumberField("Consumption (kWh/100km)");
        consumptionField.setId("consumptionField");
        consumptionField.setValue(selectedModel.efficiency());
        consumptionField.setStepButtonsVisible(true);
        consumptionField.setStep(0.1);
        consumptionField.setVisible(false);

        vehicleSelect = new Select<>();
        vehicleSelect.setId("vehicleSelect");
        vehicleSelect.setItems(EVModel.PRESETS);
        vehicleSelect.setItemLabelGenerator(EVModel::getDisplayName);
        vehicleSelect.setValue(selectedModel);
        vehicleSelect.setWidthFull();
        vehicleSelect.addValueChangeListener(e -> {
            selectedModel = e.getValue();
            vehicleName.setText(selectedModel.name());
            if (selectedModel.equals(EVModel.CUSTOM)) {
                batteryCapacityField.setVisible(true);
                consumptionField.setVisible(true);
            } else {
                batteryCapacityField.setVisible(false);
                consumptionField.setVisible(false);
                batteryCapacityField.setValue((double) selectedModel.capacity());
                consumptionField.setValue(selectedModel.efficiency());
            }
            doCalculation();
        });

        customFieldsDiv.add(batteryCapacityField, consumptionField);
        vehicleSelectionDiv.add(vehicleSelect, customFieldsDiv);

        vehicleCard.add(vehicleSection, changeVehicleBtn, vehicleSelectionDiv);
        add(vehicleCard);

        // ===== CHARGE LEVEL CARD =====
        Card chargeLevelCard = new Card(new Icon(VaadinIcon.PLUG), "Charge Level");

        socSlider = new DualRangeSlider(0, 100, preservedState.charge.getCurrentSOC(), preservedState.charge.getTargetSOC());
        socSlider.setWidthFull();
        socSlider.addValueChangeListener(e -> doCalculation());

        Div chargeLevelSummary = new Div();
        chargeLevelSummary.addClassName("charge-level-summary");
        addingKwhSpan = new Span();
        batteryCapacitySpan = new Span();
        batteryCapacitySpan.addClassName(LumoUtility.TextColor.SECONDARY);
        chargeLevelSummary.add(addingKwhSpan, batteryCapacitySpan);

        chargeLevelCard.add(socSlider, chargeLevelSummary);
        add(chargeLevelCard);

        // ===== CHARGING SPEED CARD =====
        Card chargingSpeedCard = new Card();

        Div speedHeader = new Div();
        speedHeader.addClassName("charging-speed-header");

        Div headerLeft = new Div();
        headerLeft.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
        Icon zapIcon = new Icon(VaadinIcon.FLASH);
        zapIcon.addClassName(LumoUtility.TextColor.PRIMARY);
        zapIcon.setSize("18px");
        Span speedLabel = new Span("Charging Speed");
        speedLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        speedLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
        headerLeft.add(zapIcon, speedLabel);

        powerValueSpan = new Span("11.0 kW");
        powerValueSpan.addClassName("power-value");

        speedHeader.add(headerLeft, powerValueSpan);

        // Amperage slider (native HTML input)
        Html amperesSlider = new Html("<input type='range' min='1' max='32' value='16' class='single-slider' id='amperesSlider'/>");
        amperesSlider.getElement().addEventListener("input", e -> {
            // Value will be updated via JS
        }).addEventData("event.target.value");

        amperesSlider.getElement().executeJs(
                "this.addEventListener('input', function(e) { " +
                        "  $0.$server.updateAmperes(parseInt(e.target.value)); " +
                        "});", getElement());

        amperesValueSpan = new Span("16 A");
        amperesValueSpan.addClassName("amperage-value");

        // Advanced section
        Details advancedDetails = new Details("Advanced");
        advancedDetails.addClassName("advanced-toggle");

        Div advancedSection = new Div();
        advancedSection.addClassName("advanced-section");

        phasesField = new IntegerField("Phases");
        phasesField.setId("phasesField");
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setValue(preservedState.charge.getPhases());
        phasesField.setStepButtonsVisible(true);

        voltageField = new IntegerField("Voltage");
        voltageField.setId("voltageField");
        voltageField.setSuffixComponent(new Span("V"));
        voltageField.setMin(1);
        voltageField.setMax(1000);
        voltageField.setValue(preservedState.charge.getVoltage());
        voltageField.setStepButtonsVisible(true);

        chargingLossField = new NumberField("Charging Loss");
        chargingLossField.setId("chargingLossField");
        chargingLossField.setSuffixComponent(new Span("%"));
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        chargingLossField.setValue(preservedState.charge.getChargingLoss());
        chargingLossField.setStepButtonsVisible(true);
        chargingLossField.addClassName("full-width");

        advancedSection.add(phasesField, voltageField, chargingLossField);
        advancedDetails.add(advancedSection);

        chargingSpeedCard.add(speedHeader, amperesSlider, amperesValueSpan, advancedDetails);
        add(chargingSpeedCard);

        // ===== SCHEDULE CARD =====
        Card scheduleCard = new Card(new Icon(VaadinIcon.CALENDAR), "Schedule");

        Div scheduleGrid = new Div();
        scheduleGrid.addClassName("schedule-grid");

        final var datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setFirstDayOfWeek(1);
        datePickerI18n.setDateFormat("dd.MM.yyyy");

        startDatePicker = new DatePicker("Start Date");
        startDatePicker.setId("startDatePicker");
        startDatePicker.setI18n(datePickerI18n);
        startDatePicker.setLocale(Locale.of("fi", "FI"));
        startDatePicker.setValue(preservedState.charge.getStartTime().toLocalDate());

        endDatePicker = new DatePicker("End Date");
        endDatePicker.setId("endDatePicker");
        endDatePicker.setI18n(datePickerI18n);
        endDatePicker.setLocale(Locale.of("fi", "FI"));
        endDatePicker.setValue(preservedState.charge.getStartTime().toLocalDate());

        startTimePicker = new TimePicker("Start Time");
        startTimePicker.setId("startTimePicker");
        startTimePicker.setStep(Duration.ofMinutes(15));
        startTimePicker.setLocale(Locale.of("fi", "FI"));
        startTimePicker.setValue(preservedState.charge.getStartTime().toLocalTime());

        endTimePicker = new TimePicker("End Time");
        endTimePicker.setId("endTimePicker");
        endTimePicker.setStep(Duration.ofMinutes(15));
        endTimePicker.setLocale(Locale.of("fi", "FI"));
        endTimePicker.setReadOnly(true);

        scheduleGrid.add(startDatePicker, endDatePicker, startTimePicker, endTimePicker);

        // Calculate mode buttons
        Div calcModeButtons = new Div();
        calcModeButtons.addClassName("calc-mode-buttons");

        calcEndButton = new Button("Calculate End");
        calcEndButton.addClassNames("calc-mode-btn", "active");
        calcEndButton.addClickListener(e -> setCalculationMode(CalculationTarget.CHARGING_END));

        calcStartButton = new Button("Calculate Start");
        calcStartButton.addClassNames("calc-mode-btn", "inactive");
        calcStartButton.addClickListener(e -> setCalculationMode(CalculationTarget.CHARGING_START));

        calcModeButtons.add(calcEndButton, calcStartButton);

        scheduleCard.add(scheduleGrid, calcModeButtons);
        add(scheduleCard);

        // ===== CHARGING SUMMARY CARD =====
        Card summaryCard = new Card(new Icon(VaadinIcon.EURO), "Charging Summary");

        Div summaryRows = new Div();
        summaryRows.addClassName("summary-rows");

        // Duration row
        Div durationRow = new Div();
        durationRow.addClassName("summary-row");
        Span durationLabelSpan = new Span();
        durationLabelSpan.addClassName("label");
        Icon clockIcon = new Icon(VaadinIcon.CLOCK);
        clockIcon.setSize("14px");
        durationLabelSpan.add(clockIcon, new Span("Duration"));
        durationValueSpan = new Span();
        durationValueSpan.addClassName("value");
        durationRow.add(durationLabelSpan, durationValueSpan);
        summaryRows.add(durationRow);

        // Energy consumed row
        Div energyConsumedRow = new Div();
        energyConsumedRow.addClassName("summary-row");
        Span energyConsumedLabel = new Span("Energy consumed");
        energyConsumedLabel.addClassName("label");
        energyConsumedValueSpan = new Span();
        energyConsumedValueSpan.addClassName("value");
        energyConsumedRow.add(energyConsumedLabel, energyConsumedValueSpan);
        summaryRows.add(energyConsumedRow);

        // Added to battery row
        Div addedToBatteryRow = new Div();
        addedToBatteryRow.addClassName("summary-row");
        Span addedToBatteryLabel = new Span("Added to battery");
        addedToBatteryLabel.addClassName("label");
        addedToBatteryValueSpan = new Span();
        addedToBatteryValueSpan.addClassName("value");
        addedToBatteryRow.add(addedToBatteryLabel, addedToBatteryValueSpan);
        summaryRows.add(addedToBatteryRow);

        // Lost to heat row
        Div lostToHeatRow = new Div();
        lostToHeatRow.addClassName("summary-row");
        Span lostToHeatLabel = new Span("Lost to heat");
        lostToHeatLabel.addClassName("label");
        lostToHeatValueSpan = new Span();
        lostToHeatValueSpan.addClassNames("value", "warning");
        lostToHeatRow.add(lostToHeatLabel, lostToHeatValueSpan);
        summaryRows.add(lostToHeatRow);

        // Spot price row
        Div spotPriceRow = new Div();
        spotPriceRow.addClassName("summary-row");
        Span spotPriceLabel = new Span("Spot price");
        spotPriceLabel.addClassName("label");
        spotPriceValueSpan = new Span();
        spotPriceValueSpan.addClassName("value");
        spotAveragePing = new Ping("Price");
        Div spotValueDiv = new Div(spotPriceValueSpan, spotAveragePing);
        spotValueDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.AlignItems.CENTER);
        spotPriceRow.add(spotPriceLabel, spotValueDiv);
        summaryRows.add(spotPriceRow);

        // Total cost row
        Div totalCostRow = new Div();
        totalCostRow.addClassNames("summary-row", "total-cost-row");
        Span totalCostLabel = new Span("Total Cost");
        totalCostLabel.addClassName("label");
        totalCostValueSpan = new Span("0.00 €");
        totalCostValueSpan.addClassName("value");
        electricityCostPing = new Ping("Cost");
        Div totalValueDiv = new Div(totalCostValueSpan, electricityCostPing);
        totalValueDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.AlignItems.CENTER);
        totalCostRow.add(totalCostLabel, totalValueDiv);
        summaryRows.add(totalCostRow);

        summaryCard.add(summaryRows);
        add(summaryCard);

        // ===== BINDER SETUP =====
        chargeBinder = new Binder<>(Charge.class);
        chargeBinder.bind(batteryCapacityField, Charge::getCapacity, Charge::setCapacity);
        chargeBinder.bind(phasesField, Charge::getPhases, Charge::setPhases);
        chargeBinder.bind(voltageField, Charge::getVoltage, Charge::setVoltage);
        chargeBinder.bind(chargingLossField, Charge::getChargingLoss, Charge::setChargingLoss);

        chargeBinder.setBean(preservedState.charge);

        // Add change listeners
        chargeBinder.addValueChangeListener(e -> doCalculation());

        batteryCapacityField.addValueChangeListener(e -> {
            mapperService.saveFieldValue(batteryCapacityField);
            doCalculation();
        });
        consumptionField.addValueChangeListener(e -> doCalculation());
        phasesField.addValueChangeListener(e -> {
            mapperService.saveFieldValue(phasesField);
            doCalculation();
        });
        voltageField.addValueChangeListener(e -> {
            mapperService.saveFieldValue(voltageField);
            doCalculation();
        });
        chargingLossField.addValueChangeListener(e -> {
            mapperService.saveFieldValue(chargingLossField);
            doCalculation();
        });
        startDatePicker.addValueChangeListener(e -> doCalculation());
        startTimePicker.addValueChangeListener(e -> doCalculation());
        endDatePicker.addValueChangeListener(e -> doCalculation());
        endTimePicker.addValueChangeListener(e -> {
            if (!endTimePicker.isReadOnly()) {
                doCalculation();
            }
        });

        // Set valid calculation range
        final var calculationRange = liukuriService.getValidCalculationRange();
        final var start = Instant.ofEpochMilli(calculationRange.getStart());
        final var end = Instant.ofEpochMilli(calculationRange.getEnd());
        startDatePicker.setMin(start.atZone(fiZoneID).toLocalDate());
        startDatePicker.setMax(end.atZone(fiZoneID).toLocalDate());
        startTimePicker.setMin(LocalTime.of(0, 0));
        startTimePicker.setMax(LocalTime.of(23, 45));

        readFieldValues();
        doCalculation();
    }

    @com.vaadin.flow.component.ClientCallable
    public void updateAmperes(int value) {
        currentAmps = value;
        preservedState.charge.setAmperes(value);
        amperesValueSpan.setText(value + " A");
        doCalculation();
    }

    private void setCalculationMode(CalculationTarget mode) {
        preservedState.charge.setCalculationTarget(mode);
        if (mode == CalculationTarget.CHARGING_END) {
            calcEndButton.removeClassName("inactive");
            calcEndButton.addClassName("active");
            calcStartButton.removeClassName("active");
            calcStartButton.addClassName("inactive");
            startTimePicker.setReadOnly(false);
            endTimePicker.setReadOnly(true);
        } else {
            calcStartButton.removeClassName("inactive");
            calcStartButton.addClassName("active");
            calcEndButton.removeClassName("active");
            calcEndButton.addClassName("inactive");
            startTimePicker.setReadOnly(true);
            endTimePicker.setReadOnly(false);
        }
        doCalculation();
    }

    private Html createCarSvg() {
        String svg = """
                <svg viewBox="0 0 200 80" class="vehicle-svg" style="width: 180px; height: 80px;">
                    <defs>
                        <linearGradient id="carGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                            <stop offset="0%" style="stop-color: var(--lumo-contrast-60pct)"/>
                            <stop offset="100%" style="stop-color: var(--lumo-contrast-70pct)"/>
                        </linearGradient>
                        <linearGradient id="windowGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                            <stop offset="0%" style="stop-color: var(--lumo-primary-color-50pct)"/>
                            <stop offset="100%" style="stop-color: var(--lumo-primary-color)"/>
                        </linearGradient>
                    </defs>
                    <ellipse cx="45" cy="65" rx="18" ry="18" fill="var(--lumo-contrast-90pct)"/>
                    <ellipse cx="45" cy="65" rx="12" ry="12" fill="var(--lumo-contrast-50pct)"/>
                    <ellipse cx="155" cy="65" rx="18" ry="18" fill="var(--lumo-contrast-90pct)"/>
                    <ellipse cx="155" cy="65" rx="12" ry="12" fill="var(--lumo-contrast-50pct)"/>
                    <path d="M20 50 Q25 25 60 20 L140 20 Q175 25 180 50 L180 55 Q180 60 175 60 L25 60 Q20 60 20 55 Z" fill="url(#carGrad)"/>
                    <path d="M55 22 Q60 12 80 10 L120 10 Q140 12 145 22 L140 20 L60 20 Z" fill="url(#windowGrad)" opacity="0.9"/>
                    <rect x="5" y="48" width="15" height="6" rx="2" fill="#fbbf24" opacity="0.8"/>
                    <rect x="180" y="48" width="15" height="6" rx="2" fill="#ef4444" opacity="0.8"/>
                </svg>
                """;
        return new Html(svg);
    }

    private void loadSettingsFromStorage() {
        final var objectMapper = new ObjectMapper();
        WebStorage.getItem(SettingsDialog.margin, item -> {
            if (item == null) return;
            try {
                final var margin = objectMapper.readValue(item, new TypeReference<Double>() {});
                settingsState.getSettings().setMargin(margin);
            } catch (IOException e) {
                log.info("Could not read value: %s".formatted(e.toString()));
            }
        });
        WebStorage.getItem(SettingsDialog.vat, item -> {
            if (item == null) return;
            try {
                final var vat = objectMapper.readValue(item, new TypeReference<Boolean>() {});
                settingsState.getSettings().setVat(vat);
            } catch (IOException e) {
                log.info("Could not read value: %s".formatted(e.toString()));
            }
        });
    }

    private void doCalculation() {
        // Get values from slider and fields
        double currentSoc = socSlider.getLowValue();
        double targetSoc = socSlider.getHighValue();
        double capacity = batteryCapacityField.getValue() != null ? batteryCapacityField.getValue() : 75;
        double consumption = consumptionField.getValue() != null ? consumptionField.getValue() : 17;
        int amperes = currentAmps;
        int phases = phasesField.getValue() != null ? phasesField.getValue() : 3;
        int voltage = voltageField.getValue() != null ? voltageField.getValue() : 230;
        double chargingLoss = chargingLossField.getValue() != null ? chargingLossField.getValue() : 10;

        // Update preserved state
        preservedState.charge.setCurrentSOC(currentSoc);
        preservedState.charge.setTargetSOC(targetSoc);
        preservedState.charge.setCapacity(capacity);

        // Calculate ranges
        int currentRange = (int) Math.round((capacity * currentSoc / 100.0) / consumption * 100);
        int targetRange = (int) Math.round((capacity * targetSoc / 100.0) / consumption * 100);
        int rangeAdded = targetRange - currentRange;

        // Update vehicle display
        currentSocValueSpan.setText(String.format("%.0f%%", currentSoc));
        currentRangeSpan.setText(currentRange + " km");
        targetSocValueSpan.setText(String.format("%.0f%%", targetSoc));
        targetRangeSpan.setText(targetRange + " km");
        rangeAddedSpan.setText(rangeAdded + " km");

        // Calculate energy
        double socIncrease = targetSoc - currentSoc;
        double capacityIncrease = capacity / 100 * socIncrease;

        Span boldKwh = new Span(String.format("%.1f kWh", capacityIncrease));
        boldKwh.getStyle().set("font-weight", "600");
        addingKwhSpan.removeAll();
        addingKwhSpan.add(new Span("Adding: "), boldKwh);
        batteryCapacitySpan.setText(String.format("%.0f kWh battery", capacity));

        // Calculate charging power
        double chargingPowerInWatts = amperes * phases * voltage;
        double chargingPowerKw = chargingPowerInWatts / 1000.0;
        powerValueSpan.setText(String.format("%.1f kW", chargingPowerKw));

        // Calculate time
        double chargingSpeedMinusLoss = chargingPowerInWatts * ((100 - chargingLoss) / 100);
        double chargingTimeHours = capacityIncrease * 1000 / chargingSpeedMinusLoss;
        int chargingTimeSeconds = (int) (chargingTimeHours * 3600);
        int hours = (int) chargingTimeHours;
        int minutes = (chargingTimeSeconds % 3600) / 60;

        // Update schedule
        LocalDate startDate = startDatePicker.getValue();
        LocalTime startTime = startTimePicker.getValue();
        CalculationTarget calculationMode = preservedState.charge.getCalculationTarget();

        Instant chargingStartTime;
        if (calculationMode == CalculationTarget.CHARGING_END) {
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            LocalDateTime endDateTime = startDateTime.plusSeconds(chargingTimeSeconds);
            endDatePicker.setValue(endDateTime.toLocalDate());
            endTimePicker.setValue(endDateTime.toLocalTime());
            preservedState.charge.setStartTime(startDateTime);
            chargingStartTime = ZonedDateTime.of(startDateTime, fiZoneID).toInstant();
        } else {
            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = endTimePicker.getValue();
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
            LocalDateTime startDateTime = endDateTime.minusSeconds(chargingTimeSeconds);
            startDatePicker.setValue(startDateTime.toLocalDate());
            startTimePicker.setValue(startDateTime.toLocalTime());
            preservedState.charge.setStartTime(startDateTime);
            chargingStartTime = ZonedDateTime.of(startDateTime, fiZoneID).toInstant();
        }

        // Calculate energy consumed and lost
        double electricityConsumed = chargingPowerKw * chargingTimeHours;
        double lostPercentage = chargingLoss / 100.0;
        double lostElectricity = electricityConsumed * lostPercentage;
        double addedElectricity = electricityConsumed - lostElectricity;

        // Update summary
        durationValueSpan.setText(String.format("%dh %dmin", hours, minutes));
        energyConsumedValueSpan.setText(String.format("%.2f kWh", electricityConsumed));
        addedToBatteryValueSpan.setText(String.format("%.2f kWh", addedElectricity));
        lostToHeatValueSpan.setText(String.format("%.2f kWh", lostElectricity));

        // Get spot price from API
        final var longDoubleLinkedHashMap = mapChargingEventToConsumptionData(chargingPowerKw, chargingStartTime, chargingTimeHours);
        final var margin = settingsState.getSettings().getMargin();
        final var vat = settingsState.getSettings().getVat();
        final var calculationResponse = liukuriService.performCalculation(longDoubleLinkedHashMap, margin == null ? 0 : margin, vat != null && vat);

        if (calculationResponse != null) {
            final var averagePrice = calculationResponse.getAveragePrice();
            spotPriceValueSpan.setText(String.format("%.2f c/kWh", averagePrice));
            totalCostValueSpan.setText(String.format("%.2f €", calculationResponse.getTotalCost()));

            if (averagePrice >= 10) {
                totalCostValueSpan.setClassName("value");
                totalCostValueSpan.addClassName(LumoUtility.TextColor.ERROR);
                spotPriceValueSpan.setClassName("value");
                spotPriceValueSpan.addClassName(LumoUtility.TextColor.ERROR);
                electricityCostPing.setType(Ping.Type.HIGH);
                spotAveragePing.setType(Ping.Type.HIGH);
            } else if (averagePrice >= 5) {
                totalCostValueSpan.setClassName("value");
                totalCostValueSpan.addClassName(LumoUtility.TextColor.PRIMARY);
                spotPriceValueSpan.setClassName("value");
                spotPriceValueSpan.addClassName(LumoUtility.TextColor.PRIMARY);
                electricityCostPing.setType(Ping.Type.NORMAL);
                spotAveragePing.setType(Ping.Type.NORMAL);
            } else {
                totalCostValueSpan.setClassName("value");
                totalCostValueSpan.addClassName(LumoUtility.TextColor.SUCCESS);
                spotPriceValueSpan.setClassName("value");
                spotPriceValueSpan.addClassName(LumoUtility.TextColor.SUCCESS);
                electricityCostPing.setType(Ping.Type.LOW);
                spotAveragePing.setType(Ping.Type.LOW);
            }
        }
    }

    public static LinkedHashMap<Long, Double> mapChargingEventToConsumptionData(
            double chargingPowerKw, Instant startInstant, double lengthHours) {

        LinkedHashMap<Long, Double> consumptionData = new LinkedHashMap<>();
        Instant endInstant = startInstant.plusSeconds((long) (lengthHours * 3600));
        Instant intervalStart = startInstant.truncatedTo(ChronoUnit.HOURS);

        while (intervalStart.isBefore(endInstant)) {
            Instant intervalEnd = intervalStart.plus(1, ChronoUnit.HOURS);
            Instant actualStart = intervalStart.isBefore(startInstant) ? startInstant : intervalStart;
            Instant actualEnd = intervalEnd.isAfter(endInstant) ? endInstant : intervalEnd;
            double intervalDurationHours = Duration.between(actualStart, actualEnd).toSeconds() / 3600.0;

            if (intervalDurationHours > 0) {
                double energyConsumedKwh = chargingPowerKw * intervalDurationHours;
                long intervalStartEpochMilli = intervalStart.toEpochMilli();
                consumptionData.put(intervalStartEpochMilli, energyConsumedKwh);
            }
            intervalStart = intervalEnd;
        }
        return consumptionData;
    }

    public void readFieldValues() {
        WebStorage.getItem(batteryCapacityField.getId().orElseThrow(), item -> mapperService.readValue(item, batteryCapacityField));
        WebStorage.getItem(phasesField.getId().orElseThrow(), item -> mapperService.readValue(item, phasesField));
        WebStorage.getItem(voltageField.getId().orElseThrow(), item -> mapperService.readValue(item, voltageField));
        WebStorage.getItem(chargingLossField.getId().orElseThrow(), item -> mapperService.readValue(item, chargingLossField));
    }

    @Setter
    @Getter
    @AllArgsConstructor
    static class Charge {
        double capacity;
        double currentSOC;
        double targetSOC;
        int amperes;
        int phases;
        int voltage;
        double chargingLoss;
        CalculationTarget calculationTarget;
        LocalDateTime startTime;
    }

    @Getter
    @AllArgsConstructor
    public enum CalculationTarget {
        CHARGING_START("Start time"),
        CHARGING_END("End time");

        private final String name;
    }

    @VaadinSessionScope
    @Component
    public static class PreservedState {
        Charge charge = new Charge(
                75,
                29,
                80,
                16,
                3,
                230,
                10,
                CalculationTarget.CHARGING_END,
                LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        );
    }
}
