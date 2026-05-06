package com.vesanieminen.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

public class TraficomInspectionService {

    private static final Logger log = LoggerFactory.getLogger(TraficomInspectionService.class);

    private static final String CSV_FILENAME = "data/traficom-katsastus-2025.csv";
    private static final String TOTALS_YEAR = "Vuodet yhteensä";
    private static final String TOTALS_MAKE = "Merkit yhteensä";
    private static final String TOTALS_MODEL = "Mallit yhteensä";

    public enum Powertrain { BEV, ICE_OR_OTHER }

    public record InspectionRow(
            Integer cohortYear,
            String make,
            String model,
            int inspections,
            double failPct,
            int avgKm,
            int medianKm,
            List<String> topDefects
    ) {
    }

    private static volatile List<InspectionRow> cached;

    private static final Map<String, Set<String>> BEV_MODELS = Map.ofEntries(
            Map.entry("Tesla", Set.of("MODEL S", "MODEL X", "MODEL 3", "MODEL Y")),
            Map.entry("Polestar", Set.of("2")),
            Map.entry("Audi", Set.of("E-TRON", "Q4")),
            Map.entry("BMW", Set.of("IX", "IX3")),
            Map.entry("Mercedes-Benz", Set.of("EQA", "EQC")),
            Map.entry("Volkswagen", Set.of("ID.3", "ID.4")),
            Map.entry("Hyundai", Set.of("IONIQ5")),
            Map.entry("Kia", Set.of("EV6")),
            Map.entry("Nissan", Set.of("LEAF")),
            Map.entry("Renault", Set.of("ZOE")),
            Map.entry("Skoda", Set.of("ENYAQ")),
            Map.entry("Porsche", Set.of("TAYCAN")),
            Map.entry("Jaguar", Set.of("I-PACE"))
    );

    public static List<InspectionRow> loadAll() {
        if (cached != null) {
            return cached;
        }
        synchronized (TraficomInspectionService.class) {
            if (cached == null) {
                cached = doLoad();
            }
            return cached;
        }
    }

    private static List<InspectionRow> doLoad() {
        try (InputStream in = TraficomInspectionService.class.getClassLoader().getResourceAsStream(CSV_FILENAME);
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(in, "missing " + CSV_FILENAME), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withSkipLines(2)
                    .withCSVParser(parser)
                    .build();

            List<InspectionRow> rows = new ArrayList<>(3500);
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < 7) {
                    continue;
                }
                Integer year = TOTALS_YEAR.equals(line[0]) ? null : Integer.valueOf(line[0]);
                String make = line[1];
                String model = line[2];
                int inspections = Integer.parseInt(line[3]);
                double failPct = Double.parseDouble(line[4]);
                int avgKm = parseIntOrZero(line[5]);
                int medianKm = parseIntOrZero(line[6]);
                List<String> defects = new ArrayList<>(3);
                for (int i = 7; i < 10 && i < line.length; i++) {
                    if (line[i] != null && !line[i].isBlank()) {
                        defects.add(line[i]);
                    }
                }
                rows.add(new InspectionRow(year, make, model, inspections, failPct, avgKm, medianKm, List.copyOf(defects)));
            }
            return List.copyOf(rows);
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("Failed to load Traficom inspection data", e);
        }
    }

    private static int parseIntOrZero(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Powertrain classify(String make, String model) {
        if (make == null || model == null) {
            return Powertrain.ICE_OR_OTHER;
        }
        String upperModel = model.toUpperCase(Locale.ROOT).trim();
        Set<String> bev = BEV_MODELS.get(make);
        if (bev != null && bev.contains(upperModel)) {
            return Powertrain.BEV;
        }
        return Powertrain.ICE_OR_OTHER;
    }

    /**
     * Per-model rows for the league table (BEV-only).
     *
     * @param cohortYear when null, returns the "Vuodet yhteensä" totals per model;
     *                   otherwise returns rows for that specific cohort year.
     */
    public static List<InspectionRow> leagueRows(Integer cohortYear) {
        return loadAll().stream()
                .filter(r -> Objects.equals(r.cohortYear(), cohortYear))
                .filter(r -> !TOTALS_MAKE.equals(r.make()) && !TOTALS_MODEL.equals(r.model()))
                .filter(r -> classify(r.make(), r.model()) == Powertrain.BEV)
                .sorted(Comparator.comparingDouble(InspectionRow::failPct))
                .toList();
    }

    /**
     * Cohort-year baseline (the "Merkit yhteensä, Mallit yhteensä" row for that year).
     *
     * @param cohortYear null returns the all-years grand total.
     */
    public static OptionalDouble baselineFailPct(Integer cohortYear) {
        return loadAll().stream()
                .filter(r -> Objects.equals(r.cohortYear(), cohortYear))
                .filter(r -> TOTALS_MAKE.equals(r.make()) && TOTALS_MODEL.equals(r.model()))
                .mapToDouble(InspectionRow::failPct)
                .findFirst();
    }

    /**
     * Set of cohort years present in the dataset (excludes the totals-year sentinel).
     */
    public static List<Integer> cohortYears() {
        return loadAll().stream()
                .map(InspectionRow::cohortYear)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /**
     * Tesla model × cohort-year leaf rows (excludes totals).
     */
    public static List<InspectionRow> teslaRows() {
        return loadAll().stream()
                .filter(r -> r.cohortYear() != null)
                .filter(r -> "Tesla".equals(r.make()))
                .filter(r -> !TOTALS_MODEL.equals(r.model()))
                .toList();
    }

    /**
     * Returns the i18n key for a Finnish defect string, e.g.
     * {@code inspection.defect.etuakselisto}. Callers should pass the result
     * through {@code T.tr(...)} so localisation respects the current UI locale.
     * Returns {@code null} for unknown / null inputs.
     */
    public static String defectKey(String defectFi) {
        if (defectFi == null) {
            return null;
        }
        String trimmed = defectFi.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "inspection.defect." + slug(trimmed);
    }

    private static String slug(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
                sb.append('_');
            }
        }
        // Normalise Finnish characters to ASCII so the i18n key lookup is stable.
        String normalised = sb.toString();
        normalised = normalised.replace('ä', 'a').replace('ö', 'o').replace('å', 'a');
        if (normalised.endsWith("_")) {
            normalised = normalised.substring(0, normalised.length() - 1);
        }
        return normalised;
    }

    /**
     * Distinct Finnish defect strings observed across all leaf rows. Useful
     * for asserting glossary coverage in tests.
     */
    public static java.util.Set<String> distinctDefectStrings() {
        return loadAll().stream()
                .filter(r -> r.cohortYear() != null)
                .filter(r -> !TOTALS_MAKE.equals(r.make()) && !TOTALS_MODEL.equals(r.model()))
                .flatMap(r -> r.topDefects().stream())
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    // -- Defect themes ---------------------------------------------------

    public enum DefectTheme {
        CHASSIS, BRAKES, BODY, POWERTRAIN, WHEELS, LIGHTS_SAFETY, DOCUMENTS
    }

    public record MakeModel(String make, String model) implements Comparable<MakeModel> {
        @Override
        public int compareTo(MakeModel o) {
            int c = make.compareToIgnoreCase(o.make);
            return c != 0 ? c : model.compareToIgnoreCase(o.model);
        }
    }

    public record ThemeBreakdown(DefectTheme theme, double share, double weight) {
    }

    /**
     * Distinct (make, model) pairs found in the dataset, excluding any totals
     * rows. Sorted alphabetically by make then model.
     */
    public static List<MakeModel> distinctMakeModels() {
        return loadAll().stream()
                .filter(r -> !TOTALS_MAKE.equals(r.make()) && !TOTALS_MODEL.equals(r.model()))
                .map(r -> new MakeModel(r.make(), r.model()))
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * The curated BEV-only allow-list as a set of {@link MakeModel} pairs.
     * Useful as the default selection in a model-picker UI.
     */
    public static java.util.LinkedHashSet<MakeModel> bevAllowList() {
        java.util.LinkedHashSet<MakeModel> out = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : BEV_MODELS.entrySet()) {
            for (String model : e.getValue()) {
                out.add(new MakeModel(e.getKey(), model));
            }
        }
        return out;
    }

    private static final Map<String, DefectTheme> DEFECT_THEMES;

    static {
        Map<String, DefectTheme> m = new HashMap<>();
        // CHASSIS
        m.put("Etuakselisto", DefectTheme.CHASSIS);
        m.put("Taka-akselisto", DefectTheme.CHASSIS);
        m.put("Jousitus ja iskunvaimennus", DefectTheme.CHASSIS);
        m.put("Ohjausnivelet ja -tangot", DefectTheme.CHASSIS);
        // BRAKES
        m.put("Käyttöjarru", DefectTheme.BRAKES);
        m.put("Käyttöjarrun dynamometritesti", DefectTheme.BRAKES);
        m.put("Seisontajarrun dynamometritesti", DefectTheme.BRAKES);
        // BODY
        m.put("Alustan kotelot ja pohjalevy", DefectTheme.BODY);
        m.put("Kori", DefectTheme.BODY);
        m.put("Runko", DefectTheme.BODY);
        m.put("Muut ikkunat", DefectTheme.BODY);
        // POWERTRAIN
        m.put("Sisäinen valvontajärjestelmä (OBD)", DefectTheme.POWERTRAIN);
        m.put("Bensiinimoottorin pakokaasumittaus", DefectTheme.POWERTRAIN);
        m.put("Dieselmoottorin pakokaasumittaus", DefectTheme.POWERTRAIN);
        m.put("Pakokaasupäästöt", DefectTheme.POWERTRAIN);
        // WHEELS
        m.put("Renkaat ja vanteet", DefectTheme.WHEELS);
        // LIGHTS_SAFETY
        m.put("Ajovakautusjärjestelmä", DefectTheme.LIGHTS_SAFETY);
        m.put("Ajovalo", DefectTheme.LIGHTS_SAFETY);
        m.put("Lähivalo", DefectTheme.LIGHTS_SAFETY);
        m.put("Turvavyöt ja -varusteet", DefectTheme.LIGHTS_SAFETY);
        m.put("Taksivarustus", DefectTheme.LIGHTS_SAFETY);
        // DOCUMENTS
        m.put("Asiapaperit", DefectTheme.DOCUMENTS);
        m.put("Valmistajan kilpi", DefectTheme.DOCUMENTS);
        DEFECT_THEMES = Map.copyOf(m);
    }

    /**
     * Maps a Finnish defect string to its theme. Returns {@code null} for
     * unknown strings so callers can choose to log + skip rather than throw.
     */
    public static DefectTheme themeOf(String defectFi) {
        if (defectFi == null) {
            return null;
        }
        return DEFECT_THEMES.get(defectFi.trim());
    }

    /**
     * Defect-theme breakdown for the entire dataset (every leaf row).
     * Convenience for the "all cars baseline" comparison.
     */
    public static List<ThemeBreakdown> breakdownAll() {
        return aggregateBreakdown(r -> true);
    }

    /**
     * Defect-theme breakdown for the rows matching the given (make, model)
     * selection. Returns zero-share rows for every theme when the selection
     * is empty.
     */
    public static List<ThemeBreakdown> breakdown(Set<MakeModel> selection) {
        Set<MakeModel> sel = selection == null ? Set.of() : selection;
        if (sel.isEmpty()) {
            return zeroBreakdown();
        }
        return aggregateBreakdown(r -> sel.contains(new MakeModel(r.make(), r.model())));
    }

    private static List<ThemeBreakdown> aggregateBreakdown(java.util.function.Predicate<InspectionRow> rowFilter) {
        EnumMap<DefectTheme, Double> weights = new EnumMap<>(DefectTheme.class);
        for (DefectTheme t : DefectTheme.values()) {
            weights.put(t, 0.0);
        }
        for (InspectionRow r : loadAll()) {
            if (r.cohortYear() == null) {
                continue; // skip "Vuodet yhteensä" totals rows
            }
            if (TOTALS_MAKE.equals(r.make()) || TOTALS_MODEL.equals(r.model())) {
                continue; // skip per-year and per-brand totals
            }
            if (!rowFilter.test(r)) {
                continue;
            }
            double rowWeight = r.inspections() * (r.failPct() / 100.0);
            if (rowWeight <= 0) {
                continue;
            }
            for (String defect : r.topDefects()) {
                DefectTheme theme = themeOf(defect);
                if (theme != null) {
                    weights.merge(theme, rowWeight, Double::sum);
                } else {
                    log.warn("Traficom defect string '{}' has no theme mapping (data refresh may need an update)", defect);
                }
            }
        }
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        List<ThemeBreakdown> out = new ArrayList<>(DefectTheme.values().length);
        for (DefectTheme t : DefectTheme.values()) {
            double w = weights.get(t);
            double share = total > 0 ? w / total : 0.0;
            out.add(new ThemeBreakdown(t, share, w));
        }
        return List.copyOf(out);
    }

    private static List<ThemeBreakdown> zeroBreakdown() {
        List<ThemeBreakdown> out = new ArrayList<>(DefectTheme.values().length);
        for (DefectTheme t : DefectTheme.values()) {
            out.add(new ThemeBreakdown(t, 0.0, 0.0));
        }
        return List.copyOf(out);
    }
}
