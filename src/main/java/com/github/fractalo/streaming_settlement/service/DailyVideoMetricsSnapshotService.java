package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.DailyVideoMetricsSnapshot;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.DailyVideoMetricsSnapshotRepository;
import com.github.fractalo.streaming_settlement.settlement.SettlementConst;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyVideoMetricsSnapshotService {
    private final DailyVideoMetricsSnapshotRepository dailyVideoMetricsSnapshotRepository;
    private final SettlementConst settlementConst;

    @Transactional
    public void tryCreateSnapshotIfRequired(Video video) {
        if (!isSnapshotRequired(video)) return;

        LocalDate yesterday = LocalDate.now(settlementConst.ZONE_ID).minusDays(1);
        DailyVideoMetricsSnapshot snapshot = new DailyVideoMetricsSnapshot(video, yesterday);
        try {
            dailyVideoMetricsSnapshotRepository.save(snapshot);
        } catch (DataIntegrityViolationException ignored) {}
    }

    private boolean isSnapshotRequired(Video video) {
        LocalDate today = LocalDate.now(settlementConst.ZONE_ID);
        Instant createdAt = video.getCreatedAt();
        if (createdAt == null ||
            createdAt.atZone(settlementConst.ZONE_ID).toLocalDate().isEqual(today)) {
            return false;
        }

        Instant metricsUpdatedAt = video.getMetricsUpdatedAt();
        if (metricsUpdatedAt == null) return true;

        return metricsUpdatedAt
                .atZone(settlementConst.ZONE_ID)
                .toLocalDate()
                .isBefore(today);
    }
}
