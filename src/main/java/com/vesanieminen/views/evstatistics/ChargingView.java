package com.vesanieminen.views.evstatistics;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.components.GridLayout;
import com.vesanieminen.views.MainLayout;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

@PageTitle("Charging tool")
@Route(value = "charging", layout = MainLayout.class)
public class ChargingView extends Main {

    public ChargingView() {
        final var topGrid = new GridLayout();
        var batteryCapacityField = new NumberField("Battery capacity");
        batteryCapacityField.setSuffixComponent(new Span("kWh"));
        batteryCapacityField.setHelperText("e.g. 75 kWh");
        topGrid.add(batteryCapacityField);
        var currentSocField = new IntegerField("Current SOC");
        currentSocField.setSuffixComponent(new Span("%"));
        currentSocField.setStep(1);
        currentSocField.setMin(0);
        currentSocField.setStepButtonsVisible(true);
        currentSocField.setHelperText("Current battery charge level");
        topGrid.add(currentSocField);
        var targetSocField = new IntegerField("Target SOC");
        targetSocField.setMin(0);
        targetSocField.setSuffixComponent(new Span("%"));
        targetSocField.setStep(1);
        targetSocField.setStepButtonsVisible(true);
        targetSocField.setHelperText("Target battery charge level");
        topGrid.add(targetSocField);
        add(topGrid);

        final var secondRow = new GridLayout();
        var amperesField = new IntegerField("Charging speed");
        amperesField.setMin(0);
        amperesField.setStepButtonsVisible(true);
        amperesField.setHelperText("In amperes (A)");
        secondRow.add(amperesField);
        var phasesField = new IntegerField("Phases");
        phasesField.setHelperText("How many phases are used?");
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setStepButtonsVisible(true);
        secondRow.add(phasesField);
        var voltageField = new IntegerField("Voltage");
        voltageField.setHelperText("What voltage is used? (V)");
        voltageField.setMin(1);
        voltageField.setMax(1000);
        voltageField.setStepButtonsVisible(true);
        secondRow.add(voltageField);
        add(secondRow);

        final var thirdRow = new GridLayout();
        NumberField chargingLossField = new NumberField("Charging loss");
        chargingLossField.setHelperText("How much goes to waste?");
        chargingLossField.setSuffixComponent(new Span("%"));
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        thirdRow.add(chargingLossField);
        TimePicker chargingStartTimeField = new TimePicker("Charging start time");
        chargingStartTimeField.setStep(Duration.ofMinutes(15));
        chargingStartTimeField.setLocale(new Locale("fi", "FI"));
        thirdRow.add(chargingStartTimeField);
        TimePicker chargingEndTimeField = new TimePicker("Calculated charging end time");
        chargingEndTimeField.setStep(Duration.ofMinutes(1));
        chargingEndTimeField.setReadOnly(true);
        chargingEndTimeField.setLocale(new Locale("fi", "FI"));
        thirdRow.add(chargingEndTimeField);
        add(thirdRow);

        final var chargeBinder = new Binder<Charge>();
        chargeBinder.bind(batteryCapacityField, Charge::getCapacity, Charge::setCapacity);
        chargeBinder.bind(currentSocField, Charge::getCurrentSOC, Charge::setCurrentSOC);
        chargeBinder.bind(targetSocField, Charge::getTargetSOC, Charge::setTargetSOC);
        chargeBinder.bind(amperesField, Charge::getAmperes, Charge::setAmperes);
        chargeBinder.bind(phasesField, Charge::getPhases, Charge::setPhases);
        chargeBinder.bind(voltageField, Charge::getVoltage, Charge::setVoltage);
        chargeBinder.bind(chargingLossField, Charge::getChargingLoss, Charge::setChargingLoss);
        chargeBinder.bind(chargingStartTimeField, Charge::getStartTime, Charge::setStartTime);

        final var charge = new Charge(
                75,
                20,
                50,
                16,
                3,
                230,
                15,
                LocalTime.of(0, 0)
        );
        chargeBinder.setBean(charge);

        chargeBinder.addValueChangeListener(e -> {
            var socIncrease = targetSocField.getValue() - currentSocField.getValue();
            var capacityIncrease = batteryCapacityField.getValue() / 100 * socIncrease;
            var chargingSpeed = amperesField.getValue() * phasesField.getValue() * voltageField.getValue();
            var chargingSpeedMinusLoss = chargingSpeed * ((100 - chargingLossField.getValue()) / 100);
            var chargingTimeHours = capacityIncrease * 1000 / chargingSpeedMinusLoss;
            var chargingTimeMinutes = chargingTimeHours * 60;
            var chargingEndTime = chargingStartTimeField.getValue().plusMinutes((long) chargingTimeMinutes);
            chargingEndTimeField.setValue(chargingEndTime);
        });
    }

    static class Charge {
        double capacity;
        int currentSOC;
        int targetSOC;
        int amperes;
        int phases;
        int voltage;
        double chargingLoss;
        LocalTime startTime;

        public Charge() {
        }

        public Charge(double capacity, int currentSOC, int targetSOC, int amperes, int phases, int voltage, double chargingLoss, LocalTime startTime) {
            this.capacity = capacity;
            this.currentSOC = currentSOC;
            this.targetSOC = targetSOC;
            this.amperes = amperes;
            this.phases = phases;
            this.voltage = voltage;
            this.chargingLoss = chargingLoss;
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
    }

}