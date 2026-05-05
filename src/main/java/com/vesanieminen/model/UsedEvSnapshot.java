package com.vesanieminen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "used_ev_snapshots", indexes = {
        @Index(name = "idx_used_ev_snapshots_fetched_at", columnList = "fetched_at")
})
public class UsedEvSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @jakarta.persistence.Column(nullable = false)
    private int count;

    public UsedEvSnapshot() {
    }

    public UsedEvSnapshot(Instant fetchedAt, int count) {
        this.fetchedAt = fetchedAt;
        this.count = count;
    }

    public Long getId() {
        return id;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
