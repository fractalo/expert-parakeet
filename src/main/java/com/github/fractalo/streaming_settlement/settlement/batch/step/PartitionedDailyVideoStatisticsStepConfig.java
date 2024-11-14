package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.DailyVideoStatistics;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.batch.VideoIdRangePartitioner;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PartitionedDailyVideoStatisticsStepConfig {

    public static final String STEP_NAME = "partitionedDailyVideoStatisticsStep";
    private static final int CHUNK_SIZE = 50;
    private static final int POOL_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final ItemProcessor<DailyVideoStatisticsInput, DailyVideoStatistics> dailyVideoStatisticsProcessor;
    private final RepositoryItemWriter<DailyVideoStatistics> dailyVideoStatisticsWriter;


    @Bean
    public Step partitionedDailyVideoStatisticsStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<DailyVideoStatisticsInput, DailyVideoStatistics>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(partitionedDailyVideoStatisticsInputReader(null, null, null))
                .processor(dailyVideoStatisticsProcessor)
                .writer(dailyVideoStatisticsWriter)
                .build();
    }

    @Bean
    public Step partitionedDailyVideoStatisticsStepManager() {
        return new StepBuilder(STEP_NAME + ".manager", jobRepository)
                .partitioner(STEP_NAME, partitioner(null))
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DailyVideoStatisticsInput> partitionedDailyVideoStatisticsInputReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate,
            @Value("#{stepExecutionContext[minVideoId]}") Long minVideoId,
            @Value("#{stepExecutionContext[maxVideoId]}") Long maxVideoId
    ) {
        Instant startOfDay = baseDate.atStartOfDay(settlementConst.ZONE_ID).toInstant();
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JpaPagingItemReaderBuilder<DailyVideoStatisticsInput>()
                .name("partitionedDailyVideoStatisticsInputReader")
                .queryString("""
                        SELECT new com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput(
                            v.id,
                            COALESCE(today_vms.viewCount - yesterday_vms.viewCount, today_vms.viewCount),
                            COALESCE(SUM(vwh.watchTimeMs), 0),
                            COALESCE(yesterday_vs.weeklyViewCount, 0),
                            COALESCE(yesterday_vs.monthlyViewCount, 0),
                            COALESCE(yesterday_vs.weeklyWatchTimeMs, 0),
                            COALESCE(yesterday_vs.monthlyWatchTimeMs, 0)
                        )
                        FROM Video v
                            LEFT JOIN VideoWatchHistory vwh
                                ON vwh.video.id = v.id
                                AND vwh.viewStartedAt >= :startOfDay AND vwh.viewStartedAt < :startOfNextDay
                            LEFT JOIN DailyVideoMetricsSnapshot today_vms
                                ON today_vms.video.id = v.id
                                AND today_vms.date = :today
                            LEFT JOIN DailyVideoMetricsSnapshot yesterday_vms
                                ON yesterday_vms.video.id = v.id
                                AND yesterday_vms.date = :yesterday
                            LEFT JOIN DailyVideoStatistics yesterday_vs
                                ON yesterday_vs.video.id = v.id
                                AND yesterday_vs.date = :yesterday
                        WHERE v.id BETWEEN :minVideoId AND :maxVideoId
                        GROUP BY v.id
                        ORDER BY v.id ASC
                        """)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(Map.of(
                        "startOfDay", startOfDay,
                        "startOfNextDay", startOfNextDay,
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1),
                        "minVideoId", minVideoId,
                        "maxVideoId", maxVideoId
                ))
                .build();
    }

    @Bean(name = STEP_NAME + "_partitioner")
    @StepScope
    public Partitioner partitioner(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        return new VideoIdRangePartitioner(videoRepository, settlementConst, baseDate);
    }

    @Bean(name = STEP_NAME + "_taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setThreadNamePrefix(STEP_NAME + "-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = STEP_NAME + "_partitionHandler")
    public PartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setStep(partitionedDailyVideoStatisticsStep());
        partitionHandler.setTaskExecutor(taskExecutor());
        partitionHandler.setGridSize(POOL_SIZE);
        return partitionHandler;
    }

}
