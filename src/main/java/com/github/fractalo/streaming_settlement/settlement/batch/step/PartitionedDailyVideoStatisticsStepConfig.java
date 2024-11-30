package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.DailyVideoStatistics;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.batch.VideoIdRangePartitioner;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInitializer;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PartitionedDailyVideoStatisticsStepConfig {

    public static final String STEP_NAME = "partitionedDailyVideoStatisticsStep";
    private static final int CHUNK_SIZE = 100;
    private static final int POOL_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final ItemProcessor<DailyVideoStatisticsInput, DailyVideoStatisticsInitializer> dailyVideoStatisticsInitProcessor;
    private final JdbcBatchItemWriter<DailyVideoStatisticsInitializer> dailyVideoStatisticsBatchWriter;
    private final DataSource dataSource;


    @Bean
    public Step partitionedDailyVideoStatisticsStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<DailyVideoStatisticsInput, DailyVideoStatisticsInitializer>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(partitionedDailyVideoStatisticsInputZeroOffsetReader(null, null, null))
                .processor(dailyVideoStatisticsInitProcessor)
                .writer(dailyVideoStatisticsBatchWriter)
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
                        WHERE v.createdAt < :startOfNextDay
                            AND v.id BETWEEN :minVideoId AND :maxVideoId
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

    @Bean
    @StepScope
    public JdbcPagingItemReader<DailyVideoStatisticsInput> partitionedDailyVideoStatisticsInputZeroOffsetReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate,
            @Value("#{stepExecutionContext[minVideoId]}") Long minVideoId,
            @Value("#{stepExecutionContext[maxVideoId]}") Long maxVideoId
    ) {
        Instant startOfDay = baseDate.atStartOfDay(settlementConst.ZONE_ID).toInstant();
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JdbcPagingItemReaderBuilder<DailyVideoStatisticsInput>()
                .name("partitionedDailyVideoStatisticsInputZeroOffsetReader")
                .selectClause("""
                        v.video_id,
                        COALESCE(dvms_today.view_count - dvms_yesterday.view_count, dvms_today.view_count) AS viewCountIncrement,
                        (
                            SELECT COALESCE(SUM(vwh.watch_time_ms), 0)
                            FROM video_watch_history vwh
                            WHERE vwh.video_id = v.video_id
                                AND vwh.view_started_at >= :startOfDay
                                AND vwh.view_started_at < :startOfNextDay
                        ) AS watchTimeIncrementMs,
                        COALESCE(dvs_yesterday.weekly_view_count, 0) AS yesterdayWeeklyViewCount,
                        COALESCE(dvs_yesterday.monthly_view_count, 0) AS yesterdayMonthlyViewCount,
                        COALESCE(dvs_yesterday.weekly_watch_time_ms, 0) AS yesterdayWeeklyWatchTimeMs,
                        COALESCE(dvs_yesterday.monthly_watch_time_ms, 0) AS yesterdayMonthlyWatchTimeMs
                        """)
                .fromClause("""
                        video v
                        LEFT JOIN
                            daily_video_metrics_snapshot dvms_today
                            ON dvms_today.video_id = v.video_id
                            AND dvms_today.date = :today
                        LEFT JOIN
                            daily_video_metrics_snapshot dvms_yesterday
                            ON dvms_yesterday.video_id = v.video_id
                            AND dvms_yesterday.date = :yesterday
                        LEFT JOIN
                            daily_video_statistics dvs_yesterday
                            ON dvs_yesterday.date = :yesterday
                            AND dvs_yesterday.video_id = v.video_id
                        """)
                .whereClause("""
                        v.created_at < :startOfNextDay
                        AND v.video_id BETWEEN :minVideoId AND :maxVideoId
                        """)
                .sortKeys(Map.of("v.video_id", Order.ASCENDING))
                .pageSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .parameterValues(Map.of(
                        "startOfDay", startOfDay.toString(),
                        "startOfNextDay", startOfNextDay.toString(),
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1),
                        "minVideoId", minVideoId,
                        "maxVideoId", maxVideoId
                ))
                .rowMapper(new DataClassRowMapper<>(DailyVideoStatisticsInput.class))
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
