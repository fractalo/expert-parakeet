package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.DailyVideoMetrics;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.DailyVideoMetricsRepository;
import com.github.fractalo.streaming_settlement.settlement.SettlementConst;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyVideoMetricsService {
    private final DailyVideoMetricsRepository dailyVideoMetricsRepository;
    private final SettlementConst settlementConst;

    @Transactional
    public DailyVideoMetrics createOrGetDailyVideoMetricsForUpdate(Video video) {
        LocalDate now = LocalDate.now(settlementConst.ZONE_ID);
        Optional<DailyVideoMetrics> metrics = dailyVideoMetricsRepository
                .findByVideoIdAndDateWithLock(video.getId(), now);

        if (metrics.isPresent()) {
            return metrics.get();
        }

        try {
            DailyVideoMetrics dailyVideoMetrics = new DailyVideoMetrics(video, now);
            dailyVideoMetricsRepository.save(dailyVideoMetrics);
            return dailyVideoMetrics;
        } catch (DataIntegrityViolationException e) {
            return dailyVideoMetricsRepository.findByVideoIdAndDateWithLock(video.getId(), now)
                    .orElseThrow(() -> new IllegalStateException("Failed to create or get daily metrics."));
        }
    }
}
