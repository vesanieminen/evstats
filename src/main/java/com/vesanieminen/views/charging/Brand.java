package com.vesanieminen.views.charging;

import com.vesanieminen.model.EVModel;

/**
 * Identifies the manufacturer of a {@link EVModel} preset and provides the
 * corresponding CSS class slug for per-brand theming on the charging view.
 *
 * <p>Brand classification is purely string-prefix-based on {@code EVModel.name()}.
 * The {@link #DEFAULT} value is returned for the {@code Custom} preset and any
 * unknown future preset whose first word doesn't match a known brand.</p>
 */
public enum Brand {
    TESLA, BMW, MERCEDES, AUDI, PORSCHE, VOLKSWAGEN, HYUNDAI, POLESTAR, FORD, DEFAULT;

    /**
     * Typed entry point: prefer this over {@link #fromModelName(String)} from
     * view code. Returns {@link #DEFAULT} for {@code null} or {@link EVModel#CUSTOM}.
     */
    public static Brand from(EVModel model) {
        if (model == null || model == EVModel.CUSTOM) {
            return DEFAULT;
        }
        return fromModelName(model.name());
    }

    /**
     * Classifier on the raw model-name string. The brand is the first
     * whitespace-separated token of the preset name, e.g. {@code "Tesla Model 3 LR"}
     * → {@link #TESLA}, {@code "Mercedes EQS 450+"} → {@link #MERCEDES}.
     */
    public static Brand fromModelName(String evModelName) {
        if (evModelName == null) {
            return DEFAULT;
        }
        String head = evModelName.split(" ", 2)[0];
        return switch (head) {
            case "Tesla" -> TESLA;
            case "BMW" -> BMW;
            case "Mercedes" -> MERCEDES;
            case "Audi" -> AUDI;
            case "Porsche" -> PORSCHE;
            case "Volkswagen" -> VOLKSWAGEN;
            case "Hyundai" -> HYUNDAI;
            case "Polestar" -> POLESTAR;
            case "Ford" -> FORD;
            default -> DEFAULT;
        };
    }

    /** CSS class slug, e.g. {@code "brand-tesla"} for {@link #TESLA}. */
    public String cssClass() {
        return "brand-" + name().toLowerCase();
    }
}
