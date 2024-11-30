package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.DailyVideoSettlement;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInitializer;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInput;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import com.github.fractalo.streaming_settlement.settlement.batch.VideoIdRangePartitioner;
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
public class PartitionedDailyVideoSettlementStepConfig {

    public static final String STEP_NAME = "partitionedDailyVideoSettlementStep";
    private static final int CHUNK_SIZE = 100;
    private static final int POOL_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final ItemProcessor<DailyVideoSettlementInput, DailyVideoSettlementInitializer> dailyVideoSettlementInitProcessor;
    private final JdbcBatchItemWriter<DailyVideoSettlementInitializer> dailyVideoSettlementBatchWriter;
    private final DataSource dataSource;


    @Bean
    public Step partitionedDailyVideoSettlementStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<DailyVideoSettlementInput, DailyVideoSettlementInitializer>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(partitionedDailyVideoSettlementInputZeroOffsetReader(null, null, null))
                .processor(dailyVideoSettlementInitProcessor)
                .writer(dailyVideoSettlementBatchWriter)
                .build();
    }

    @Bean
    public Step partitionedDailyVideoSettlementStepManager() {
        return new StepBuilder(STEP_NAME + ".manager", jobRepository)
                .partitioner(STEP_NAME, partitioner(null))
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DailyVideoSettlementInput> partitionedDailyVideoSettlementInputReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate,
            @Value("#{stepExecutionContext[minVideoId]}") Long minVideoId,
            @Value("#{stepExecutionContext[maxVideoId]}") Long maxVideoId
    ) {
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JpaPagingItemReaderBuilder<DailyVideoSettlementInput>()
                .name("partitionedDailyVideoSettlementInputReader")
                .queryString("""
                        SELECT new com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInput(
                            v.id,
                            today_vms.viewCount,
                            today_vms.advertisementVideoViewCount,
                            COALESCE(yesterday_vms.viewCount, 0),
                            COALESCE(yesterday.advertisementVideoViewCount, 0)
                        )
                        FROM Video v
                            LEFT JOIN DailyVideoMetricsSnapshot today_vms
                                ON today_vms.video.id = v.id
                                AND today_vms.date = :today
                            LEFT JOIN DailyVideoMetricsSnapshot yesterday_vms
                                ON yesterday_vms.video.id = v.id
                                AND yesterday_vms.date = :yesterday
                        WHERE v.createdAt < :startOfNextDay
                            AND v.id BETWEEN :minVideoId AND :maxVideoId
                        ORDER BY v.id ASC
                        """)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(Map.of(
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1),
                        "startOfNextDay", startOfNextDay,
                        "minVideoId", minVideoId,
                        "maxVideoId", maxVideoId
                ))
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<DailyVideoSettlementInput> partitionedDailyVideoSettlementInputZeroOffsetReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate,
            @Value("#{stepExecutionContext[minVideoId]}") Long minVideoId,
            @Value("#{stepExecutionContext[maxVideoId]}") Long maxVideoId
    ) {
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JdbcPagingItemReaderBuilder<DailyVideoSettlementInput>()
                .name("partitionedDailyVideoSettlementInputZeroOffsetReader")
                .selectClause("""
                        v.video_id,
                        dvms_today.view_count AS todayViewCount,
                        dvms_today.advertisement_video_view_count AS todayAdVideoViewCount,
                        COALESCE(dvms_yesterday.view_count, 0) AS yesterdayViewCount,
                        COALESCE(dvms_yesterday.advertisement_video_view_count, 0) AS yesterdayAdVideoViewCount
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
                        """)
                .whereClause("""
                        v.created_at < :startOfNextDay
                        AND v.video_id BETWEEN :minVideoId AND :maxVideoId
                        """)
                .sortKeys(Map.of("v.video_id", Order.ASCENDING))
                .pageSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .parameterValues(Map.of(
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1),
                        "startOfNextDay", startOfNextDay.toString(),
                        "minVideoId", minVideoId,
                        "maxVideoId", maxVideoId
                ))
                .rowMapper(new DataClassRowMapper<>(DailyVideoSettlementInput.class))
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
        partitionHandler.setStep(partitionedDailyVideoSettlementStep());
        partitionHandler.setTaskExecutor(taskExecutor());
        partitionHandler.setGridSize(POOL_SIZE);
        return partitionHandler;
    }

}
