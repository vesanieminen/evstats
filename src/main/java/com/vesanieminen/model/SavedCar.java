package com.vesanieminen.model;

import java.util.UUID;

public record SavedCar(
        String id,
        String name,
        int capacity,
        double efficiency,
        String imageUrl
) {
    public SavedCar(String name, int capacity, double efficiency, String imageUrl) {
        this(UUID.randomUUID().toString(), name, capacity, efficiency, imageUrl);
    }

    public String getDisplayName() {
        return name + " (" + capacity + " kWh)";
    }

    public int calculateRange(double soc) {
        double energyAvailable = capacity * soc / 100.0;
        return (int) Math.round(energyAvailable / efficiency * 100);
    }

    public EVModel toEVModel() {
        return new EVModel(name, capacity, efficiency);
    }
}
