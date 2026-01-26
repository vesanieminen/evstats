package com.vesanieminen.model;

import java.util.List;

public record EVModel(String name, int capacity, double efficiency) {

    public static final EVModel CUSTOM = new EVModel("Custom", 75, 17.0);

    public static final List<EVModel> PRESETS = List.of(
            new EVModel("Tesla Model 3 LR", 75, 14.5),
            new EVModel("Tesla Model Y LR", 75, 15.2),
            new EVModel("Tesla Model S", 100, 16.1),
            new EVModel("Tesla Model X", 100, 18.5),
            new EVModel("BMW iX xDrive50", 105, 19.8),
            new EVModel("Mercedes EQS 450+", 108, 17.5),
            new EVModel("Audi e-tron GT", 93, 19.2),
            new EVModel("Porsche Taycan", 93, 18.8),
            new EVModel("Volkswagen ID.4", 77, 16.5),
            new EVModel("Hyundai Ioniq 6", 77, 14.0),
            new EVModel("Polestar 2 LR", 78, 16.8),
            new EVModel("Ford Mustang Mach-E", 91, 17.2),
            CUSTOM
    );

    public static EVModel findByName(String name) {
        return PRESETS.stream()
                .filter(m -> m.name().equals(name))
                .findFirst()
                .orElse(CUSTOM);
    }

    public String getDisplayName() {
        return name + " (" + capacity + " kWh)";
    }

    public int calculateRange(double soc) {
        double energyAvailable = capacity * soc / 100.0;
        return (int) Math.round(energyAvailable / efficiency * 100);
    }
}
