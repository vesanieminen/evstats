package com.vesanieminen.views;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.model.UsedEvSnapshot;
import com.vesanieminen.services.SettingsService;
import com.vesanieminen.services.UsedEvListingsService;
import com.vesanieminen.services.UsedEvSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route("admin")
@PageTitle("Admin")
public class AdminView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AdminView.class);

    private static final ZoneId HELSINKI = ZoneId.of("Europe/Helsinki");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").withZone(HELSINKI);

    private final UsedEvListingsService service;
    private final UsedEvSnapshotRepository repository;
    private final SettingsService settingsService;
    private final String adminToken;

    private final Span lastSnapshot = new Span();
    private final Grid<UsedEvSnapshot> grid = new Grid<>(UsedEvSnapshot.class, false);

    public AdminView(UsedEvListingsService service,
                     UsedEvSnapshotRepository repository,
                     SettingsService settingsService,
                     @Value("${admin.token:}") String adminToken) {
        this.service = service;
        this.repository = repository;
        this.settingsService = settingsService;
        this.adminToken = adminToken;

        setMaxWidth("720px");
        setPadding(true);
        setSpacing(true);
        getStyle().set("margin", "0 auto");

        if (adminToken == null || adminToken.isBlank()) {
            renderDisabled();
        } else {
            renderLogin();
        }
    }

    private void renderDisabled() {
        removeAll();
        add(new H2("Admin"));
        Paragraph notice = new Paragraph(
                "Admin actions are disabled because the ADMIN_TOKEN environment variable is not set.");
        notice.addClassNames(LumoUtility.TextColor.SECONDARY);
        add(notice);
    }

    private void renderLogin() {
        removeAll();
        add(new H2("Admin"));

        PasswordField password = new PasswordField("Admin password");
        password.setWidthFull();
        password.setAutofocus(true);

        Button unlock = new Button("Unlock", e -> {
            if (matches(password.getValue(), adminToken)) {
                renderPanel();
            } else {
                Notification n = Notification.show("Wrong password.", 3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                password.clear();
                password.focus();
            }
        });
        unlock.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        unlock.addClickShortcut(Key.ENTER);

        add(password, unlock);
    }

    private void renderPanel() {
        removeAll();

        HorizontalLayout header = new HorizontalLayout(new H2("Admin"));
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        Button lock = new Button("Lock", new Icon(VaadinIcon.LOCK), e -> renderLogin());
        lock.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        header.add(lock);
        add(header);

        lastSnapshot.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        Button refresh = new Button("Fetch fresh snapshot now", new Icon(VaadinIcon.REFRESH), e -> handleRefresh());
        refresh.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Checkbox autoFetchToggle = new Checkbox("Automatic hourly fetching");
        autoFetchToggle.setValue(settingsService.isScheduledFetchEnabled());
        autoFetchToggle.addValueChangeListener(e -> {
            settingsService.setScheduledFetchEnabled(e.getValue());
            Notification n = Notification.show(
                    Boolean.TRUE.equals(e.getValue())
                            ? "Automatic fetching enabled."
                            : "Automatic fetching disabled.",
                    2500, Notification.Position.BOTTOM_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        HorizontalLayout fetchControls = new HorizontalLayout(refresh, autoFetchToggle);
        fetchControls.setAlignItems(Alignment.CENTER);

        add(lastSnapshot, fetchControls);

        add(new H3("Snapshots"));

        HorizontalLayout actions = new HorizontalLayout();
        actions.setAlignItems(Alignment.END);
        actions.setSpacing(true);

        Button addRow = new Button("Add data point", new Icon(VaadinIcon.PLUS), e -> openEditor(null));
        addRow.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Anchor downloadCsv = new Anchor(
                new StreamResource("used-ev-snapshots.csv", () -> new ByteArrayInputStream(buildCsv())),
                "");
        downloadCsv.getElement().setAttribute("download", true);
        Button downloadButton = new Button("Download CSV", new Icon(VaadinIcon.DOWNLOAD));
        downloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        downloadCsv.add(downloadButton);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("text/csv", ".csv");
        upload.setMaxFiles(1);
        Button uploadButton = new Button("Upload CSV", new Icon(VaadinIcon.UPLOAD));
        uploadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        upload.setUploadButton(uploadButton);
        upload.setDropAllowed(false);
        upload.addSucceededListener(e -> handleImport(buffer, upload));

        actions.add(addRow, downloadCsv, upload);
        add(actions);

        configureGrid();
        add(grid);

        refreshGrid();
        refreshLastSnapshotLabel();
    }

    private byte[] buildCsv() {
        StringBuilder sb = new StringBuilder("fetchedAt,count\n");
        for (UsedEvSnapshot s : repository.findAllByOrderByFetchedAtDesc()) {
            sb.append(s.getFetchedAt().toString()).append(',').append(s.getCount()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void handleImport(MemoryBuffer buffer, Upload upload) {
        int imported = 0;
        int duplicates = 0;
        List<String> skipped = new ArrayList<>();
        java.util.Set<Instant> seenInBatch = new java.util.HashSet<>();
        try (InputStream in = buffer.getInputStream();
             CSVReader reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String[] row;
            int lineNumber = 0;
            while ((row = reader.readNext()) != null) {
                lineNumber++;
                if (lineNumber == 1 && row.length >= 2
                        && row[0].trim().equalsIgnoreCase("fetchedAt")
                        && row[1].trim().equalsIgnoreCase("count")) {
                    continue;
                }
                if (row.length < 2 || row[0].isBlank() || row[1].isBlank()) {
                    skipped.add("line " + lineNumber + ": missing fields");
                    continue;
                }
                try {
                    Instant fetchedAt = Instant.parse(row[0].trim());
                    int count = Integer.parseInt(row[1].trim());
                    if (count < 0) {
                        skipped.add("line " + lineNumber + ": negative count");
                        continue;
                    }
                    if (!seenInBatch.add(fetchedAt) || repository.existsByFetchedAt(fetchedAt)) {
                        duplicates++;
                        continue;
                    }
                    repository.save(new UsedEvSnapshot(fetchedAt, count));
                    imported++;
                } catch (Exception parseEx) {
                    skipped.add("line " + lineNumber + ": " + parseEx.getClass().getSimpleName());
                }
            }
        } catch (IOException | CsvValidationException ex) {
            Notification n = Notification.show(
                    "Import failed: " + ex.getClass().getSimpleName(),
                    4000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            upload.clearFileList();
            return;
        }
        upload.clearFileList();
        refreshGrid();
        refreshLastSnapshotLabel();

        StringBuilder summary = new StringBuilder("Imported ")
                .append(imported).append(" row").append(imported == 1 ? "" : "s").append('.');
        if (duplicates > 0) {
            summary.append(" Skipped ").append(duplicates).append(" duplicate")
                    .append(duplicates == 1 ? "" : "s").append('.');
        }
        if (!skipped.isEmpty()) {
            summary.append(" Skipped ").append(skipped.size()).append(": ")
                    .append(String.join("; ", skipped));
        }
        Notification n = Notification.show(summary.toString(), 5000, Notification.Position.BOTTOM_CENTER);
        boolean hadIssues = !skipped.isEmpty() || duplicates > 0;
        n.addThemeVariants(hadIssues ? NotificationVariant.LUMO_WARNING : NotificationVariant.LUMO_SUCCESS);
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.addColumn(s -> TIMESTAMP_FORMAT.format(s.getFetchedAt()))
                .setHeader("Fetched at").setAutoWidth(true);
        grid.addColumn(UsedEvSnapshot::getCount).setHeader("Count").setAutoWidth(true);
        grid.addComponentColumn(snapshot -> {
            Button edit = new Button(new Icon(VaadinIcon.EDIT), e -> openEditor(snapshot));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
            edit.getElement().setAttribute("aria-label", "Edit");
            Button delete = new Button(new Icon(VaadinIcon.TRASH), e -> confirmDelete(snapshot));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            delete.getElement().setAttribute("aria-label", "Delete");
            return new HorizontalLayout(edit, delete);
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.setAllRowsVisible(false);
        grid.setHeight("420px");
    }

    private void refreshGrid() {
        grid.setItems(repository.findAllByOrderByFetchedAtDesc());
    }

    private void refreshLastSnapshotLabel() {
        Optional<UsedEvSnapshot> latest = repository.findFirstByOrderByFetchedAtDesc();
        if (latest.isEmpty()) {
            lastSnapshot.setText("No snapshots yet.");
        } else {
            UsedEvSnapshot snapshot = latest.get();
            lastSnapshot.setText("Last snapshot: " + snapshot.getCount()
                    + " listings on " + TIMESTAMP_FORMAT.format(snapshot.getFetchedAt()));
        }
    }

    private void handleRefresh() {
        try {
            UsedEvSnapshot saved = service.fetchAndPersist();
            refreshGrid();
            refreshLastSnapshotLabel();
            Notification n = Notification.show(
                    "Persisted " + saved.getCount() + " listings.",
                    3000, Notification.Position.BOTTOM_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            log.warn("Admin-triggered refresh failed.", ex);
            String detail = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
            Notification n = Notification.show(
                    "Refresh failed (" + ex.getClass().getSimpleName() + "): " + detail,
                    8000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openEditor(UsedEvSnapshot existing) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(existing == null ? "Add data point" : "Edit data point");

        DateTimePicker timePicker = new DateTimePicker("Fetched at");
        timePicker.setStep(java.time.Duration.ofMinutes(1));
        timePicker.setWidthFull();

        IntegerField countField = new IntegerField("Count");
        countField.setStepButtonsVisible(true);
        countField.setMin(0);
        countField.setWidthFull();

        if (existing != null) {
            timePicker.setValue(LocalDateTime.ofInstant(existing.getFetchedAt(), HELSINKI));
            countField.setValue(existing.getCount());
        } else {
            timePicker.setValue(LocalDateTime.now(HELSINKI).withSecond(0).withNano(0));
        }

        VerticalLayout form = new VerticalLayout(timePicker, countField);
        form.setPadding(false);
        form.setSpacing(true);
        dlg.add(form);

        Button cancel = new Button("Cancel", e -> dlg.close());
        Button save = new Button("Save", e -> {
            LocalDateTime ldt = timePicker.getValue();
            Integer count = countField.getValue();
            if (ldt == null || count == null || count < 0) {
                Notification n = Notification.show("Both fields are required.", 3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            Instant fetchedAt = ldt.atZone(HELSINKI).toInstant();
            if (existing == null) {
                repository.save(new UsedEvSnapshot(fetchedAt, count));
            } else {
                existing.setFetchedAt(fetchedAt);
                existing.setCount(count);
                repository.save(existing);
            }
            dlg.close();
            refreshGrid();
            refreshLastSnapshotLabel();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dlg.getFooter().add(cancel, save);

        dlg.open();
        timePicker.focus();
    }

    private void confirmDelete(UsedEvSnapshot snapshot) {
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Delete snapshot?");
        confirm.setText("Delete the snapshot from " + TIMESTAMP_FORMAT.format(snapshot.getFetchedAt())
                + " (" + snapshot.getCount() + " listings)? This cannot be undone.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            repository.deleteById(snapshot.getId());
            refreshGrid();
            refreshLastSnapshotLabel();
        });
        confirm.open();
    }

    private static boolean matches(String provided, String expected) {
        if (provided == null) {
            return false;
        }
        byte[] a = provided.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
