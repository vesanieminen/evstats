package com.vesanieminen.services;

import com.vesanieminen.model.UsedEvSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsedEvSnapshotRepository extends JpaRepository<UsedEvSnapshot, Long> {

    Optional<UsedEvSnapshot> findFirstByOrderByFetchedAtDesc();

    List<UsedEvSnapshot> findAllByOrderByFetchedAtAsc();

    List<UsedEvSnapshot> findAllByOrderByFetchedAtDesc();

    boolean existsByFetchedAt(Instant fetchedAt);
}
