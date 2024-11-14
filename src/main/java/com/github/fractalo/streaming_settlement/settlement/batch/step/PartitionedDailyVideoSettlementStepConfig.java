package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.DailyVideoSettlement;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInput;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import com.github.fractalo.streaming_settlement.settlement.batch.VideoIdRangePartitioner;
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

import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PartitionedDailyVideoSettlementStepConfig {

    public static final String STEP_NAME = "partitionedDailyVideoSettlementStep";
    private static final int CHUNK_SIZE = 50;
    private static final int POOL_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final ItemProcessor<DailyVideoSettlementInput, DailyVideoSettlement> dailyVideoSettlementProcessor;
    private final RepositoryItemWriter<DailyVideoSettlement> dailyVideoSettlementWriter;


    @Bean
    public Step partitionedDailyVideoSettlementStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<DailyVideoSettlementInput, DailyVideoSettlement>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(partitionedDailyVideoSettlementInputReader(null, null, null))
                .processor(dailyVideoSettlementProcessor)
                .writer(dailyVideoSettlementWriter)
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
                        WHERE v.id BETWEEN :minVideoId AND :maxVideoId
                        ORDER BY v.id ASC
                        """)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(Map.of(
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
        partitionHandler.setStep(partitionedDailyVideoSettlementStep());
        partitionHandler.setTaskExecutor(taskExecutor());
        partitionHandler.setGridSize(POOL_SIZE);
        return partitionHandler;
    }

}
