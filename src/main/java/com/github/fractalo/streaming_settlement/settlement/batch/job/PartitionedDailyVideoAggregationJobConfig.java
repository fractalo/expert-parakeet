package com.github.fractalo.streaming_settlement.settlement.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PartitionedDailyVideoAggregationJobConfig {

    public static final String JOB_NAME = "partitionedDailyVideoAggregationJob";

    private final JobRepository jobRepository;
    private final Step partitionedDailyVideoMetricsSnapshotStep;
    private final Step partitionedDailyVideoStatisticsStep;
    private final Step partitionedDailyVideoSettlementStep;
    private final JobExecutionDecider skipDailyVideoMetricsSnapshotStepDecider;
    private final JobParametersValidator dailyVideoAggregationJobParametersValidator;


    @Bean
    public Job partitionedDailyVideoAggregationJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(dailyVideoAggregationJobParametersValidator)
                .start(skipDailyVideoMetricsSnapshotStepDecider)
                    .on("CONTINUE")
                    .to(partitionedDailyVideoMetricsSnapshotStep)
                    .next(mainFlow())
                .from(skipDailyVideoMetricsSnapshotStepDecider)
                    .on("SKIP")
                    .to(mainFlow())
                .end()
                .build();
    }

    @Bean(name = JOB_NAME + "_mainFlow")
    public Flow mainFlow() {
        return new FlowBuilder<Flow>("mainFlow")
                .start(partitionedDailyVideoStatisticsStep)
                .next(partitionedDailyVideoSettlementStep)
                .end();
    }

}
