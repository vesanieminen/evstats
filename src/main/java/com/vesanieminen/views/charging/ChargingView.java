package com.vesanieminen.views.charging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.GridLayout;
import com.vesanieminen.components.Ping;
import com.vesanieminen.services.LiukuriService;
import com.vesanieminen.services.ObjectMapperService;
import com.vesanieminen.views.MainLayout;
import com.vesanieminen.views.SettingsView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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

    private final NumberField batteryCapacityField;
    private final NumberField currentSocField;
    private final NumberField targetSocField;
    private final IntegerField amperesField;
    private final IntegerField phasesField;
    private final IntegerField voltageField;
    private final NumberField chargingLossField;
    private final DateTimePicker chargingDateTimeField;
    private final DateTimePicker chargingResultDateTimeField;
    private final Span consumedElectricitySpan;
    private final Span lostElectricitySpan;
    private final Select<CalculationTarget> calculationTarget;
    private final Span chargingSpeedSpan;
    private final Span chargingSpeedMinusLossSpan;
    private final Span addedElectricitySpan;
    private final Span chargingLength;
    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private final LiukuriService liukuriService;
    private final Span electricityCostSpan;
    private final Span electricityCostValueSpan;
    private final Span spotAverage;
    private final Span spotAverageValue;
    private final ObjectMapperService mapperService;
    private final Ping electricityCostPing;
    private final Ping spotAveragePing;
    private final Span chargingLengthResult;
    private final Span chargingSpeedResultSpan;
    private final Span chargingSpeedMinusLossResultSpan;
    private final Span consumedElectricityResultSpan;
    private final Span addedElectricityResultSpan;
    private final Span lostElectricityResultSpan;
    private final SettingsView.SettingsState settingsState;

    public ChargingView(PreservedState preservedState, LiukuriService liukuriService, ObjectMapperService mapperService, SettingsView.SettingsState settingsState) {
        this.liukuriService = liukuriService;
        this.mapperService = mapperService;
        this.settingsState = settingsState;

        final var objectMapper = new ObjectMapper();
        WebStorage.getItem(SettingsView.margin, item -> {
            if (item == null) {
                return;
            }
            try {
                final var margin = objectMapper.readValue(item, new TypeReference<Double>() {
                });
                settingsState.getSettings().setMargin(margin);
            } catch (IOException e) {
                log.info("Could not read value: %s".formatted(e.toString()));
            }
        });
        WebStorage.getItem(SettingsView.vat, item -> {
            if (item == null) {
                return;
            }
            try {
                final var vat = objectMapper.readValue(item, new TypeReference<Boolean>() {
                });
                settingsState.getSettings().setVat(vat);
            } catch (IOException e) {
                log.info("Could not read value: %s".formatted(e.toString()));
            }
        });

        setHeight("var(--fullscreen-height-charging)");
        final var topGrid = new GridLayout();
        batteryCapacityField = new NumberField("Battery capacity");
        batteryCapacityField.setId("batteryCapacityField");
        batteryCapacityField.setStepButtonsVisible(true);
        batteryCapacityField.setSuffixComponent(new Span("kWh"));
        batteryCapacityField.setHelperText("e.g. 75 kWh");
        topGrid.add(batteryCapacityField);
        currentSocField = new NumberField("Current SOC");
        currentSocField.setId("currentSocField");
        currentSocField.setSuffixComponent(new Span("%"));
        currentSocField.setMin(0);
        currentSocField.setStepButtonsVisible(true);
        currentSocField.setHelperText("Current battery charge level");
        topGrid.add(currentSocField);
        targetSocField = new NumberField("Target SOC");
        targetSocField.setId("targetSocField");
        targetSocField.setMin(0);
        targetSocField.setSuffixComponent(new Span("%"));
        targetSocField.setStepButtonsVisible(true);
        targetSocField.setHelperText("Target battery charge level");
        topGrid.add(targetSocField);
        add(topGrid);

        amperesField = new IntegerField("Charging speed");
        amperesField.setId("amperesField");
        amperesField.setSuffixComponent(new Span("A"));
        amperesField.setMin(0);
        amperesField.setStepButtonsVisible(true);
        amperesField.setHelperText("In amperes (A)");
        topGrid.add(amperesField);
        phasesField = new IntegerField("Phases");
        phasesField.setId("phasesField");
        phasesField.setHelperText("How many phases are used?");
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setStepButtonsVisible(true);
        topGrid.add(phasesField);
        voltageField = new IntegerField("Voltage");
        voltageField.setId("voltageField");
        voltageField.setSuffixComponent(new Span("V"));
        voltageField.setHelperText("What voltage is used? (V)");
        voltageField.setMin(1);
        voltageField.setMax(1000);
        voltageField.setStepButtonsVisible(true);
        topGrid.add(voltageField);

        chargingLossField = new NumberField("Charging loss");
        chargingLossField.setId("chargingLossField");
        chargingLossField.setHelperText("How much goes to waste?");
        chargingLossField.setSuffixComponent(new Span("%"));
        chargingLossField.setStepButtonsVisible(true);
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        topGrid.add(chargingLossField);

        calculationTarget = new Select<>();
        calculationTarget.setId("calculationTarget");
        calculationTarget.setItems(CalculationTarget.values());
        calculationTarget.setItemLabelGenerator(CalculationTarget::getName);
        calculationTarget.setLabel("Calculate");
        topGrid.add(calculationTarget);

        chargingDateTimeField = new DateTimePicker();
        chargingDateTimeField.setId("chargingDateTimeField");
        chargingDateTimeField.setStep(Duration.ofMinutes(15));
        chargingDateTimeField.setLocale(Locale.of("fi", "FI"));
        chargingDateTimeField.addClassNames(LumoUtility.Grid.Column.COLUMN_SPAN_2);
        final var datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setFirstDayOfWeek(1);
        datePickerI18n.setDateFormat("EEE dd.MM.yyyy");
        chargingDateTimeField.setDatePickerI18n(datePickerI18n);
        topGrid.add(chargingDateTimeField);

        chargingResultDateTimeField = new DateTimePicker();
        chargingResultDateTimeField.setDatePickerI18n(datePickerI18n);
        chargingResultDateTimeField.addClassNames(LumoUtility.Grid.Column.COLUMN_SPAN_2);
        chargingResultDateTimeField.setStep(Duration.ofSeconds(1));
        chargingResultDateTimeField.setReadOnly(true);
        chargingResultDateTimeField.setLocale(Locale.of("fi", "FI"));
        topGrid.add(chargingResultDateTimeField);

        final var resultsGrid = new Div();
        resultsGrid.addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Grid.Column.COLUMNS_2,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Gap.Column.MEDIUM,
                LumoUtility.Margin.Top.SMALL,
                LumoUtility.MaxWidth.SCREEN_SMALL,
                LumoUtility.Gap.SMALL
        );
        chargingLength = new Span();
        chargingLengthResult = new Span();
        resultsGrid.add(chargingLength, chargingLengthResult);
        chargingSpeedSpan = new Span();
        chargingSpeedResultSpan = new Span();
        resultsGrid.add(chargingSpeedSpan, chargingSpeedResultSpan);
        chargingSpeedMinusLossSpan = new Span();
        chargingSpeedMinusLossResultSpan = new Span();
        resultsGrid.add(chargingSpeedMinusLossSpan, chargingSpeedMinusLossResultSpan);
        consumedElectricitySpan = new Span();
        consumedElectricityResultSpan = new Span();
        resultsGrid.add(consumedElectricitySpan, consumedElectricityResultSpan);
        addedElectricitySpan = new Span();
        addedElectricityResultSpan = new Span();
        resultsGrid.add(addedElectricitySpan, addedElectricityResultSpan);
        lostElectricitySpan = new Span();
        lostElectricityResultSpan = new Span();
        resultsGrid.add(lostElectricitySpan, lostElectricityResultSpan);

        spotAverage = new Span();
        spotAverageValue = new Span();
        spotAveragePing = new Ping("Price");
        final var spotAverageDiv = new Div(spotAverageValue, spotAveragePing);
        spotAverageDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.AlignItems.CENTER);
        resultsGrid.add(spotAverage, spotAverageDiv);

        electricityCostSpan = new Span();
        electricityCostValueSpan = new Span();
        electricityCostPing = new Ping("Cost");
        final var electricityCostDiv = new Div(electricityCostValueSpan, electricityCostPing);
        electricityCostDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.AlignItems.CENTER);
        resultsGrid.add(electricityCostSpan, electricityCostDiv);

        add(resultsGrid);

        final var chargeBinder = new Binder<Charge>();
        chargeBinder.bind(batteryCapacityField, Charge::getCapacity, Charge::setCapacity);
        chargeBinder.bind(currentSocField, Charge::getCurrentSOC, Charge::setCurrentSOC);
        chargeBinder.bind(targetSocField, Charge::getTargetSOC, Charge::setTargetSOC);
        //chargeBinder.forField(currentSocField).withValidator((value, context) -> value <= charge.getTargetSOC() ? ValidationResult.ok() : ValidationResult.error("Invalid SOC target")).bind(Charge::getCurrentSOC, Charge::setCurrentSOC);
        //chargeBinder.forField(targetSocField).withValidator((value, context) -> value >= charge.getCurrentSOC() ? ValidationResult.ok() : ValidationResult.error("Invalid SOC target")).bind(Charge::getTargetSOC, Charge::setTargetSOC);
        chargeBinder.bind(amperesField, Charge::getAmperes, Charge::setAmperes);
        chargeBinder.bind(phasesField, Charge::getPhases, Charge::setPhases);
        chargeBinder.bind(voltageField, Charge::getVoltage, Charge::setVoltage);
        chargeBinder.bind(chargingLossField, Charge::getChargingLoss, Charge::setChargingLoss);
        chargeBinder.bind(calculationTarget, Charge::getCalculationTarget, Charge::setCalculationTarget);
        chargeBinder.bind(chargingDateTimeField, Charge::getStartTime, Charge::setStartTime);

        chargeBinder.setBean(preservedState.charge);
        chargeBinder.addValueChangeListener(e -> {
            if (chargeBinder.isValid()) {
                doCalculation();
            } else {
                chargingLength.setText(null);
                chargingSpeedSpan.setText(null);
                chargingSpeedMinusLossSpan.setText(null);
                chargingResultDateTimeField.setValue(null);
                consumedElectricitySpan.setText(null);
                addedElectricitySpan.setText(null);
                lostElectricitySpan.setText(null);
            }
        });
        doCalculation();
        readFieldValues();

        batteryCapacityField.addValueChangeListener(item -> mapperService.saveFieldValue(batteryCapacityField));
        currentSocField.addValueChangeListener(item -> mapperService.saveFieldValue(currentSocField));
        targetSocField.addValueChangeListener(item -> mapperService.saveFieldValue(targetSocField));
        amperesField.addValueChangeListener(item -> mapperService.saveFieldValue(amperesField));
        phasesField.addValueChangeListener(item -> mapperService.saveFieldValue(phasesField));
        voltageField.addValueChangeListener(item -> mapperService.saveFieldValue(voltageField));
        chargingLossField.addValueChangeListener(item -> mapperService.saveFieldValue(chargingLossField));
        calculationTarget.addValueChangeListener(item -> mapperService.saveFieldValue(calculationTarget));
        chargingDateTimeField.addValueChangeListener(item -> mapperService.saveFieldValue(chargingDateTimeField));

        final var calculationRange = liukuriService.getValidCalculationRange();
        final var start = Instant.ofEpochMilli(calculationRange.getStart());
        final var end = Instant.ofEpochMilli(calculationRange.getEnd());
        chargingDateTimeField.setMin(start.atZone(fiZoneID).toLocalDateTime());
        chargingDateTimeField.setMax(end.atZone(fiZoneID).toLocalDateTime());
    }

    private void doCalculation() {
        var socIncrease = targetSocField.getValue() - currentSocField.getValue();
        var capacityIncrease = batteryCapacityField.getValue() / 100 * socIncrease;
        var chargingPowerInWatts = amperesField.getValue() * phasesField.getValue() * voltageField.getValue();
        var chargingSpeedMinusLoss = chargingPowerInWatts * ((100 - chargingLossField.getValue()) / 100);
        var chargingTimeHours = capacityIncrease * 1000 / chargingSpeedMinusLoss;
        var chargingTimeSeconds = (int) (chargingTimeHours * 3600);

        Instant chargingStartTime;
        if (calculationTarget.getValue() == CalculationTarget.CHARGING_END) {
            chargingDateTimeField.setLabel("Select charging start");
            var chargingResultTime = chargingDateTimeField.getValue().plusSeconds(chargingTimeSeconds);
            chargingResultDateTimeField.setValue(chargingResultTime);
            chargingResultDateTimeField.setLabel("Calculated charging end time");

            chargingStartTime = ZonedDateTime.of(chargingDateTimeField.getValue(), fiZoneID).toInstant();
        } else {
            chargingDateTimeField.setLabel("Select charging end");
            var chargingResultTime = chargingDateTimeField.getValue().minusSeconds(chargingTimeSeconds);
            chargingResultDateTimeField.setValue(chargingResultTime);
            chargingResultDateTimeField.setLabel("Calculated charging start time");

            chargingStartTime = ZonedDateTime.of(chargingResultDateTimeField.getValue(), fiZoneID).toInstant();
        }

        chargingLength.setText("Charging length: ");
        chargingLengthResult.setText("%d h, %d min, %d sec".formatted((int) chargingTimeHours, (chargingTimeSeconds % 3600) / 60, chargingTimeSeconds % 60));

        final var chargingPowerInKilowatts = chargingPowerInWatts / 1000.0;
        chargingSpeedSpan.setText("Charging speed: ");
        chargingSpeedResultSpan.setText("%.2f kW".formatted(chargingPowerInKilowatts));
        chargingSpeedMinusLossSpan.setText("Charging speed minus loss: ");
        chargingSpeedMinusLossResultSpan.setText("%.2f kW".formatted(chargingSpeedMinusLoss / 1000.0));

        var electricityConsumed = (chargingPowerInWatts / 1000.0) * chargingTimeHours;
        final var electricityConsumedText = "%.2f kWh".formatted(electricityConsumed);
        consumedElectricitySpan.setText("Consumed electricity: ");
        consumedElectricityResultSpan.setText(electricityConsumedText);

        final var lostPercentage = chargingLossField.getValue() / 100.0;
        final var lostElectricity = electricityConsumed * lostPercentage;
        final var electricityLostText = "%.2f kWh".formatted(lostElectricity);
        lostElectricitySpan.setText("Lost electricity: ");
        lostElectricityResultSpan.setText(electricityLostText);

        var addedElectricity = electricityConsumed - lostElectricity;
        final var addedElectricityText = "%.2f kWh".formatted(addedElectricity);
        addedElectricitySpan.setText("Charge added to battery: ");
        addedElectricityResultSpan.setText(addedElectricityText);

        final var longDoubleLinkedHashMap = mapChargingEventToConsumptionData(chargingPowerInKilowatts, chargingStartTime, chargingTimeHours);
        final var margin = settingsState.getSettings().getMargin();
        final var vat = settingsState.getSettings().getVat();
        final var calculationResponse = liukuriService.performCalculation(longDoubleLinkedHashMap, margin == null ? 0 : margin, vat != null && vat);
        if (calculationResponse != null) {
            final var averagePrice = calculationResponse.getAveragePrice();
            electricityCostSpan.setText(Boolean.TRUE.equals(vat) ? "Total cost (inc. VAT): " : "Total cost: ");
            electricityCostValueSpan.setText("%.2f €".formatted(calculationResponse.getTotalCost()));
            spotAverage.setText("Spot average (inc. margin): ");
            spotAverageValue.setText("%.2f c/kWh".formatted(averagePrice));
            if (averagePrice >= 10) {
                electricityCostValueSpan.setClassName(LumoUtility.TextColor.ERROR);
                spotAverageValue.setClassName(LumoUtility.TextColor.ERROR);
                electricityCostPing.setType(Ping.Type.HIGH);
                spotAveragePing.setType(Ping.Type.HIGH);
            } else if (averagePrice >= 5) {
                electricityCostValueSpan.setClassName(LumoUtility.TextColor.PRIMARY);
                spotAverageValue.setClassName(LumoUtility.TextColor.PRIMARY);
                electricityCostPing.setType(Ping.Type.NORMAL);
                spotAveragePing.setType(Ping.Type.NORMAL);
            } else if (averagePrice < 5) {
                electricityCostValueSpan.setClassName(LumoUtility.TextColor.SUCCESS);
                spotAverageValue.setClassName(LumoUtility.TextColor.SUCCESS);
                electricityCostPing.setType(Ping.Type.LOW);
                spotAveragePing.setType(Ping.Type.LOW);
            }
        }
    }

    public static LinkedHashMap<Long, Double> mapChargingEventToConsumptionData(
            double chargingPowerKw, Instant startInstant, double lengthHours) {

        LinkedHashMap<Long, Double> consumptionData = new LinkedHashMap<>();

        // Calculate the end time of the charging event
        Instant endInstant = startInstant.plusSeconds((long) (lengthHours * 3600));

        // Round down the start time to the previous whole hour
        Instant intervalStart = startInstant.truncatedTo(ChronoUnit.HOURS);

        while (intervalStart.isBefore(endInstant)) {
            // The end of the current interval is one hour later
            Instant intervalEnd = intervalStart.plus(1, ChronoUnit.HOURS);

            // Determine the actual start and end times within the charging event
            Instant actualStart = intervalStart.isBefore(startInstant) ? startInstant : intervalStart;
            Instant actualEnd = intervalEnd.isAfter(endInstant) ? endInstant : intervalEnd;

            // Calculate the duration of the actual interval in hours
            double intervalDurationHours = Duration.between(actualStart, actualEnd).toSeconds() / 3600.0;

            // Skip intervals with zero duration
            if (intervalDurationHours > 0) {
                // Calculate the energy consumed during this interval
                double energyConsumedKwh = chargingPowerKw * intervalDurationHours;

                // Key is the start time of the interval in epoch milliseconds
                long intervalStartEpochMilli = intervalStart.toEpochMilli();

                // Store the interval's start time and energy consumed in the map
                consumptionData.put(intervalStartEpochMilli, energyConsumedKwh);
            }

            // Move to the next interval
            intervalStart = intervalEnd;
        }

        return consumptionData;
    }

    public void readFieldValues() {
        WebStorage.getItem(batteryCapacityField.getId().orElseThrow(), item -> mapperService.readValue(item, batteryCapacityField));
        WebStorage.getItem(currentSocField.getId().orElseThrow(), item -> mapperService.readValue(item, currentSocField));
        WebStorage.getItem(targetSocField.getId().orElseThrow(), item -> mapperService.readValue(item, targetSocField));
        WebStorage.getItem(amperesField.getId().orElseThrow(), item -> mapperService.readValue(item, amperesField));
        WebStorage.getItem(phasesField.getId().orElseThrow(), item -> mapperService.readValue(item, phasesField));
        WebStorage.getItem(voltageField.getId().orElseThrow(), item -> mapperService.readValue(item, voltageField));
        WebStorage.getItem(chargingLossField.getId().orElseThrow(), item -> mapperService.readValue(item, chargingLossField));
        WebStorage.getItem(chargingDateTimeField.getId().orElseThrow(), item -> mapperService.readLocalDateTime(item, chargingDateTimeField));
        WebStorage.getItem(calculationTarget.getId().orElseThrow(), item -> mapperService.readCalculationTarget(item, calculationTarget));
    }

    @Setter
    @Getter
    @AllArgsConstructor
    //@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
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
    //@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
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
                20,
                50,
                16,
                3,
                230,
                10,
                CalculationTarget.CHARGING_END,
                LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        );
    }

}
