package com.github.fractalo.streaming_settlement.settlement.batch.job;

import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;


@Configuration
@RequiredArgsConstructor
public class DailyVideoAggregationJobConfig {

    public static final String JOB_NAME = "dailyVideoAggregationJob";

    private final JobRepository jobRepository;
    private final Step dailyVideoMetricsSnapshotStep;
    private final Step dailyVideoStatisticsStep;
    private final Step dailyVideoSettlementStep;
    private final SettlementConst settlementConst;
    private final JobExecutionDecider skipDailyVideoMetricsSnapshotStepDecider;

    @Bean
    public Job dailyVideoAggregationJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(dailyVideoAggregationJobParametersValidator())
                .start(skipDailyVideoMetricsSnapshotStepDecider)
                    .on("CONTINUE")
                    .to(dailyVideoMetricsSnapshotStep)
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
                .start(dailyVideoStatisticsStep)
                .next(dailyVideoSettlementStep)
                .end();
    }

    @Bean
    public JobParametersValidator dailyVideoAggregationJobParametersValidator() {
        return (JobParameters jobParameters) -> {
            if (jobParameters == null) {
                throw new JobParametersInvalidException("jobParameters is null");
            }

            LocalDate baseDate = jobParameters.getLocalDate("baseDate");
            LocalDate today = LocalDate.now(settlementConst.ZONE_ID);

            if (baseDate == null) {
                throw new JobParametersInvalidException("baseDate is required");
            } else if (!baseDate.isBefore(today)) {
                throw new JobParametersInvalidException("baseDate must be before today");
            }
        };
    }

}
