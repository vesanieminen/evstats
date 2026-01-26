package com.vesanieminen.views.charging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.vaadin.lineawesome.LineAwesomeIcon;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final TimePicker startTimePicker;

    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private final LiukuriService liukuriService;
    private final ObjectMapperService mapperService;
    private final SettingsDialog.SettingsState settingsState;

    // Display elements
    private final Span currentSocDisplay;
    private final Span targetSocDisplay;
    private final Span rangeDisplay;
    private final Span speedKwDisplay;
    private final Span endTimeDisplay;
    private final Span durationDisplay;
    private final Span toBatteryDisplay;
    private final Span consumedDisplay;
    private final Span lostDisplay;
    private final Span spotPriceDisplay;
    private final Span costDisplay;
    private final Div chargeBarCurrent;
    private final Div chargeBarFill;
    private final Div chargeMarkerCurrent;
    private final Div chargeMarkerTarget;
    private final Span amperesDisplay;

    // Advanced section
    private final Div advancedSection;
    private boolean advancedVisible = false;
    private final Button phase1Button;
    private final Button phase2Button;
    private final Button phase3Button;

    // Car selector
    private final Paragraph carNameDisplay;
    private final Paragraph capacityDisplay;
    private final Div carSelectorDropdown;
    private boolean carSelectorVisible = false;

    // Schedule mode
    private boolean calculateStartMode = false;
    private TimePicker endTimePicker;
    private Span startTimeDisplay;

    // EV Models
    private static final List<EvModel> EV_MODELS = List.of(
            new EvModel("Tesla Model 3", 75),
            new EvModel("Tesla Model Y", 75),
            new EvModel("Tesla Model S", 100),
            new EvModel("Tesla Model X", 100),
            new EvModel("VW ID.4", 77),
            new EvModel("VW ID.3", 58),
            new EvModel("BMW iX3", 74),
            new EvModel("Audi e-tron", 95),
            new EvModel("Hyundai Ioniq 5", 77),
            new EvModel("Kia EV6", 77),
            new EvModel("Polestar 2", 78),
            new EvModel("Mercedes EQC", 80),
            new EvModel("Nissan Leaf", 62),
            new EvModel("Custom", 75)
    );

    record EvModel(String name, int capacity) {
        @Override
        public String toString() {
            return name;
        }
    }

    public ChargingView(PreservedState preservedState, LiukuriService liukuriService, ObjectMapperService mapperService, SettingsDialog.SettingsState settingsState) {
        this.liukuriService = liukuriService;
        this.mapperService = mapperService;
        this.settingsState = settingsState;

        addClassName("charging-view");

        final var objectMapper = new ObjectMapper();
        WebStorage.getItem(SettingsDialog.margin, item -> {
            if (item == null) return;
            try {
                settingsState.getSettings().setMargin(objectMapper.readValue(item, new TypeReference<Double>() {}));
            } catch (IOException e) {
                log.info("Could not read margin: %s".formatted(e.toString()));
            }
        });
        WebStorage.getItem(SettingsDialog.vat, item -> {
            if (item == null) return;
            try {
                settingsState.getSettings().setVat(objectMapper.readValue(item, new TypeReference<Boolean>() {}));
            } catch (IOException e) {
                log.info("Could not read vat: %s".formatted(e.toString()));
            }
        });

        // Header
        var header = new Div();
        header.addClassName("charging-header");

        var headerTitle = new Div();
        headerTitle.addClassName("charging-header-title");
        var leafIcon = LineAwesomeIcon.LEAF_SOLID.create();
        leafIcon.getElement().getStyle().set("color", "#10b981");
        headerTitle.add(leafIcon);
        var title = new H1("Charging");
        headerTitle.add(title);

        header.add(headerTitle);
        add(header);

        // Car Card
        var carCard = new Div();
        carCard.addClassNames("charging-card", "car-card");

        // Car SVG placeholder
        var carImage = new Div();
        carImage.addClassName("car-image");
        String carSvg = """
            <svg viewBox="0 0 200 100" style="width:100%;height:100%;filter:drop-shadow(0 4px 6px rgba(0,0,0,0.1))">
                <defs>
                    <linearGradient id="s-body" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#0ea5e9"/>
                        <stop offset="100%" stop-color="#0ea5e9" stop-opacity="0.7"/>
                    </linearGradient>
                    <linearGradient id="s-glass" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#e0f4ff"/>
                        <stop offset="100%" stop-color="#a8d8ea"/>
                    </linearGradient>
                </defs>
                <ellipse cx="100" cy="88" rx="90" ry="5" fill="#94a3b8" opacity="0.2"/>
                <path d="M12,72 L20,72 L32,52 L62,35 L88,28 L150,28 L178,38 L192,58 L198,72 L198,80 L10,80 Z" fill="url(#s-body)"/>
                <path d="M68,35 L86,24 L115,24 L115,40 L70,40 Z" fill="url(#s-glass)"/>
                <path d="M120,40 L120,24 L146,24 L168,38 L168,40 Z" fill="url(#s-glass)"/>
                <ellipse cx="58" cy="80" rx="17" ry="17" fill="#334155"/>
                <ellipse cx="58" cy="80" rx="13" ry="13" fill="#475569"/>
                <ellipse cx="58" cy="80" rx="7" ry="7" fill="#64748b"/>
                <ellipse cx="158" cy="80" rx="17" ry="17" fill="#334155"/>
                <ellipse cx="158" cy="80" rx="13" ry="13" fill="#475569"/>
                <ellipse cx="158" cy="80" rx="7" ry="7" fill="#64748b"/>
            </svg>
            """;
        carImage.add(new Html("<div>" + carSvg + "</div>"));
        carCard.add(carImage);

        // Car info row
        var carInfo = new Div();
        carInfo.addClassName("car-info");
        var carDetails = new Div();
        carNameDisplay = new Paragraph("Tesla Model 3");
        carNameDisplay.addClassName("car-name");
        batteryCapacityField = new NumberField();
        batteryCapacityField.setId("batteryCapacityField");
        batteryCapacityField.setVisible(false);
        capacityDisplay = new Paragraph();
        capacityDisplay.addClassName("car-capacity");
        carDetails.add(carNameDisplay, capacityDisplay);
        var chevron = VaadinIcon.CHEVRON_DOWN.create();
        chevron.setSize("1.25rem");
        chevron.setColor("#94a3b8");
        chevron.addClassName("car-chevron");
        carInfo.add(carDetails, chevron);
        carCard.add(carInfo);

        // Car selector dropdown
        carSelectorDropdown = new Div();
        carSelectorDropdown.addClassName("car-selector-dropdown");
        carSelectorDropdown.setVisible(false);
        // Stop click events from bubbling up to the car card
        carSelectorDropdown.getElement().addEventListener("click", e -> {}).addEventData("event.stopPropagation()");

        // Store chevron reference for click handler
        final var finalChevron = chevron;

        // Capacity field (defined first so it can be referenced in combobox listener)
        final var capacityIntegerField = new IntegerField("Capacity");

        var carComboBox = new ComboBox<EvModel>("Select car model");
        carComboBox.setId("carComboBox");
        carComboBox.setItems(EV_MODELS);
        carComboBox.setWidthFull();
        carComboBox.setItemLabelGenerator(EvModel::name);
        // Set default car model
        var defaultCar = EV_MODELS.get(0); // Tesla Model 3
        carComboBox.setValue(defaultCar);
        carComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                carNameDisplay.setText(e.getValue().name());
                batteryCapacityField.setValue((double) e.getValue().capacity());
                capacityDisplay.setText(e.getValue().capacity() + " kWh");
                // Also update the capacity integer field
                capacityIntegerField.setValue(e.getValue().capacity());
                // Save selected car name to WebStorage
                WebStorage.setItem("selectedCarName", e.getValue().name());
            }
        });
        carSelectorDropdown.add(carComboBox);

        // Restore saved car selection from WebStorage
        WebStorage.getItem("selectedCarName", savedCarName -> {
            if (savedCarName != null && !savedCarName.isEmpty()) {
                // Find the EvModel with matching name and set it
                EV_MODELS.stream()
                    .filter(model -> model.name().equals(savedCarName))
                    .findFirst()
                    .ifPresent(model -> {
                        carComboBox.setValue(model);
                        carNameDisplay.setText(model.name());
                    });
            }
        });
        capacityIntegerField.setMin(10);
        capacityIntegerField.setMax(200);
        capacityIntegerField.setStepButtonsVisible(true);
        capacityIntegerField.setWidthFull();
        capacityIntegerField.setSuffixComponent(new Span("kWh"));
        // Set default capacity value
        capacityIntegerField.setValue(defaultCar.capacity());
        capacityIntegerField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                batteryCapacityField.setValue(e.getValue().doubleValue());
                capacityDisplay.setText(e.getValue() + " kWh");
            }
        });
        carSelectorDropdown.add(capacityIntegerField);

        carCard.add(carSelectorDropdown);

        // Click handler for car card
        carCard.addClickListener(e -> {
            carSelectorVisible = !carSelectorVisible;
            carSelectorDropdown.setVisible(carSelectorVisible);
            if (carSelectorVisible) {
                finalChevron.addClassName("expanded");
            } else {
                finalChevron.removeClassName("expanded");
            }
        });

        // Stats row
        var statsRow = new Div();
        statsRow.addClassName("stats-row");

        var currentStat = new Div();
        currentStat.addClassName("stat-item");
        currentSocDisplay = new Span("20%");
        currentSocDisplay.addClassNames("stat-value", "current");
        var currentLabel = new Span("Current");
        currentLabel.addClassName("stat-label");
        currentStat.add(currentSocDisplay, currentLabel);

        var targetStat = new Div();
        targetStat.addClassName("stat-item");
        targetSocDisplay = new Span("80%");
        targetSocDisplay.addClassNames("stat-value", "target");
        var targetLabel = new Span("Target");
        targetLabel.addClassName("stat-label");
        targetStat.add(targetSocDisplay, targetLabel);

        var rangeStat = new Div();
        rangeStat.addClassName("stat-item");
        rangeDisplay = new Span("248km");
        rangeDisplay.addClassNames("stat-value", "range");
        var rangeLabel = new Span("Range");
        rangeLabel.addClassName("stat-label");
        rangeStat.add(rangeDisplay, rangeLabel);

        statsRow.add(currentStat, targetStat, rangeStat);
        carCard.add(statsRow);
        add(carCard);

        // Charge Level Card
        var chargeCard = new Div();
        chargeCard.addClassName("charging-card");

        var chargeHeader = new Div();
        chargeHeader.addClassName("section-header");
        var batteryIcon = VaadinIcon.STORAGE.create();
        chargeHeader.add(batteryIcon, new Span("Charge Level"));
        chargeCard.add(chargeHeader);

        // Interactive Charge bar container with embedded draggable handles
        var chargeBarContainer = new Div();
        chargeBarContainer.addClassName("charge-bar-container");
        chargeBarContainer.addClassName("interactive");

        var chargeBarBackground = new Div();
        chargeBarBackground.addClassName("charge-bar-background");

        chargeBarCurrent = new Div();
        chargeBarCurrent.addClassName("charge-bar-current");
        chargeBarCurrent.getStyle().set("width", "20%");

        chargeBarFill = new Div();
        chargeBarFill.addClassName("charge-bar-fill");
        chargeBarFill.getStyle().set("left", "20%").set("width", "60%");

        chargeMarkerCurrent = new Div();
        chargeMarkerCurrent.addClassNames("charge-marker", "draggable");
        chargeMarkerCurrent.setText("20%");
        chargeMarkerCurrent.getStyle().set("left", "20%");

        chargeMarkerTarget = new Div();
        chargeMarkerTarget.addClassNames("charge-marker", "target", "draggable");
        chargeMarkerTarget.setText("80%");
        chargeMarkerTarget.getStyle().set("left", "80%");

        chargeBarBackground.add(chargeBarCurrent, chargeBarFill);
        chargeBarContainer.add(chargeBarBackground, chargeMarkerCurrent, chargeMarkerTarget);
        chargeCard.add(chargeBarContainer);

        // Hidden fields for SOC (for binder)
        currentSocField = new NumberField();
        currentSocField.setId("currentSocField");
        currentSocField.setMin(0);
        currentSocField.setMax(100);
        currentSocField.setVisible(false);

        targetSocField = new NumberField();
        targetSocField.setId("targetSocField");
        targetSocField.setMin(0);
        targetSocField.setMax(100);
        targetSocField.setVisible(false);

        // Add JavaScript for draggable markers
        // Only sends to server when drag ends to reduce lag
        chargeBarContainer.getElement().executeJs("""
            const container = this;
            const currentMarker = container.querySelector('.charge-marker:not(.target)');
            const targetMarker = container.querySelector('.charge-marker.target');
            const background = container.querySelector('.charge-bar-background');
            const currentBar = container.querySelector('.charge-bar-current');
            const fillBar = container.querySelector('.charge-bar-fill');

            function getPercentFromEvent(e) {
                const rect = background.getBoundingClientRect();
                const clientX = e.touches ? e.touches[0].clientX : e.clientX;
                let percent = ((clientX - rect.left) / rect.width) * 100;
                return Math.max(0, Math.min(100, Math.round(percent)));
            }

            function updateVisuals(currentPercent, targetPercent) {
                currentMarker.style.left = currentPercent + '%';
                currentMarker.textContent = currentPercent + '%';
                targetMarker.style.left = targetPercent + '%';
                targetMarker.textContent = targetPercent + '%';
                currentBar.style.width = currentPercent + '%';
                fillBar.style.left = currentPercent + '%';
                fillBar.style.width = (targetPercent - currentPercent) + '%';
            }

            let currentValue = parseFloat(currentMarker.style.left) || 20;
            let targetValue = parseFloat(targetMarker.style.left) || 80;

            function setupDrag(marker, isTarget) {
                let isDragging = false;
                let dragValue = isTarget ? targetValue : currentValue;

                marker.addEventListener('mousedown', (e) => {
                    isDragging = true;
                    dragValue = isTarget ? targetValue : currentValue;
                    e.preventDefault();
                });

                marker.addEventListener('touchstart', (e) => {
                    isDragging = true;
                    dragValue = isTarget ? targetValue : currentValue;
                }, { passive: true });

                document.addEventListener('mousemove', (e) => {
                    if (!isDragging) return;
                    dragValue = getPercentFromEvent(e);
                    if (isTarget) {
                        updateVisuals(currentValue, dragValue);
                    } else {
                        updateVisuals(dragValue, targetValue);
                    }
                });

                document.addEventListener('touchmove', (e) => {
                    if (!isDragging) return;
                    dragValue = getPercentFromEvent(e);
                    if (isTarget) {
                        updateVisuals(currentValue, dragValue);
                    } else {
                        updateVisuals(dragValue, targetValue);
                    }
                }, { passive: true });

                document.addEventListener('mouseup', () => {
                    if (isDragging) {
                        isDragging = false;
                        if (isTarget) {
                            targetValue = dragValue;
                            $0.$server.updateTargetSoc(dragValue);
                        } else {
                            currentValue = dragValue;
                            $0.$server.updateCurrentSoc(dragValue);
                        }
                    }
                });

                document.addEventListener('touchend', () => {
                    if (isDragging) {
                        isDragging = false;
                        if (isTarget) {
                            targetValue = dragValue;
                            $0.$server.updateTargetSoc(dragValue);
                        } else {
                            currentValue = dragValue;
                            $0.$server.updateCurrentSoc(dragValue);
                        }
                    }
                });
            }

            setupDrag(currentMarker, false);
            setupDrag(targetMarker, true);

            // Also allow clicking on the bar to set values
            background.addEventListener('click', (e) => {
                const percent = getPercentFromEvent(e);
                const midpoint = (currentValue + targetValue) / 2;
                if (percent < midpoint) {
                    currentValue = percent;
                    updateVisuals(currentValue, targetValue);
                    $0.$server.updateCurrentSoc(percent);
                } else {
                    targetValue = percent;
                    updateVisuals(currentValue, targetValue);
                    $0.$server.updateTargetSoc(percent);
                }
            });
            """, getElement());

        add(chargeCard);

        // Speed Card
        var speedCard = new Div();
        speedCard.addClassName("charging-card");

        var speedHeader = new Div();
        speedHeader.addClassName("speed-header");
        var speedLeft = new Div();
        speedLeft.addClassName("section-header");
        speedLeft.getStyle().set("margin-bottom", "0");
        var zapIcon = VaadinIcon.BOLT.create();
        speedLeft.add(zapIcon, new Span("Speed"));
        speedKwDisplay = new Span("11.0 kW");
        speedKwDisplay.addClassName("speed-value");
        speedHeader.add(speedLeft, speedKwDisplay);
        speedCard.add(speedHeader);

        // Amperes field (hidden for binder)
        amperesField = new IntegerField();
        amperesField.setId("amperesField");
        amperesField.setMin(1);
        amperesField.setMax(32);
        amperesField.setVisible(false);

        // Speed slider using HTML range input
        var speedSlider = new Html("""
            <input type="range" class="speed-slider" min="1" max="32" value="16">
            """);
        speedCard.add(speedSlider);

        speedSlider.getElement().executeJs(
            "this.addEventListener('input', function(e) { " +
            "  $0.$server.updateAmperes(parseInt(e.target.value)); " +
            "});", getElement());

        var speedLabels = new Div();
        speedLabels.addClassName("speed-slider-labels");
        speedLabels.add(new Span("1A"));
        amperesDisplay = new Span("16A");
        amperesDisplay.addClassName("current");
        speedLabels.add(amperesDisplay);
        speedLabels.add(new Span("32A"));
        speedCard.add(speedLabels);

        // Advanced section (create before toggle so it can be referenced in click listener)
        advancedSection = new Div();
        advancedSection.addClassName("advanced-section");
        advancedSection.setVisible(false);

        // Advanced toggle
        var advancedToggle = new Button("Advanced");
        advancedToggle.addClassName("advanced-toggle");
        advancedToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        var settingsIcon = VaadinIcon.COG.create();
        settingsIcon.setSize("1rem");
        advancedToggle.setIcon(settingsIcon);
        advancedToggle.addClickListener(e -> {
            advancedVisible = !advancedVisible;
            advancedSection.setVisible(advancedVisible);
        });
        speedCard.add(advancedToggle);

        // Phases
        var phasesRow = new Div();
        phasesRow.addClassName("advanced-row");
        var phasesHeader = new Div();
        phasesHeader.addClassName("advanced-row-header");
        phasesHeader.add(new Span("Phases"));
        var phasesValue = new Span("3");
        phasesHeader.add(phasesValue);
        phasesRow.add(phasesHeader);

        var phaseButtons = new Div();
        phaseButtons.addClassName("phase-buttons");

        phasesField = new IntegerField();
        phasesField.setId("phasesField");
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setVisible(false);

        phase1Button = new Button("1P");
        phase1Button.addClassName("phase-button");
        phase1Button.addClickListener(e -> {
            phasesField.setValue(1);
            updatePhaseButtons();
            phasesValue.setText("1");
        });

        phase2Button = new Button("2P");
        phase2Button.addClassName("phase-button");
        phase2Button.addClickListener(e -> {
            phasesField.setValue(2);
            updatePhaseButtons();
            phasesValue.setText("2");
        });

        phase3Button = new Button("3P");
        phase3Button.addClassName("phase-button");
        phase3Button.addClickListener(e -> {
            phasesField.setValue(3);
            updatePhaseButtons();
            phasesValue.setText("3");
        });

        phaseButtons.add(phase1Button, phase2Button, phase3Button);
        phasesRow.add(phaseButtons);
        advancedSection.add(phasesRow);

        // Voltage with slider
        var voltageRow = new Div();
        voltageRow.addClassName("advanced-row");
        var voltageHeader = new Div();
        voltageHeader.addClassName("advanced-row-header");
        voltageHeader.add(new Span("Voltage"));
        var voltageValue = new Span("230V");
        voltageHeader.add(voltageValue);
        voltageRow.add(voltageHeader);

        voltageField = new IntegerField();
        voltageField.setId("voltageField");
        voltageField.setMin(110);
        voltageField.setMax(400);
        voltageField.setVisible(false);

        var voltageSlider = new Html("""
            <input type="range" class="voltage-slider" min="110" max="400" value="230" step="10">
            """);
        voltageRow.add(voltageSlider);

        voltageSlider.getElement().executeJs(
            "this.addEventListener('input', function(e) { " +
            "  $0.$server.updateVoltage(parseInt(e.target.value)); " +
            "});", getElement());

        voltageField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                voltageValue.setText(e.getValue() + "V");
            }
        });
        advancedSection.add(voltageRow);

        // Loss with slider (removed duplicate label)
        var lossRow = new Div();
        lossRow.addClassName("advanced-row");
        var lossHeader = new Div();
        lossHeader.addClassName("advanced-row-header");
        lossHeader.add(new Span("Loss"));
        var lossValue = new Span("10%");
        lossValue.getStyle().set("color", "#d97706");
        lossHeader.add(lossValue);
        lossRow.add(lossHeader);

        chargingLossField = new NumberField();
        chargingLossField.setId("chargingLossField");
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        chargingLossField.setVisible(false);

        var lossSlider = new Html("""
            <input type="range" class="loss-slider" min="0" max="30" value="10">
            """);
        lossRow.add(lossSlider);

        lossSlider.getElement().executeJs(
            "this.addEventListener('input', function(e) { " +
            "  $0.$server.updateLoss(parseInt(e.target.value)); " +
            "});", getElement());

        chargingLossField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                lossValue.setText(e.getValue().intValue() + "%");
            }
        });

        advancedSection.add(lossRow);
        speedCard.add(advancedSection);

        add(speedCard);

        // Schedule Card
        var scheduleCard = new Div();
        scheduleCard.addClassName("charging-card");

        var scheduleHeader = new Div();
        scheduleHeader.addClassName("section-header");
        var clockIcon = VaadinIcon.CLOCK.create();
        scheduleHeader.add(clockIcon, new Span("Schedule"));
        scheduleCard.add(scheduleHeader);

        var scheduleGrid = new Div();
        scheduleGrid.addClassName("schedule-grid");

        // Start item - contains label on top, time picker below
        var startItem = new Div();
        startItem.addClassName("schedule-item");
        var startLabel = new Span("Start");
        startLabel.addClassName("schedule-label");
        startTimePicker = new TimePicker();
        startTimePicker.setId("startTimePicker");
        startTimePicker.setLocale(Locale.of("fi", "FI"));
        startTimePicker.setStep(Duration.ofMinutes(15));
        startTimePicker.setWidth("100%");
        startTimePicker.getElement().setAttribute("theme", "dark");
        startTimeDisplay = new Span("11:00");
        startTimeDisplay.addClassName("schedule-time-display");
        startTimeDisplay.addClassName("calculated"); // Green highlight for calculated time
        startItem.add(startLabel, startTimePicker);

        // End item - contains label on top, end time display below (matching design)
        var endItem = new Div();
        endItem.addClassName("schedule-item");
        var endLabel = new Span("End");
        endLabel.addClassName("schedule-label");
        endTimeDisplay = new Span("15:25");
        endTimeDisplay.addClassName("schedule-time-display");
        endTimeDisplay.addClassName("calculated"); // Green highlight for calculated time
        endItem.add(endLabel, endTimeDisplay);

        scheduleGrid.add(startItem, endItem);
        scheduleCard.add(scheduleGrid);

        var durationDiv = new Div();
        durationDiv.addClassName("duration-display");
        durationDisplay = new Span("4h 25m");
        durationDisplay.addClassName("duration-value");
        var durationLabel = new Span("duration");
        durationLabel.addClassName("duration-label");
        durationDiv.add(durationDisplay, durationLabel);
        scheduleCard.add(durationDiv);

        // Toggle for Start/End calculation mode
        var scheduleToggleDiv = new Div();
        scheduleToggleDiv.addClassName("schedule-toggle");
        var calculateStartBtn = new Button("Calculate Start");
        calculateStartBtn.addClassName("schedule-toggle-btn");
        var calculateEndBtn = new Button("Calculate End");
        calculateEndBtn.addClassNames("schedule-toggle-btn", "active");

        // Create end time picker
        endTimePicker = new TimePicker();
        endTimePicker.setId("endTimePicker");
        endTimePicker.setLocale(Locale.of("fi", "FI"));
        endTimePicker.setStep(Duration.ofMinutes(15));
        endTimePicker.setWidth("100%");
        endTimePicker.setValue(LocalTime.of(15, 0));
        endTimePicker.getElement().setAttribute("theme", "dark");
        endTimePicker.addValueChangeListener(ev -> {
            if (calculateStartMode) {
                doCalculation();
            }
        });

        // Store references for use in click handlers
        final var finalStartItem = startItem;
        final var finalStartLabel = startLabel;
        final var finalEndItem = endItem;
        final var finalEndLabel = endLabel;

        calculateStartBtn.addClickListener(e -> {
            calculateStartMode = true;
            calculateStartBtn.addClassName("active");
            calculateEndBtn.removeClassName("active");
            // Show start time display, hide picker
            finalStartItem.removeAll();
            finalStartItem.add(finalStartLabel, startTimeDisplay);
            // Show end time picker
            finalEndItem.removeAll();
            finalEndItem.add(finalEndLabel, endTimePicker);
            doCalculation();
        });

        calculateEndBtn.addClickListener(e -> {
            calculateStartMode = false;
            calculateEndBtn.addClassName("active");
            calculateStartBtn.removeClassName("active");
            // Show start time picker
            finalStartItem.removeAll();
            finalStartItem.add(finalStartLabel, startTimePicker);
            // Show end time display
            finalEndItem.removeAll();
            finalEndItem.add(finalEndLabel, endTimeDisplay);
            doCalculation();
        });

        scheduleToggleDiv.add(calculateStartBtn, calculateEndBtn);
        scheduleCard.add(scheduleToggleDiv);

        add(scheduleCard);

        // Summary Card
        var summaryCard = new Div();
        summaryCard.addClassName("summary-card");

        var summaryTitle = new H3("Summary");
        summaryTitle.addClassName("summary-title");
        summaryCard.add(summaryTitle);

        var summaryRows = new Div();
        summaryRows.addClassName("summary-rows");

        // To battery
        var toBatteryRow = new Div();
        toBatteryRow.addClassName("summary-row");
        var toBatteryLabel = new Span("To battery");
        toBatteryLabel.addClassName("label");
        toBatteryDisplay = new Span("45.0 kWh");
        toBatteryDisplay.addClassName("value");
        toBatteryRow.add(toBatteryLabel, toBatteryDisplay);
        summaryRows.add(toBatteryRow);

        // Consumed
        var consumedRow = new Div();
        consumedRow.addClassName("summary-row");
        var consumedLabel = new Span("Consumed");
        consumedLabel.addClassName("label");
        consumedDisplay = new Span("48.9 kWh");
        consumedDisplay.addClassName("value");
        consumedRow.add(consumedLabel, consumedDisplay);
        summaryRows.add(consumedRow);

        // Lost
        var lostRow = new Div();
        lostRow.addClassName("summary-row");
        var lostLabel = new Span("Lost");
        lostLabel.addClassName("label");
        lostDisplay = new Span("3.9 kWh");
        lostDisplay.addClassNames("value", "lost");
        lostRow.add(lostLabel, lostDisplay);
        summaryRows.add(lostRow);

        // Spot price
        var spotRow = new Div();
        spotRow.addClassName("summary-row");
        var spotLabel = new Span("Spot price");
        spotLabel.addClassName("label");
        spotPriceDisplay = new Span("19.08 c/kWh");
        spotPriceDisplay.addClassName("value");
        spotRow.add(spotLabel, spotPriceDisplay);
        summaryRows.add(spotRow);

        // Cost divider
        var costDivider = new Div();
        costDivider.addClassNames("summary-row", "summary-divider", "summary-cost");
        var costLabel = new Span("Cost");
        costLabel.addClassName("label");
        costDisplay = new Span("9.33 €");
        costDisplay.addClassName("value");
        costDivider.add(costLabel, costDisplay);
        summaryRows.add(costDivider);

        summaryCard.add(summaryRows);
        add(summaryCard);

        // Footer
        var footer = new Paragraph("Auto Liukuri");
        footer.addClassName("charging-footer");
        add(footer);

        // Add hidden fields to DOM
        add(batteryCapacityField, currentSocField, targetSocField, amperesField, phasesField, voltageField, chargingLossField);

        // Setup binder
        var chargeBinder = new Binder<Charge>();
        chargeBinder.bind(batteryCapacityField, Charge::getCapacity, Charge::setCapacity);
        chargeBinder.bind(currentSocField, Charge::getCurrentSOC, Charge::setCurrentSOC);
        chargeBinder.bind(targetSocField, Charge::getTargetSOC, Charge::setTargetSOC);
        chargeBinder.bind(amperesField, Charge::getAmperes, Charge::setAmperes);
        chargeBinder.bind(phasesField, Charge::getPhases, Charge::setPhases);
        chargeBinder.bind(voltageField, Charge::getVoltage, Charge::setVoltage);
        chargeBinder.bind(chargingLossField, Charge::getChargingLoss, Charge::setChargingLoss);
        chargeBinder.bind(startTimePicker, Charge::getStartTime, Charge::setStartTime);

        chargeBinder.setBean(preservedState.charge);
        chargeBinder.addValueChangeListener(e -> {
            if (chargeBinder.isValid()) {
                doCalculation();
            }
        });

        // Initialize displays
        capacityDisplay.setText(batteryCapacityField.getValue().intValue() + " kWh");

        // Update phase buttons initial state
        updatePhaseButtons();

        doCalculation();
        readFieldValues();

        // Save field values on change
        batteryCapacityField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(batteryCapacityField);
            capacityDisplay.setText(item.getValue().intValue() + " kWh");
            // Also sync the capacity integer field in dropdown
            if (capacityIntegerField.getValue() == null ||
                !capacityIntegerField.getValue().equals(item.getValue().intValue())) {
                capacityIntegerField.setValue(item.getValue().intValue());
            }
        });
        currentSocField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(currentSocField);
        });
        targetSocField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(targetSocField);
        });
        amperesField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(amperesField);
            // Update slider via JS
            getElement().executeJs("document.querySelector('.speed-slider').value = $0", item.getValue());
        });
        phasesField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(phasesField);
            phasesValue.setText(String.valueOf(item.getValue()));
            updatePhaseButtons();
        });
        voltageField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(voltageField);
            // Update slider via JS
            getElement().executeJs("document.querySelector('.voltage-slider').value = $0", item.getValue());
        });
        chargingLossField.addValueChangeListener(item -> {
            mapperService.saveFieldValue(chargingLossField);
            // Update slider via JS
            getElement().executeJs("document.querySelector('.loss-slider').value = $0", item.getValue().intValue());
        });
        startTimePicker.addValueChangeListener(item -> mapperService.saveFieldValue(startTimePicker));
    }

    // Server-side methods called from JavaScript
    @com.vaadin.flow.component.ClientCallable
    public void updateCurrentSoc(int value) {
        currentSocField.setValue((double) value);
    }

    @com.vaadin.flow.component.ClientCallable
    public void updateTargetSoc(int value) {
        targetSocField.setValue((double) value);
    }

    @com.vaadin.flow.component.ClientCallable
    public void updateAmperes(int value) {
        amperesField.setValue(value);
    }

    @com.vaadin.flow.component.ClientCallable
    public void updateVoltage(int value) {
        voltageField.setValue(value);
    }

    @com.vaadin.flow.component.ClientCallable
    public void updateLoss(int value) {
        chargingLossField.setValue((double) value);
    }

    private void updatePhaseButtons() {
        int phases = phasesField.getValue() != null ? phasesField.getValue() : 3;
        phase1Button.removeClassName("active");
        phase2Button.removeClassName("active");
        phase3Button.removeClassName("active");
        switch (phases) {
            case 1 -> phase1Button.addClassName("active");
            case 2 -> phase2Button.addClassName("active");
            case 3 -> phase3Button.addClassName("active");
        }
    }


    private void doCalculation() {
        double currentSoc = currentSocField.getValue() != null ? currentSocField.getValue() : 20;
        double targetSoc = targetSocField.getValue() != null ? targetSocField.getValue() : 80;
        double capacity = batteryCapacityField.getValue() != null ? batteryCapacityField.getValue() : 75;
        int amperes = amperesField.getValue() != null ? amperesField.getValue() : 16;
        int phases = phasesField.getValue() != null ? phasesField.getValue() : 3;
        int voltage = voltageField.getValue() != null ? voltageField.getValue() : 230;
        double loss = chargingLossField.getValue() != null ? chargingLossField.getValue() : 10;

        // Update displays
        currentSocDisplay.setText((int) currentSoc + "%");
        targetSocDisplay.setText((int) targetSoc + "%");
        amperesDisplay.setText(amperes + "A");

        // Update charge bar
        chargeBarCurrent.getStyle().set("width", currentSoc + "%");
        chargeBarFill.getStyle().set("left", currentSoc + "%").set("width", (targetSoc - currentSoc) + "%");
        chargeMarkerCurrent.setText((int) currentSoc + "%");
        chargeMarkerCurrent.getStyle().set("left", currentSoc + "%");
        chargeMarkerTarget.setText((int) targetSoc + "%");
        chargeMarkerTarget.getStyle().set("left", targetSoc + "%");

        // Calculate
        double socIncrease = targetSoc - currentSoc;
        double capacityIncrease = capacity / 100 * socIncrease;
        double chargingPowerInWatts = amperes * phases * voltage;
        double chargingSpeedMinusLoss = chargingPowerInWatts * ((100 - loss) / 100);
        double chargingTimeHours = capacityIncrease * 1000 / chargingSpeedMinusLoss;
        int chargingTimeSeconds = (int) (chargingTimeHours * 3600);

        // Speed display
        double kW = chargingPowerInWatts / 1000.0;
        speedKwDisplay.setText(String.format("%.1f kW", kW));

        // Duration
        int hours = (int) chargingTimeHours;
        int minutes = (chargingTimeSeconds % 3600) / 60;
        durationDisplay.setText(hours + "h " + minutes + "m");

        // Calculate start/end times based on mode
        LocalTime startTime;
        LocalTime endTime;

        if (calculateStartMode) {
            // User sets end time, calculate start time
            endTime = endTimePicker.getValue() != null ? endTimePicker.getValue() : LocalTime.of(15, 0);
            startTime = endTime.minusSeconds(chargingTimeSeconds);
            startTimeDisplay.setText(String.format("%02d:%02d", startTime.getHour(), startTime.getMinute()));
        } else {
            // User sets start time, calculate end time (default)
            startTime = startTimePicker.getValue() != null ? startTimePicker.getValue() : LocalTime.of(11, 0);
            endTime = startTime.plusSeconds(chargingTimeSeconds);
            endTimeDisplay.setText(String.format("%02d:%02d", endTime.getHour(), endTime.getMinute()));
        }

        // Range at target SOC (assuming 5.5 km/kWh average efficiency)
        // Shows estimated driving distance after charging is complete
        double usableCapacityAtTarget = (targetSoc / 100.0) * capacity;
        int range = (int) (usableCapacityAtTarget * 5.5);
        rangeDisplay.setText(range + "km");

        // Energy calculations
        double electricityConsumed = kW * chargingTimeHours;
        double lostElectricity = electricityConsumed * (loss / 100.0);
        double addedElectricity = electricityConsumed - lostElectricity;

        toBatteryDisplay.setText(String.format("%.1f kWh", addedElectricity));
        consumedDisplay.setText(String.format("%.1f kWh", electricityConsumed));
        lostDisplay.setText(String.format("%.1f kWh", lostElectricity));

        // Cost calculation using LiukuriService
        Instant chargingStartTime = ZonedDateTime.now(fiZoneID)
                .with(startTime)
                .toInstant();

        var consumptionData = mapChargingEventToConsumptionData(kW, chargingStartTime, chargingTimeHours);
        var margin = settingsState.getSettings().getMargin();
        var vat = settingsState.getSettings().getVat();
        var calculationResponse = liukuriService.performCalculation(consumptionData, margin == null ? 0 : margin, vat != null && vat);

        if (calculationResponse != null) {
            double averagePrice = calculationResponse.getAveragePrice();
            spotPriceDisplay.setText(String.format("%.2f c/kWh", averagePrice));
            costDisplay.setText(String.format("%.2f €", calculationResponse.getTotalCost()));
        } else {
            spotPriceDisplay.setText("19.08 c/kWh");
            double cost = electricityConsumed * 19.08 / 100;
            costDisplay.setText(String.format("%.2f €", cost));
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
        WebStorage.getItem(currentSocField.getId().orElseThrow(), item -> mapperService.readValue(item, currentSocField));
        WebStorage.getItem(targetSocField.getId().orElseThrow(), item -> mapperService.readValue(item, targetSocField));
        WebStorage.getItem(amperesField.getId().orElseThrow(), item -> mapperService.readValue(item, amperesField));
        WebStorage.getItem(phasesField.getId().orElseThrow(), item -> mapperService.readValue(item, phasesField));
        WebStorage.getItem(voltageField.getId().orElseThrow(), item -> mapperService.readValue(item, voltageField));
        WebStorage.getItem(chargingLossField.getId().orElseThrow(), item -> mapperService.readValue(item, chargingLossField));
        WebStorage.getItem(startTimePicker.getId().orElseThrow(), item -> mapperService.readLocalTime(item, startTimePicker));
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
        LocalTime startTime;
    }

    @VaadinSessionScope
    @Component
    public static class PreservedState {
        Charge charge = new Charge(
                75,
                20,
                80,
                16,
                3,
                230,
                10,
                LocalTime.of(11, 0)
        );
    }

}
