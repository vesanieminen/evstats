package com.vesanieminen.views.evstatistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.GridLayout;
import com.vesanieminen.services.LiukuriService;
import com.vesanieminen.views.MainLayout;
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
    private final Span spotAverage;
    private final ObjectMapper objectMapper;

    public ChargingView(PreservedState preservedState, LiukuriService liukuriService, ObjectMapper objectMapper) {
        this.liukuriService = liukuriService;
        this.objectMapper = objectMapper;

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
        topGrid.add(chargingDateTimeField);

        chargingResultDateTimeField = new DateTimePicker();
        chargingResultDateTimeField.addClassNames(LumoUtility.Grid.Column.COLUMN_SPAN_2);
        chargingResultDateTimeField.setStep(Duration.ofSeconds(1));
        chargingResultDateTimeField.setReadOnly(true);
        chargingResultDateTimeField.setLocale(Locale.of("fi", "FI"));
        topGrid.add(chargingResultDateTimeField);

        final var fourthRow = new GridLayout();
        fourthRow.removeClassNames(
                LumoUtility.Grid.Breakpoint.Large.COLUMNS_3,
                LumoUtility.Grid.Column.COLUMNS_2
        );
        fourthRow.addClassNames(LumoUtility.Grid.Breakpoint.Small.COLUMNS_1);
        chargingLength = new Span();
        chargingLength.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(chargingLength);
        chargingSpeedSpan = new Span();
        chargingSpeedSpan.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(chargingSpeedSpan);
        chargingSpeedMinusLossSpan = new Span();
        chargingSpeedMinusLossSpan.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(chargingSpeedMinusLossSpan);
        consumedElectricitySpan = new Span();
        consumedElectricitySpan.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(consumedElectricitySpan);
        addedElectricitySpan = new Span();
        addedElectricitySpan.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(addedElectricitySpan);
        lostElectricitySpan = new Span();
        lostElectricitySpan.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(lostElectricitySpan);
        electricityCostSpan = new Span();
        electricityCostSpan.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(electricityCostSpan);
        spotAverage = new Span();
        spotAverage.addClassNames(LumoUtility.FontSize.SMALL);
        fourthRow.add(spotAverage);
        add(fourthRow);

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

        batteryCapacityField.addValueChangeListener(item -> saveFieldValue(batteryCapacityField));
        currentSocField.addValueChangeListener(item -> saveFieldValue(currentSocField));
        targetSocField.addValueChangeListener(item -> saveFieldValue(targetSocField));
        amperesField.addValueChangeListener(item -> saveFieldValue(amperesField));
        phasesField.addValueChangeListener(item -> saveFieldValue(phasesField));
        voltageField.addValueChangeListener(item -> saveFieldValue(voltageField));
        chargingLossField.addValueChangeListener(item -> saveFieldValue(chargingLossField));
        calculationTarget.addValueChangeListener(item -> saveFieldValue(calculationTarget));
        chargingDateTimeField.addValueChangeListener(item -> saveFieldValue(chargingDateTimeField));

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

        chargingLength.setText("Charging length: %d h, %d min, %d sec".formatted((int) chargingTimeHours, (chargingTimeSeconds % 3600) / 60, chargingTimeSeconds % 60));

        final var chargingPowerInKilowatts = chargingPowerInWatts / 1000.0;
        chargingSpeedSpan.setText("Charging speed: %.2f kW".formatted(chargingPowerInKilowatts));
        chargingSpeedMinusLossSpan.setText("Charging speed minus loss: %.2f kW".formatted(chargingSpeedMinusLoss / 1000.0));

        var electricityConsumed = (chargingPowerInWatts / 1000.0) * chargingTimeHours;
        final var electricityConsumedText = "Consumed electricity: %.2f kWh".formatted(electricityConsumed);
        consumedElectricitySpan.setText(electricityConsumedText);

        final var lostPercentage = chargingLossField.getValue() / 100.0;
        final var lostElectricity = electricityConsumed * lostPercentage;
        final var electricityLostText = "Lost electricity: %.2f kWh".formatted(lostElectricity);
        lostElectricitySpan.setText(electricityLostText);

        var addedElectricity = electricityConsumed - lostElectricity;
        final var addedElectricityText = "Charge added to battery: %.2f kWh".formatted(addedElectricity);
        addedElectricitySpan.setText(addedElectricityText);


        final var longDoubleLinkedHashMap = mapChargingEventToConsumptionData(chargingPowerInKilowatts, chargingStartTime, chargingTimeHours);
        final var calculationResponse = liukuriService.performCalculation(longDoubleLinkedHashMap, 0, true);
        if (calculationResponse != null) {

            electricityCostSpan.setText("Total cost (inc. VAT): %.2f €".formatted(calculationResponse.getTotalCost()));
            spotAverage.setText("Spot average: %.2f c/kWh".formatted(calculationResponse.getAveragePrice()));
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

    public <C extends AbstractField<C, T>, T> void saveFieldValue(AbstractField<C, T> field) {
        try {
            WebStorage.setItem(field.getId().orElseThrow(), objectMapper.writeValueAsString(field.getValue()));
        } catch (JsonProcessingException e) {
            log.info("Could not save value: %s".formatted(e.toString()));
        }
    }

    public void readFieldValues() {
        WebStorage.getItem(batteryCapacityField.getId().orElseThrow(), item -> readValue(item, batteryCapacityField));
        WebStorage.getItem(currentSocField.getId().orElseThrow(), item -> readValue(item, currentSocField));
        WebStorage.getItem(targetSocField.getId().orElseThrow(), item -> readValue(item, targetSocField));
        WebStorage.getItem(amperesField.getId().orElseThrow(), item -> readValue(item, amperesField));
        WebStorage.getItem(phasesField.getId().orElseThrow(), item -> readValue(item, phasesField));
        WebStorage.getItem(voltageField.getId().orElseThrow(), item -> readValue(item, voltageField));
        WebStorage.getItem(chargingLossField.getId().orElseThrow(), item -> readValue(item, chargingLossField));

        WebStorage.getItem(chargingDateTimeField.getId().orElseThrow(), item -> {
            if (item == null) {
                return;
            }
            try {
                var value = objectMapper.readValue(item, new TypeReference<LocalDateTime>() {
                });
                chargingDateTimeField.setValue(value);
            } catch (IOException e) {
                log.info("Could not read value: %s".formatted(e.toString()));
            }
        });

        WebStorage.getItem(calculationTarget.getId().orElseThrow(), item -> {
            if (item == null) {
                return;
            }
            try {
                var value = objectMapper.readValue(item, new TypeReference<CalculationTarget>() {
                });
                calculationTarget.setValue(value);
            } catch (IOException e) {
                log.info("Could not read value: %s".formatted(e.toString()));
            }
        });
    }


    public <C extends HasValue.ValueChangeEvent<T>, T> void readValue(String key, HasValue<C, T> hasValue) {
        if (key == null) {
            return;
        }
        try {
            T value = objectMapper.readValue(key, new TypeReference<>() {
            });
            hasValue.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }

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
    enum CalculationTarget {
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