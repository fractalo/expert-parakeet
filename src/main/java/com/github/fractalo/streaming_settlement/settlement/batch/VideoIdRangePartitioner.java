package com.github.fractalo.streaming_settlement.settlement.batch;

import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class VideoIdRangePartitioner implements Partitioner {

    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final LocalDate baseDate;

    @Override
    @Transactional(readOnly = true)
    public Map<String, ExecutionContext> partition(int gridSize) {
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        Optional<Video> firstVideo = videoRepository.findFirstByCreatedAtBeforeOrderByIdAsc(startOfNextDay);
        Optional<Video> lastVideo = videoRepository.findFirstByCreatedAtBeforeOrderByIdDesc(startOfNextDay);
        if (firstVideo.isEmpty() || lastVideo.isEmpty()) {
            return Map.of();
        }

        final long minId = firstVideo.get().getId();
        final long maxId = lastVideo.get().getId();

        Map<String, ExecutionContext> partitions = new HashMap<>();
        final long targetSize = (maxId - minId) / gridSize + 1;
        long start = minId;
        long end = start + targetSize - 1;

        for (int i = 0; start <= maxId; ++i) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minVideoId", start);
            context.putLong("maxVideoId", Math.min(end, maxId));
            partitions.put("partition" + i, context);

            start += targetSize;
            end += targetSize;
        }

        return partitions;
    }
}
