package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.DailyVideoMetrics;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyVideoMetricsRepository extends JpaRepository<DailyVideoMetrics, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select dvm from DailyVideoMetrics dvm where dvm.video.id = :videoId and dvm.date = :date")
    Optional<DailyVideoMetrics> findByVideoIdAndDateWithLock(Long videoId, LocalDate date);

    Optional<DailyVideoMetrics> findByVideoIdAndDate(Long videoId, LocalDate date);
}
