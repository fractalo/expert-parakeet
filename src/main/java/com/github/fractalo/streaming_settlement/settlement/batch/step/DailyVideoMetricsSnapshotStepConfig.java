package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.service.DailyVideoMetricsSnapshotService;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DailyVideoMetricsSnapshotStepConfig {

    private static final int CHUNK_SIZE = 50;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final DailyVideoMetricsSnapshotService dailyVideoMetricsSnapshotService;

    @Bean
    public Step dailyVideoMetricsSnapshotStep() {
        return new StepBuilder("dailyVideoMetricsSnapshotStep", jobRepository)
                .<Video, Video>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(videoReader(null))
                .writer(dailyVideoMetricsSnapshotWriter())
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Video> videoReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();
        return new RepositoryItemReaderBuilder<Video>()
                .name("videoReader")
                .pageSize(CHUNK_SIZE)
                .methodName("findByCreatedAtBefore")
                .arguments(Collections.singletonList(startOfNextDay))
                .repository(videoRepository)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemWriter<Video> dailyVideoMetricsSnapshotWriter() {
        return videos -> videos.forEach(dailyVideoMetricsSnapshotService::tryCreateSnapshotIfRequired);
    }

    @Bean
    public JobExecutionDecider skipDailyVideoMetricsSnapshotStepDecider() {
        return (jobExecution, stepExecution) -> {
            LocalDate baseDate = jobExecution.getJobParameters().getLocalDate("baseDate");
            LocalDate yesterday = LocalDate.now(settlementConst.ZONE_ID).minusDays(1);

            assert baseDate != null;

            if (baseDate.isEqual(yesterday)) {
                return new FlowExecutionStatus("CONTINUE");
            } else {
                return new FlowExecutionStatus("SKIP");
            }
        };
    }

}
