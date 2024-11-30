package com.github.fractalo.streaming_settlement.settlement.batch;

import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Map;


@SpringBootTest
class VideoIdRangePartitionerTest {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private SettlementConst settlementConst;

    @Test
    void testPartition() {
        VideoIdRangePartitioner videoIdRangePartitioner = new VideoIdRangePartitioner(
                videoRepository, settlementConst, LocalDate.of(2024, 11, 7));

        Map<String, ExecutionContext> partition = videoIdRangePartitioner.partition(10);
        for (Map.Entry<String, ExecutionContext> entry : partition.entrySet()) {
            System.out.println("entry = " + entry);
        }
    }

}