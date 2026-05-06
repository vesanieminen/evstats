package com.vesanieminen.services;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;

public class TraficomInspectionService {

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
}
