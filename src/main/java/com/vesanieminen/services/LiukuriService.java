package com.vesanieminen.services;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service
public class LiukuriService {

    private final LiukuriApiClient liukuriApiClient;

    public LiukuriService() {
        this.liukuriApiClient = new LiukuriApiClient();
    }

    public LiukuriApiClient.CalculationResponse performCalculation() {
        return liukuriApiClient.calculateCost();
    }

    public LiukuriApiClient.CalculationResponse performCalculation(LinkedHashMap<Long, Double> consumptionData, double margin, boolean vat) {
        return liukuriApiClient.calculateCost(consumptionData, margin, vat);
    }

}