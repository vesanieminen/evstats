package com.vesanieminen.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TraficomInspectionServiceTest {

    /**
     * Every distinct Finnish defect string in the parsed dataset must map to a
     * known {@link TraficomInspectionService.DefectTheme}. A new string in a
     * future Traficom data release fails this test so the maintainer remembers
     * to extend {@code DEFECT_THEMES}.
     */
    @Test
    void everyDistinctDefectStringHasATheme() {
        Set<String> distinct = TraficomInspectionService.distinctDefectStrings();
        assertTrue(distinct.size() >= 20, "expected the dataset to have at least 20 distinct defect strings");
        List<String> unmapped = new ArrayList<>();
        for (String fi : distinct) {
            if (TraficomInspectionService.themeOf(fi) == null) {
                unmapped.add(fi);
            }
        }
        if (!unmapped.isEmpty()) {
            fail("Defect strings without a theme mapping: " + unmapped);
        }
    }

    /**
     * Every distinct defect string must have a corresponding {@code
     * inspection.defect.<slug>} entry in {@code messages.properties} (the
     * default English bundle, used as the fallback for all locales).
     */
    @Test
    void everyDistinctDefectStringHasAnEnglishGlossaryEntry() throws IOException {
        Properties props = new Properties();
        try (InputStream in = TraficomInspectionServiceTest.class.getClassLoader()
                .getResourceAsStream("messages.properties")) {
            assertNotNull(in, "messages.properties not on test classpath");
            props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        }
        Set<String> distinct = TraficomInspectionService.distinctDefectStrings();
        List<String> missing = new ArrayList<>();
        for (String fi : distinct) {
            String key = TraficomInspectionService.defectKey(fi);
            assertNotNull(key, "defectKey(\"" + fi + "\") returned null");
            if (!props.containsKey(key) || props.getProperty(key).isBlank()) {
                missing.add(fi + " -> " + key);
            }
        }
        if (!missing.isEmpty()) {
            fail("Defect strings missing from messages.properties: " + missing);
        }
    }
}
