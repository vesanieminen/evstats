package com.vesanieminen.views.charging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.components.Card;
import com.vesanieminen.components.DualRangeSlider;
import com.vesanieminen.components.Ping;
import com.vesanieminen.components.SingleRangeSlider;
import com.vesanieminen.i18n.T;
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
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "lataus", layout = MainLayout.class)
@RouteAlias(value = "charging", layout = MainLayout.class)
@Slf4j
@PreserveOnRefresh
public class ChargingView extends Main implements com.vaadin.flow.router.HasDynamicTitle {

    @Override
    public String getPageTitle() {
        return T.tr("charging.title");
    }


    // Vehicle section fields
    private final Select<EVModel> vehicleSelect;
    private final NumberField batteryCapacityField;
    private final NumberField consumptionField;
    private final Span currentRangeSpan;
    private final Span rangeAddedSpan;
    private final Span targetRangeSpan;
    private EVModel selectedModel;

    // Charge level fields
    private final DualRangeSlider socSlider;
    private final Span addingKwhSpan;
    private final Span batteryCapacitySpan;

    // Charging speed fields
    private final Span powerValueSpan;
    private final SingleRangeSlider amperesSlider;
    private final IntegerField phasesField;
    private final IntegerField voltageField;
    private final NumberField chargingLossField;
    private static final String AMPERES_STORAGE_KEY = "amperesSlider";
    private static final String VEHICLE_STORAGE_KEY = "vehicleSelect";
    private static final String CAR_IMAGE_STORAGE_KEY = "carImage";
    private static final String BRAND_STORAGE_KEY = "theme.brand";
    private static final int MAX_IMAGE_SIZE_BYTES = 100 * 1024; // 100KB max
    private Div carImageContainer;

    // Schedule fields
    private final DatePicker startDatePicker;
    private final TimePicker startTimePicker;
    private final DatePicker endDatePicker;
    private final TimePicker endTimePicker;
    private final Button scheduleModeFlipBtn;

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

    // Vehicle picker — custom bottom-sheet overlay (NOT a Vaadin Dialog).
    // Vaadin Dialog marks the background aria-hidden, which breaks
    // role-based e2e queries against the page heading; this overlay is a
    // plain Div with manual show/hide via a data-open attribute.
    private Span vehicleNameSpan;
    private Span vehicleMetaSpan;
    private Div vehicleSelectionDiv;
    private Div vehicleSheetOverlay;

    public ChargingView(PreservedState preservedState, LiukuriService liukuriService, ObjectMapperService mapperService, SettingsDialog.SettingsState settingsState) {
        this.preservedState = preservedState;
        this.liukuriService = liukuriService;
        this.mapperService = mapperService;
        this.settingsState = settingsState;

        // Initial model: preserved-state cache (survives intra-session navigation)
        // beats the hardcoded fallback. Without this, navigating back to /charging
        // would render the default vehicle for a frame before localStorage rehydrates.
        selectedModel = preservedState.selectedModel != null
                ? preservedState.selectedModel
                : EVModel.PRESETS.get(0);

        setHeight("var(--fullscreen-height-charging-updated)");

        loadSettingsFromStorage();

        addClassName("charging-view-container");

        // ===== VEHICLE CHIP (own section, per design handoff) =====
        // Exposed as role="button" with aria-label "Change Vehicle" so
        // existing e2e selectors (getByRole) keep finding it. Using Div
        // instead of Button because Vaadin Button's add() is private (no
        // slotted content composition).
        Div vehicleChip = new Div();
        vehicleChip.addClassName("vehicle-chip");
        vehicleChip.getElement().setAttribute("role", "button");
        vehicleChip.getElement().setAttribute("tabindex", "0");
        vehicleChip.getElement().setAttribute("aria-label", T.tr("charging.changeVehicle"));

        carImageContainer = new Div();
        carImageContainer.addClassName("car-svg-container");
        carImageContainer.getElement().setProperty("innerHTML", defaultVehicleHtml());

        vehicleNameSpan = new Span(selectedModel.name());
        vehicleNameSpan.addClassName("vehicle-name");

        vehicleMetaSpan = new Span();
        vehicleMetaSpan.addClassName("vehicle-meta");
        vehicleMetaSpan.setText(selectedModel.capacity() + " kWh");

        Div vehicleChipText = new Div(vehicleNameSpan, vehicleMetaSpan);
        vehicleChipText.addClassName("vehicle-chip-text");

        // Compact edit-icon affordance instead of a "Change ›" text pill —
        // the whole row is already the click target (role="button" with
        // aria-label "Change Vehicle"); the icon is purely a visual hint.
        // Keeps more horizontal space free for long vehicle names.
        Icon editIcon = new Icon(VaadinIcon.EDIT);
        Div changeChip = new Div(editIcon);
        changeChip.addClassName("change-chip");
        changeChip.getElement().setAttribute("aria-hidden", "true");

        vehicleChip.add(carImageContainer, vehicleChipText, changeChip);

        // Section that hosts vehicleSelect + custom fields + image upload —
        // moved into the bottom-sheet panel below.
        vehicleSelectionDiv = new Div();
        vehicleSelectionDiv.addClassName("vehicle-selection");

        // Custom vehicle fields - initialize before vehicleSelect listener
        Div customFieldsDiv = new Div();
        customFieldsDiv.addClassName("vehicle-custom-fields");

        batteryCapacityField = new NumberField(T.tr("charging.batteryCapacity"));
        batteryCapacityField.setId("batteryCapacityField");
        batteryCapacityField.setValue((double) selectedModel.capacity());
        batteryCapacityField.setStepButtonsVisible(true);
        batteryCapacityField.setVisible(false);

        consumptionField = new NumberField(T.tr("charging.consumption"));
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
            preservedState.selectedModel = selectedModel;
            vehicleNameSpan.setText(selectedModel.name());
            vehicleMetaSpan.setText(selectedModel.capacity() + " kWh");
            WebStorage.setItem(VEHICLE_STORAGE_KEY, selectedModel.name());
            applyBrandTheme(selectedModel);
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
            // Close the bottom sheet after a client-driven pick.
            if (e.isFromClient() && vehicleSheetOverlay != null) {
                vehicleSheetOverlay.getElement().setAttribute("data-open", "false");
            }
        });
        // No unconditional applyBrandTheme() at construction time on purpose —
        // the inline boot script in index.html has already painted the right
        // brand from localStorage. Re-applying here would race the boot output
        // with the (possibly stale) constructor-time selectedModel and flash
        // the default brand for one frame. WebStorage hydration in
        // readFieldValues() will fire the listener with the user's pick if it
        // ever differs from preservedState.

        customFieldsDiv.add(batteryCapacityField, consumptionField);

        // Car image change section
        Div imageChangeSection = new Div();
        imageChangeSection.addClassName("image-change-section");

        Span imageLabel = new Span(T.tr("charging.image.title"));
        imageLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        // File upload
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");
        upload.setMaxFileSize(MAX_IMAGE_SIZE_BYTES);
        upload.setDropAllowed(true);
        upload.setWidthFull();

        Div uploadLabel = new Div();
        uploadLabel.setText(T.tr("charging.image.uploadLabel"));
        uploadLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        upload.setDropLabelIcon(new Icon(VaadinIcon.UPLOAD));

        upload.addSucceededListener(event -> {
            try {
                String mimeType = event.getMIMEType();
                InputStream inputStream = buffer.getInputStream();
                byte[] bytes = inputStream.readAllBytes();

                if (bytes.length > MAX_IMAGE_SIZE_BYTES) {
                    Notification.show(T.tr("charging.image.tooLarge"), 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                String base64 = Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:" + mimeType + ";base64," + base64;
                setCarImage(dataUrl);
                WebStorage.setItem(CAR_IMAGE_STORAGE_KEY, dataUrl);
                Notification.show(T.tr("charging.image.updated"), 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IOException ex) {
                Notification.show(T.tr("charging.image.failed"), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFileRejectedListener(event -> {
            Notification.show(event.getErrorMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        // URL input
        TextField imageUrlField = new TextField(T.tr("charging.image.upload"));
        imageUrlField.setWidthFull();
        imageUrlField.setPlaceholder("https://example.com/car.png");
        imageUrlField.setClearButtonVisible(true);

        Button loadUrlBtn = new Button(T.tr("charging.summary.load"), new Icon(VaadinIcon.DOWNLOAD));
        loadUrlBtn.addClickListener(e -> {
            String url = imageUrlField.getValue();
            if (url != null && !url.isBlank()) {
                // Validate URL format
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    Notification.show(T.tr("charging.image.invalidUrl"), 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                setCarImage(url);
                WebStorage.setItem(CAR_IMAGE_STORAGE_KEY, url);
                Notification.show(T.tr("charging.image.updated"), 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                imageUrlField.clear();
            }
        });

        Div urlRow = new Div(imageUrlField, loadUrlBtn);
        urlRow.addClassName("image-url-row");

        // Reset button
        Button resetImageBtn = new Button(T.tr("charging.image.reset"), new Icon(VaadinIcon.REFRESH));
        resetImageBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        resetImageBtn.addClickListener(e -> {
            carImageContainer.getElement().setProperty("innerHTML", defaultVehicleHtml());
            WebStorage.removeItem(CAR_IMAGE_STORAGE_KEY);
            Notification.show(T.tr("charging.image.resetDone"), 2000, Notification.Position.MIDDLE);
        });

        imageChangeSection.add(imageLabel, upload, uploadLabel, urlRow, resetImageBtn);
        vehicleSelectionDiv.add(vehicleSelect, customFieldsDiv, imageChangeSection);

        // ===== BOTTOM-SHEET VEHICLE PICKER (custom overlay, no Vaadin Dialog) =====
        vehicleSheetOverlay = new Div();
        vehicleSheetOverlay.addClassName("vehicle-sheet-overlay");
        vehicleSheetOverlay.getElement().setAttribute("data-open", "false");

        Div sheetBackdrop = new Div();
        sheetBackdrop.addClassName("vehicle-sheet-backdrop");
        sheetBackdrop.addClickListener(e ->
                vehicleSheetOverlay.getElement().setAttribute("data-open", "false"));

        Div sheetPanel = new Div();
        sheetPanel.addClassName("vehicle-sheet");
        // Click on the sheet itself should NOT propagate up to the backdrop
        // (which would dismiss it). Stop propagation in the DOM.
        sheetPanel.getElement().addEventListener("click", e -> {})
                .addEventData("event.stopPropagation()");

        Div sheetHeader = new Div();
        sheetHeader.addClassName("vehicle-sheet-header");
        com.vaadin.flow.component.html.H3 sheetTitle = new com.vaadin.flow.component.html.H3(T.tr("charging.changeVehicle"));
        Button sheetCloseBtn = new Button(new Icon(VaadinIcon.CLOSE));
        sheetCloseBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        sheetCloseBtn.setAriaLabel(T.tr("common.close"));
        sheetCloseBtn.addClickListener(e ->
                vehicleSheetOverlay.getElement().setAttribute("data-open", "false"));
        sheetHeader.add(sheetTitle, sheetCloseBtn);

        sheetPanel.add(sheetHeader, vehicleSelectionDiv);
        vehicleSheetOverlay.add(sheetBackdrop, sheetPanel);

        // Tap on the vehicle chip opens the bottom sheet.
        vehicleChip.addClickListener(e ->
                vehicleSheetOverlay.getElement().setAttribute("data-open", "true"));

        // Add the chip and the sheet as top-level children of the view per
        // the design handoff. The sheet itself is position:fixed so its
        // placement in the source order doesn't affect its on-screen
        // location.
        add(vehicleChip, vehicleSheetOverlay);

        // ===== CHARGE LEVEL CARD =====
        // Stats grid (current % / +km adds / target %) + dual-thumb SoC slider
        // + adding/battery summary, all in their own card per the design.
        Card chargeLevelCard = new Card(new Icon(VaadinIcon.PLUG), T.tr("charging.section.chargeLevel"));

        socSlider = new DualRangeSlider(0, 100, preservedState.charge.getCurrentSOC(), preservedState.charge.getTargetSOC());
        // Labels are intentionally omitted — the stats grid above the slider
        // already shows Current / Target values, so the slider's own labels
        // would be a duplicate.
        socSlider.setLowLabel("");
        socSlider.setHighLabel("");
        socSlider.setWidthFull();
        socSlider.addValueChangeListener(e -> doCalculation());

        Div chargeLevelSummary = new Div();
        chargeLevelSummary.addClassName("charge-level-summary");
        addingKwhSpan = new Span();
        batteryCapacitySpan = new Span();
        batteryCapacitySpan.addClassName(LumoUtility.TextColor.SECONDARY);
        chargeLevelSummary.add(addingKwhSpan, batteryCapacitySpan);

        // Range row (km only — the slider thumbs already show the %s).
        // Current / +Added (accent) / Target on a single line above the
        // adding/battery summary.
        Div rangeRow = new Div();
        rangeRow.addClassName("soc-display");
        Div curCol = new Div();
        curCol.addClassName("soc-column");
        Span curLabel = new Span(T.tr("charging.current"));
        curLabel.addClassName("soc-label");
        currentRangeSpan = new Span("103 km");
        currentRangeSpan.addClassName("soc-range");
        curCol.add(curLabel, currentRangeSpan);

        Div addCol = new Div();
        addCol.addClassNames("soc-column", "center");
        Span addLabel = new Span(T.tr("charging.added"));
        addLabel.addClassName("soc-label");
        rangeAddedSpan = new Span("+155 km");
        rangeAddedSpan.addClassNames("soc-range", "accent");
        addCol.add(addLabel, rangeAddedSpan);

        Div tgtCol = new Div();
        tgtCol.addClassNames("soc-column", "right");
        Span tgtLabel = new Span(T.tr("charging.target"));
        tgtLabel.addClassName("soc-label");
        targetRangeSpan = new Span("258 km");
        targetRangeSpan.addClassName("soc-range");
        tgtCol.add(tgtLabel, targetRangeSpan);

        rangeRow.add(curCol, addCol, tgtCol);

        chargeLevelCard.add(socSlider, rangeRow, chargeLevelSummary);
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
        Span speedLabel = new Span(T.tr("charging.charger.power"));
        speedLabel.addClassNames(LumoUtility.TextColor.SECONDARY, "section-label");
        headerLeft.add(zapIcon, speedLabel);

        powerValueSpan = new Span("11.0 kW");
        powerValueSpan.addClassName("power-value");

        speedHeader.add(headerLeft, powerValueSpan);

        // Amperage slider
        // Range 1..32: the design handoff spec'd 0..32 (default 16 A
        // mid-track), but 0 A means "not charging" which makes no physical
        // sense as a slider value — keep the floor at 1 A.
        amperesSlider = new SingleRangeSlider(1, 32, preservedState.charge.getAmperes());
        amperesSlider.setUnit("A");
        amperesSlider.setWidthFull();
        amperesSlider.addValueChangeListener(e -> {
            preservedState.charge.setAmperes(e.getValue());
            WebStorage.setItem(AMPERES_STORAGE_KEY, String.valueOf(e.getValue()));
            doCalculation();
        });

        // Advanced section
        Details advancedDetails = new Details(T.tr("charging.section.advanced"));
        advancedDetails.addClassName("advanced-toggle");

        Div advancedSection = new Div();
        advancedSection.addClassName("advanced-section");

        phasesField = new IntegerField(T.tr("charging.charger.phases"));
        phasesField.setId("phasesField");
        phasesField.setMin(1);
        phasesField.setMax(3);
        phasesField.setValue(preservedState.charge.getPhases());
        phasesField.setStepButtonsVisible(true);

        voltageField = new IntegerField(T.tr("charging.charger.voltage"));
        voltageField.setId("voltageField");
        voltageField.setSuffixComponent(new Span("V"));
        voltageField.setMin(1);
        voltageField.setMax(1000);
        voltageField.setValue(preservedState.charge.getVoltage());
        voltageField.setStepButtonsVisible(true);

        chargingLossField = new NumberField(T.tr("charging.charger.loss"));
        chargingLossField.setId("chargingLossField");
        chargingLossField.setSuffixComponent(new Span("%"));
        chargingLossField.setMin(0);
        chargingLossField.setMax(99);
        chargingLossField.setValue(preservedState.charge.getChargingLoss());
        chargingLossField.setStepButtonsVisible(true);
        chargingLossField.addClassName("full-width");

        advancedSection.add(phasesField, voltageField, chargingLossField);
        advancedDetails.add(advancedSection);

        chargingSpeedCard.add(speedHeader, amperesSlider, advancedDetails);
        add(chargingSpeedCard);

        // ===== SCHEDULE CARD =====
        // Build a custom header so we can include the inline schedule-mode flip
        // button on the right (issue #31). The Card class doesn't expose a
        // right-slot; we replicate Card's outer styling on a plain Div.
        Div scheduleCard = new Div();
        scheduleCard.addClassName("charging-card");

        Div scheduleHeader = new Div();
        scheduleHeader.addClassName("charging-card-header");
        scheduleHeader.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        Div scheduleHeaderLeft = new Div();
        scheduleHeaderLeft.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("gap", "calc(var(--cv-label) * 0.5)");
        Icon calendarIcon = new Icon(VaadinIcon.CALENDAR);
        scheduleHeaderLeft.add(calendarIcon, new Span(T.tr("charging.section.schedule")));

        Span scheduleMode = new Span();
        scheduleMode.addClassName("schedule-mode");
        Span solvingForLabel = new Span(T.tr("charging.solvingFor") + " ");
        scheduleModeFlipBtn = new Button(T.tr("charging.solvingForEnd") + " ↻");
        scheduleModeFlipBtn.addClassName("schedule-mode-flip");
        scheduleModeFlipBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        // Initial aria-label describes the action a click would perform — i.e.
        // "Calculate Start" when we currently solve for end. Keeping this in
        // sync with setCalculationMode lets existing role=button tests still
        // resolve via accessible name.
        scheduleModeFlipBtn.setAriaLabel(T.tr("charging.calculateStart"));
        scheduleModeFlipBtn.addClickListener(e -> {
            CalculationTarget current = preservedState.charge.getCalculationTarget();
            setCalculationMode(current == CalculationTarget.CHARGING_END
                    ? CalculationTarget.CHARGING_START
                    : CalculationTarget.CHARGING_END);
        });
        scheduleMode.add(solvingForLabel, scheduleModeFlipBtn);

        scheduleHeader.add(scheduleHeaderLeft, scheduleMode);
        scheduleCard.add(scheduleHeader);

        // Each row holds a date + time picker side by side. Only the row
        // currently being solved-from is visible — the other becomes the
        // computed end of the calculation, displayed via the Duration row.
        final var datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setFirstDayOfWeek(1);
        datePickerI18n.setDateFormat("dd.MM.yyyy");

        startDatePicker = new DatePicker(T.tr("charging.startDate"));
        startDatePicker.setId("startDatePicker");
        startDatePicker.setI18n(datePickerI18n);
        startDatePicker.setLocale(Locale.of("fi", "FI"));
        startDatePicker.setValue(preservedState.charge.getStartTime().toLocalDate());

        endDatePicker = new DatePicker(T.tr("charging.endDate"));
        endDatePicker.setId("endDatePicker");
        endDatePicker.setI18n(datePickerI18n);
        endDatePicker.setLocale(Locale.of("fi", "FI"));
        endDatePicker.setValue(preservedState.charge.getStartTime().toLocalDate());
        endDatePicker.setReadOnly(true);

        startTimePicker = new TimePicker(T.tr("charging.startTime"));
        startTimePicker.setId("startTimePicker");
        startTimePicker.setStep(Duration.ofMinutes(15));
        startTimePicker.setLocale(Locale.of("fi", "FI"));
        startTimePicker.setValue(preservedState.charge.getStartTime().toLocalTime());

        endTimePicker = new TimePicker(T.tr("charging.endTime"));
        endTimePicker.setId("endTimePicker");
        endTimePicker.setStep(Duration.ofMinutes(15));
        endTimePicker.setLocale(Locale.of("fi", "FI"));
        endTimePicker.setReadOnly(true);

        // Schedule layout per design: [Start group] → [End group].
        // Each group holds its date + time picker; the group's date/time
        // sit side-by-side on wide screens and stack when there isn't
        // enough room. The arrow lives between the two groups and is
        // vertically centred against them. The derived pickers stay
        // read-only so the user can see the computed end time.
        Div startGroup = new Div(startDatePicker, startTimePicker);
        startGroup.addClassName("schedule-pair");

        Div endGroup = new Div(endDatePicker, endTimePicker);
        endGroup.addClassName("schedule-pair");

        Span scheduleArrow = new Span("→");
        scheduleArrow.addClassName("schedule-arrow");
        scheduleArrow.getElement().setAttribute("aria-hidden", "true");

        Div scheduleGrid = new Div();
        scheduleGrid.addClassName("schedule-grid");
        scheduleGrid.add(startGroup, scheduleArrow, endGroup);

        // Duration row (moved from Summary card per issue #31)
        Div durationRow = new Div();
        durationRow.addClassName("duration-row");
        Span durationLabelSpan = new Span();
        durationLabelSpan.addClassName("label");
        Icon clockIcon = new Icon(VaadinIcon.CLOCK);
        clockIcon.setSize("14px");
        durationLabelSpan.add(clockIcon, new Span(T.tr("charging.summary.duration")));
        durationValueSpan = new Span();
        durationValueSpan.addClassName("value");
        durationRow.add(durationLabelSpan, durationValueSpan);

        scheduleCard.add(scheduleGrid, durationRow);
        add(scheduleCard);

        // ===== CHARGING SUMMARY CARD =====
        Card summaryCard = new Card(new Icon(VaadinIcon.EURO), T.tr("charging.section.summary"));

        Div summaryRows = new Div();
        summaryRows.addClassName("summary-rows");

        // Energy consumed row
        Div energyConsumedRow = new Div();
        energyConsumedRow.addClassName("summary-row");
        Span energyConsumedLabel = new Span(T.tr("charging.summary.energy"));
        energyConsumedLabel.addClassName("label");
        energyConsumedValueSpan = new Span();
        energyConsumedValueSpan.addClassName("value");
        energyConsumedRow.add(energyConsumedLabel, energyConsumedValueSpan);
        summaryRows.add(energyConsumedRow);

        // Added to battery row
        Div addedToBatteryRow = new Div();
        addedToBatteryRow.addClassName("summary-row");
        Span addedToBatteryLabel = new Span(T.tr("charging.summary.added"));
        addedToBatteryLabel.addClassName("label");
        addedToBatteryValueSpan = new Span();
        addedToBatteryValueSpan.addClassName("value");
        addedToBatteryRow.add(addedToBatteryLabel, addedToBatteryValueSpan);
        summaryRows.add(addedToBatteryRow);

        // Lost to heat row
        Div lostToHeatRow = new Div();
        lostToHeatRow.addClassName("summary-row");
        Span lostToHeatLabel = new Span(T.tr("charging.summary.lost"));
        lostToHeatLabel.addClassName("label");
        lostToHeatValueSpan = new Span();
        lostToHeatValueSpan.addClassName("value");
        lostToHeatRow.add(lostToHeatLabel, lostToHeatValueSpan);
        summaryRows.add(lostToHeatRow);

        // Spot price row
        Div spotPriceRow = new Div();
        spotPriceRow.addClassName("summary-row");
        Span spotPriceLabel = new Span(T.tr("charging.summary.spot"));
        spotPriceLabel.addClassName("label");
        spotPriceValueSpan = new Span();
        spotPriceValueSpan.addClassName("value");
        spotAveragePing = new Ping(T.tr("charging.summary.price"));
        Div spotValueDiv = new Div(spotPriceValueSpan, spotAveragePing);
        spotValueDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.AlignItems.CENTER);
        spotPriceRow.add(spotPriceLabel, spotValueDiv);
        summaryRows.add(spotPriceRow);

        // Total cost row
        Div totalCostRow = new Div();
        totalCostRow.addClassNames("summary-row", "total-cost-row");
        Span totalCostLabel = new Span(T.tr("charging.summary.total"));
        totalCostLabel.addClassName("label");
        totalCostValueSpan = new Span("0.00 €");
        totalCostValueSpan.addClassName("value");
        electricityCostPing = new Ping(T.tr("charging.summary.cost"));
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
        // Spot prices are only known up to tomorrow in Finland — clamp both
        // start and end pickers to the LiukuriService's valid range so users
        // can't schedule into a date for which we have no price data. End was
        // unconstrained on main because it used to be permanently read-only;
        // it's now editable in CHARGING_START mode and needs the same bounds.
        final var calculationRange = liukuriService.getValidCalculationRange();
        final var start = Instant.ofEpochMilli(calculationRange.getStart());
        final var end = Instant.ofEpochMilli(calculationRange.getEnd());
        final var minDate = start.atZone(fiZoneID).toLocalDate();
        final var maxDate = end.atZone(fiZoneID).toLocalDate();
        startDatePicker.setMin(minDate);
        startDatePicker.setMax(maxDate);
        startTimePicker.setMin(LocalTime.of(0, 0));
        startTimePicker.setMax(LocalTime.of(23, 45));
        endDatePicker.setMin(minDate);
        endDatePicker.setMax(maxDate);
        endTimePicker.setMin(LocalTime.of(0, 0));
        endTimePicker.setMax(LocalTime.of(23, 45));

        readFieldValues();
        doCalculation();
    }

    private void setCalculationMode(CalculationTarget mode) {
        preservedState.charge.setCalculationTarget(mode);
        if (mode == CalculationTarget.CHARGING_END) {
            scheduleModeFlipBtn.setText(T.tr("charging.solvingForEnd") + " ↻");
            scheduleModeFlipBtn.setAriaLabel(T.tr("charging.calculateStart"));
            startDatePicker.setReadOnly(false);
            startTimePicker.setReadOnly(false);
            endDatePicker.setReadOnly(true);
            endTimePicker.setReadOnly(true);
        } else {
            scheduleModeFlipBtn.setText(T.tr("charging.solvingForStart") + " ↻");
            scheduleModeFlipBtn.setAriaLabel(T.tr("charging.calculateEnd"));
            startDatePicker.setReadOnly(true);
            startTimePicker.setReadOnly(true);
            endDatePicker.setReadOnly(false);
            endTimePicker.setReadOnly(false);
        }
        doCalculation();
    }

    private void setCarImage(String imageSource) {
        if (imageSource.startsWith("data:") || imageSource.startsWith("http")) {
            // It's a data URL or external URL - use an img tag
            String imgHtml = "<img src=\"" + imageSource.replace("\"", "&quot;") + "\" " +
                    "style=\"width: 180px; height: 80px; object-fit: contain; display: block; margin: 0 auto;\" " +
                    "alt=\"Vehicle\" onerror=\"this.style.display='none'\" />";
            carImageContainer.getElement().setProperty("innerHTML", imgHtml);
        } else {
            // Fallback to default SVG
            carImageContainer.getElement().setProperty("innerHTML", defaultVehicleHtml());
        }
    }

    private String defaultVehicleHtml() {
        // Default vehicle thumbnail used when no custom user image is set.
        return """
                <img src="themes/evstats/images/ev.png" \
                     alt="Default vehicle" \
                     style="width: 180px; height: 80px; object-fit: contain; \
                            object-position: center; display: block; \
                            margin: 0 auto;" \
                     onerror="this.style.display='none'" />
                """;
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
        int amperes = amperesSlider.getValue();
        int phases = phasesField.getValue() != null ? phasesField.getValue() : 3;
        int voltage = voltageField.getValue() != null ? voltageField.getValue() : 230;
        double chargingLoss = chargingLossField.getValue() != null ? chargingLossField.getValue() : 10;

        // Update preserved state
        preservedState.charge.setCurrentSOC(currentSoc);
        preservedState.charge.setTargetSOC(targetSoc);
        preservedState.charge.setCapacity(capacity);

        // Range row (km only — slider thumbs cover the percentages)
        int currentRange = (int) Math.round((capacity * currentSoc / 100.0) / consumption * 100);
        int targetRange = (int) Math.round((capacity * targetSoc / 100.0) / consumption * 100);
        int rangeAdded = targetRange - currentRange;
        currentRangeSpan.setText(currentRange + " km");
        targetRangeSpan.setText(targetRange + " km");
        rangeAddedSpan.setText("+" + rangeAdded + " km");

        // Calculate energy
        double socIncrease = targetSoc - currentSoc;
        double capacityIncrease = capacity / 100 * socIncrease;

        Span boldKwh = new Span(String.format("%.1f kWh", capacityIncrease));
        boldKwh.getStyle().set("font-weight", "600");
        addingKwhSpan.removeAll();
        addingKwhSpan.add(new Span(T.tr("charging.adding") + " "), boldKwh);
        batteryCapacitySpan.setText(T.tr("charging.batteryShort", String.format("%.0f", capacity)));

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
        WebStorage.getItem(AMPERES_STORAGE_KEY, item -> {
            if (item != null && !item.isEmpty()) {
                try {
                    int amperes = Integer.parseInt(item);
                    amperesSlider.setValue(amperes);
                    preservedState.charge.setAmperes(amperes);
                    doCalculation();
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
        });
        WebStorage.getItem(VEHICLE_STORAGE_KEY, item -> {
            if (item != null && !item.isEmpty()) {
                // Find the matching EVModel by name
                EVModel.PRESETS.stream()
                    .filter(model -> model.name().equals(item))
                    .findFirst()
                    .ifPresent(model -> {
                        selectedModel = model;
                        vehicleSelect.setValue(model);
                    });
            }
        });
        WebStorage.getItem(CAR_IMAGE_STORAGE_KEY, item -> {
            if (item != null && !item.isEmpty()) {
                setCarImage(item);
            }
        });
    }

    private void applyBrandTheme(EVModel model) {
        String slug = Brand.from(model).cssClass();
        // The brand class lives on <html> so the palette covers AppLayout chrome
        // and any view, not just /charging. Persist alongside theme.preference so
        // the inline boot script in index.html can re-apply it before first paint.
        UI.getCurrent().getPage().executeJs(
                "window.applyBrandClass && window.applyBrandClass($0);", slug);
        WebStorage.setItem(BRAND_STORAGE_KEY, slug);
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
        // Cached so navigating back to /charging within the session re-uses the
        // user's last pick instead of flashing the default vehicle while
        // localStorage rehydrates.
        EVModel selectedModel;
    }
}
