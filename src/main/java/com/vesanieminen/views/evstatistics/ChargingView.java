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
        batteryCapacityField.setValue(75d);
        batteryCapacityField.setHelperText("e.g. 75 kWh");
        topGrid.add(batteryCapacityField);
        var currentSocField = new IntegerField("Current SOC");
        currentSocField.setSuffixComponent(new Span("%"));
        currentSocField.setValue(20);
        currentSocField.setStep(1);
        currentSocField.setMin(0);
        currentSocField.setStepButtonsVisible(true);
        currentSocField.setHelperText("Current battery charge level");
        topGrid.add(currentSocField);
        var targetSocField = new IntegerField("Target SOC");
        targetSocField.setMin(0);
        targetSocField.setSuffixComponent(new Span("%"));
        targetSocField.setStep(1);
        targetSocField.setValue(50);
        targetSocField.setStepButtonsVisible(true);
        targetSocField.setHelperText("Target battery charge level");
        topGrid.add(targetSocField);
        add(topGrid);

        final var secondRow = new GridLayout();
        var chargingSpeedField = new IntegerField("Charging speed");
        chargingSpeedField.setValue(16);
        chargingSpeedField.setMin(0);
        chargingSpeedField.setStepButtonsVisible(true);
        chargingSpeedField.setHelperText("In amperes (A)");
        secondRow.add(chargingSpeedField);
        var phasesField = new IntegerField("Phases");
        phasesField.setHelperText("How many phases are used?");
        phasesField.setValue(3);
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setStepButtonsVisible(true);
        NumberField chargingLossField = new NumberField("Charging loss");
        chargingLossField.setHelperText("How much goes to waste?");
        chargingLossField.setSuffixComponent(new Span("%"));
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        chargingLossField.setValue(15d);
        secondRow.add(phasesField);
        secondRow.add(chargingLossField);
        add(secondRow);

        final var thirdRow = new GridLayout();
        TimePicker chargingStartTime = new TimePicker("Charging start time");
        chargingStartTime.setStep(Duration.ofMinutes(15));
        chargingStartTime.setLocale(new Locale("fi", "FI"));
        thirdRow.add(chargingStartTime);
        TimePicker chargingEndTime = new TimePicker("Calculated charging end time");
        chargingEndTime.setStep(Duration.ofMinutes(1));
        chargingEndTime.setReadOnly(true);
        chargingEndTime.setValue(LocalTime.of(2, 30));
        chargingEndTime.setLocale(new Locale("fi", "FI"));
        thirdRow.add(chargingEndTime);
        add(thirdRow);

        var charge = new Charge(0, 0, 0, 0, 0, 0, LocalTime.MIN);

        final var chargeBinder = new Binder<Charge>();
        chargeBinder.bind(batteryCapacityField, Charge::getCapacity, Charge::setCapacity);
        chargeBinder.bind(currentSocField, Charge::getCurrentSOC, Charge::setCurrentSOC);
        chargeBinder.bind(targetSocField, Charge::getTargetSOC, Charge::setTargetSOC);
        chargeBinder.bind(chargingSpeedField, Charge::getChargingSpeed, Charge::setChargingSpeed);
        chargeBinder.bind(phasesField, Charge::getPhases, Charge::setPhases);
        chargeBinder.bind(chargingLossField, Charge::getChargingLoss, Charge::setChargingLoss);
        chargeBinder.bind(chargingStartTime, Charge::getStartTime, Charge::setStartTime);
    }

    static class Charge {
        double capacity;
        int currentSOC;
        int targetSOC;
        int chargingSpeed;
        int phases;
        double chargingLoss;
        LocalTime startTime;

        public Charge() {
        }

        public Charge(double capacity, int currentSOC, int targetSOC, int chargingSpeed, int phases, double chargingLoss, LocalTime startTime) {
            this.capacity = capacity;
            this.currentSOC = currentSOC;
            this.targetSOC = targetSOC;
            this.chargingSpeed = chargingSpeed;
            this.phases = phases;
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

        public int getChargingSpeed() {
            return chargingSpeed;
        }

        public void setChargingSpeed(int chargingSpeed) {
            this.chargingSpeed = chargingSpeed;
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
    }


}