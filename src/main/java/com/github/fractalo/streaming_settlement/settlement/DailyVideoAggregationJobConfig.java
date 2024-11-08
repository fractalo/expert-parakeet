package com.github.fractalo.streaming_settlement.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class DailyVideoAggregationJobConfig {

    private final JobRepository jobRepository;
    private final Step dailyVideoMetricsSnapshotStep;
    private final Step dailyVideoStatisticsStep;
    private final Step dailyVideoSettlementStep;

    @Bean
    public Job dailyVideoAggregationJob() {
        return new JobBuilder("dailyVideoAggregationJob", jobRepository)
                .start(dailyVideoMetricsSnapshotStep)
                .next(dailyVideoStatisticsStep)
                .next(dailyVideoSettlementStep)
                .build();
    }

}
