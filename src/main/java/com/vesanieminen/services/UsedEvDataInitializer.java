package com.vesanieminen.services;

import com.opencsv.CSVReader;
import com.vesanieminen.model.UsedEvSnapshot;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Pre-loads any historical used-EV snapshots that ship with the JAR
 * (data/used-ev-snapshots.csv) into the database on startup.
 * Idempotent: rows whose fetchedAt is already in the DB are skipped,
 * so re-deploying never duplicates data.
 */
@Component
public class UsedEvDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(UsedEvDataInitializer.class);
    private static final String RESOURCE = "data/used-ev-snapshots.csv";

    private final UsedEvSnapshotRepository repository;

    public UsedEvDataInitializer(UsedEvSnapshotRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    void preload() {
        ClassPathResource res = new ClassPathResource(RESOURCE);
        if (!res.exists()) {
            return;
        }
        int inserted = 0;
        int skipped = 0;
        Set<Instant> seen = new HashSet<>();
        try (InputStream in = res.getInputStream();
             CSVReader reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String[] row;
            int line = 0;
            while ((row = reader.readNext()) != null) {
                line++;
                if (line == 1 && row.length >= 2
                        && row[0].trim().equalsIgnoreCase("fetchedAt")
                        && row[1].trim().equalsIgnoreCase("count")) {
                    continue;
                }
                if (row.length < 2 || row[0].isBlank() || row[1].isBlank()) {
                    skipped++;
                    continue;
                }
                try {
                    Instant fetchedAt = Instant.parse(row[0].trim());
                    int count = Integer.parseInt(row[1].trim());
                    if (count < 0 || !seen.add(fetchedAt) || repository.existsByFetchedAt(fetchedAt)) {
                        skipped++;
                        continue;
                    }
                    repository.save(new UsedEvSnapshot(fetchedAt, count));
                    inserted++;
                } catch (Exception parseEx) {
                    skipped++;
                }
            }
        } catch (Exception e) {
            log.warn("Could not preload bundled used-EV snapshots from {}.", RESOURCE, e);
            return;
        }
        if (inserted > 0 || skipped > 0) {
            log.info("Used-EV preload from {}: inserted {}, skipped {} (already present or invalid).",
                    RESOURCE, inserted, skipped);
        }
    }
}
