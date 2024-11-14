package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import com.github.fractalo.streaming_settlement.settlement.batch.VideoIdRangePartitioner;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PartitionedDailyVideoMetricsSnapshotStepConfig {

    public static final String STEP_NAME = "partitionedDailyVideoMetricsSnapshotStep";
    private static final int CHUNK_SIZE = 50;
    private static final int POOL_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final ItemWriter<Video> dailyVideoMetricsSnapshotWriter;

    @Bean
    public Step partitionedDailyVideoMetricsSnapshotStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Video, Video>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(partitionedVideoReader(null, null))
                .writer(dailyVideoMetricsSnapshotWriter)
                .build();
    }

    @Bean
    public Step partitionedDailyVideoMetricsSnapshotStepManager() {
        return new StepBuilder(STEP_NAME+ ".manager", jobRepository)
                .partitioner(STEP_NAME, partitioner(null))
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public RepositoryItemReader<Video> partitionedVideoReader(
            @Value("#{stepExecutionContext[minVideoId]}") Long minVideoId,
            @Value("#{stepExecutionContext[maxVideoId]}") Long maxVideoId
    ) {
        return new RepositoryItemReaderBuilder<Video>()
                .name("partitionedVideoReader")
                .pageSize(CHUNK_SIZE)
                .methodName("findByIdBetween")
                .arguments(List.of(minVideoId, maxVideoId))
                .repository(videoRepository)
                .sorts(Map.of("id", Sort.Direction.ASC))
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
        partitionHandler.setStep(partitionedDailyVideoMetricsSnapshotStep());
        partitionHandler.setTaskExecutor(taskExecutor());
        partitionHandler.setGridSize(POOL_SIZE);
        return partitionHandler;
    }
}
