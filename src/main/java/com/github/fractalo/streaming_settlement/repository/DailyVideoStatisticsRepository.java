package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.DailyVideoStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyVideoStatisticsRepository extends JpaRepository<DailyVideoStatistics, Long> {
}
