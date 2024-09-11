package com.vesanieminen.views.evstatistics;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.GridLayout;
import com.vesanieminen.views.MainLayout;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

@PageTitle("Charging tool")
@Route(value = "charging", layout = MainLayout.class)
@PreserveOnRefresh
public class ChargingView extends Main {

    private final NumberField batteryCapacityField;
    private final IntegerField currentSocField;
    private final IntegerField targetSocField;
    private final IntegerField amperesField;
    private final IntegerField phasesField;
    private final IntegerField voltageField;
    private final NumberField chargingLossField;
    private final TimePicker chargingTimeField;
    private final TimePicker chargingResultTimeField;
    private final Span consumedElectricitySpan;
    private final Span lostElectricitySpan;
    private final Select<CalculationTarget> calculationTarget;
    private final Span chargingSpeedSpan;
    private final Span chargingSpeedMinusLossSpan;
    private final Span addedElectricitySpan;
    private final Span chargingLength;

    public ChargingView(PreservedState preservedState) {
        final var topGrid = new GridLayout();
        batteryCapacityField = new NumberField("Battery capacity");
        batteryCapacityField.setStepButtonsVisible(true);
        batteryCapacityField.setSuffixComponent(new Span("kWh"));
        batteryCapacityField.setHelperText("e.g. 75 kWh");
        topGrid.add(batteryCapacityField);
        currentSocField = new IntegerField("Current SOC");
        currentSocField.setSuffixComponent(new Span("%"));
        currentSocField.setStep(1);
        currentSocField.setMin(0);
        currentSocField.setStepButtonsVisible(true);
        currentSocField.setHelperText("Current battery charge level");
        topGrid.add(currentSocField);
        targetSocField = new IntegerField("Target SOC");
        targetSocField.setMin(0);
        targetSocField.setSuffixComponent(new Span("%"));
        targetSocField.setStep(1);
        targetSocField.setStepButtonsVisible(true);
        targetSocField.setHelperText("Target battery charge level");
        topGrid.add(targetSocField);
        add(topGrid);

        amperesField = new IntegerField("Charging speed");
        amperesField.setSuffixComponent(new Span("A"));
        amperesField.setMin(0);
        amperesField.setStepButtonsVisible(true);
        amperesField.setHelperText("In amperes (A)");
        topGrid.add(amperesField);
        phasesField = new IntegerField("Phases");
        phasesField.setHelperText("How many phases are used?");
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setStepButtonsVisible(true);
        topGrid.add(phasesField);
        voltageField = new IntegerField("Voltage");
        voltageField.setSuffixComponent(new Span("V"));
        voltageField.setHelperText("What voltage is used? (V)");
        voltageField.setMin(1);
        voltageField.setMax(1000);
        voltageField.setStepButtonsVisible(true);
        topGrid.add(voltageField);

        chargingLossField = new NumberField("Charging loss");
        chargingLossField.setHelperText("How much goes to waste?");
        chargingLossField.setSuffixComponent(new Span("%"));
        chargingLossField.setStepButtonsVisible(true);
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        topGrid.add(chargingLossField);

        calculationTarget = new Select<>();
        calculationTarget.setItems(CalculationTarget.values());
        calculationTarget.setItemLabelGenerator(CalculationTarget::getName);
        calculationTarget.setLabel("Calculation target");
        topGrid.add(calculationTarget);

        chargingTimeField = new TimePicker();
        chargingTimeField.setStep(Duration.ofMinutes(15));
        chargingTimeField.setLocale(new Locale("fi", "FI"));
        topGrid.add(chargingTimeField);

        final var fourthRow = new GridLayout();
        fourthRow.removeClassNames(
                LumoUtility.Grid.Breakpoint.Large.COLUMNS_3,
                LumoUtility.Grid.Column.COLUMNS_2
        );
        fourthRow.addClassNames(LumoUtility.Grid.Breakpoint.Small.COLUMNS_1);
        chargingResultTimeField = new TimePicker();
        //chargingResultTimeField.addClassNames(LumoUtility.Grid.Column.COLUMN_SPAN_2);
        chargingResultTimeField.setStep(Duration.ofMinutes(1));
        chargingResultTimeField.setReadOnly(true);
        chargingResultTimeField.setLocale(new Locale("fi", "FI"));
        fourthRow.add(chargingResultTimeField);
        chargingLength = new Span();
        chargingLength.setClassName("text-s");
        fourthRow.add(chargingLength);
        chargingSpeedSpan = new Span();
        chargingSpeedSpan.setClassName("text-s");
        fourthRow.add(chargingSpeedSpan);
        chargingSpeedMinusLossSpan = new Span();
        chargingSpeedMinusLossSpan.setClassName("text-s");
        fourthRow.add(chargingSpeedMinusLossSpan);
        consumedElectricitySpan = new Span();
        consumedElectricitySpan.setClassName("text-s");
        fourthRow.add(consumedElectricitySpan);
        addedElectricitySpan = new Span();
        addedElectricitySpan.setClassName("text-s");
        fourthRow.add(addedElectricitySpan);
        lostElectricitySpan = new Span();
        lostElectricitySpan.setClassName("text-s");
        fourthRow.add(lostElectricitySpan);
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
        chargeBinder.bind(chargingTimeField, Charge::getStartTime, Charge::setStartTime);

        chargeBinder.setBean(preservedState.charge);
        chargeBinder.addValueChangeListener(e -> {
            if (chargeBinder.isValid()) {
                doCalculation();
            } else {
                chargingLength.setText(null);
                chargingSpeedSpan.setText(null);
                chargingSpeedMinusLossSpan.setText(null);
                chargingResultTimeField.setValue(null);
                consumedElectricitySpan.setText(null);
                addedElectricitySpan.setText(null);
                lostElectricitySpan.setText(null);
            }
        });
        doCalculation();
    }

    private void doCalculation() {
        var socIncrease = targetSocField.getValue() - currentSocField.getValue();
        var capacityIncrease = batteryCapacityField.getValue() / 100 * socIncrease;
        var chargingSpeedInWatts = amperesField.getValue() * phasesField.getValue() * voltageField.getValue();
        var chargingSpeedMinusLoss = chargingSpeedInWatts * ((100 - chargingLossField.getValue()) / 100);
        var chargingTimeHours = capacityIncrease * 1000 / chargingSpeedMinusLoss;
        var chargingTimeSeconds = (int) (chargingTimeHours * 3600);

        if (calculationTarget.getValue() == CalculationTarget.CHARGING_END) {
            chargingTimeField.setLabel("Charging start time");
            var chargingEndTime = chargingTimeField.getValue().plusSeconds(chargingTimeSeconds);
            chargingResultTimeField.setValue(chargingEndTime);
            chargingResultTimeField.setLabel("Calculated charging end time");
        } else {
            chargingTimeField.setLabel("Charging end time");
            var chargingEndTime = chargingTimeField.getValue().minusSeconds(chargingTimeSeconds);
            chargingResultTimeField.setValue(chargingEndTime);
            chargingResultTimeField.setLabel("Calculated charging start time");
        }

        chargingLength.setText("Charging length: %.0f h, %d min, %d sec".formatted(chargingTimeHours, chargingTimeSeconds % 3600 / 60, chargingTimeSeconds % 60));

        chargingSpeedSpan.setText("Charging speed: %.2f kW".formatted(chargingSpeedInWatts / 1000.0));
        chargingSpeedMinusLossSpan.setText("Charging speed minus loss: %.2f kW".formatted(chargingSpeedMinusLoss / 1000.0));

        var electricityConsumed = (chargingSpeedInWatts / 1000.0) * chargingTimeHours;
        final var electricityConsumedText = "Consumed electricity: %.2f kWh".formatted(electricityConsumed);
        consumedElectricitySpan.setText(electricityConsumedText);

        final var lostPercentage = chargingLossField.getValue() / 100.0;
        final var lostElectricity = electricityConsumed * lostPercentage;
        final var electricityLostText = "Lost electricity: %.2f kWh".formatted(lostElectricity);
        lostElectricitySpan.setText(electricityLostText);

        var addedElectricity = electricityConsumed - lostElectricity;
        final var addedElectricityText = "Charge added to battery: %.2f kWh".formatted(addedElectricity);
        addedElectricitySpan.setText(addedElectricityText);

    }

    static class Charge {
        double capacity;
        int currentSOC;
        int targetSOC;
        int amperes;
        int phases;
        int voltage;
        double chargingLoss;
        CalculationTarget calculationTarget;
        LocalTime startTime;

        public Charge() {
        }

        public Charge(double capacity, int currentSOC, int targetSOC, int amperes, int phases, int voltage, double chargingLoss, CalculationTarget calculationTarget, LocalTime startTime) {
            this.capacity = capacity;
            this.currentSOC = currentSOC;
            this.targetSOC = targetSOC;
            this.amperes = amperes;
            this.phases = phases;
            this.voltage = voltage;
            this.chargingLoss = chargingLoss;
            this.calculationTarget = calculationTarget;
            this.startTime = startTime;
        }

        public double getCapacity() {
            return capacity;
        }

        public void setCapacity(double capacity) {
            this.capacity = capacity;
        }

        public int getCurrentSOC() {
            return currentSOC;
        }

        public void setCurrentSOC(int currentSOC) {
            this.currentSOC = currentSOC;
        }

        public int getTargetSOC() {
            return targetSOC;
        }

        public void setTargetSOC(int targetSOC) {
            this.targetSOC = targetSOC;
        }

        public int getAmperes() {
            return amperes;
        }

        public void setAmperes(int amperes) {
            this.amperes = amperes;
        }

        public int getPhases() {
            return phases;
        }

        public void setPhases(int phases) {
            this.phases = phases;
        }

        public double getChargingLoss() {
            return chargingLoss;
        }

        public void setChargingLoss(double chargingLoss) {
            this.chargingLoss = chargingLoss;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public int getVoltage() {
            return voltage;
        }

        public void setVoltage(int voltage) {
            this.voltage = voltage;
        }

        public CalculationTarget getCalculationTarget() {
            return calculationTarget;
        }

        public void setCalculationTarget(CalculationTarget calculationTarget) {
            this.calculationTarget = calculationTarget;
        }
    }

    enum CalculationTarget {
        CHARGING_START("Start time"),
        CHARGING_END("End time");

        private final String name;

        CalculationTarget(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
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
                LocalTime.of(0, 0)
        );
    }

}