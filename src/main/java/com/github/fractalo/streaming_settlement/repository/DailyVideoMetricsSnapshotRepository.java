package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.DailyVideoMetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyVideoMetricsSnapshotRepository extends JpaRepository<DailyVideoMetricsSnapshot, Long> {
    Optional<DailyVideoMetricsSnapshot> findByVideoIdAndDate(Long videoId, LocalDate date);
}
