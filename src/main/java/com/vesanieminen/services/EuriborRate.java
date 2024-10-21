package com.vesanieminen.services;

import java.time.LocalDate;

public class EuriborRate {
    private LocalDate date;
    private double rate;

    public EuriborRate(LocalDate date, double rate) {
        this.date = date;
        this.rate = rate;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getRate() {
        return rate;
    }
}