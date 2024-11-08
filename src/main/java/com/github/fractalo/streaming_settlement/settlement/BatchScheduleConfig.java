package com.github.fractalo.streaming_settlement.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class BatchScheduleConfig {
    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final SettlementConst settlementConst;

    @Scheduled(cron = "0 0 4 * * *", zone = "#{@settlementConst.ZONE_ID.toString()}")
    public void scheduleDailyJob() throws Exception {

        LocalDate yesterday = LocalDate.now(settlementConst.ZONE_ID).minusDays(1);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate("baseDate", yesterday)
                .toJobParameters();

        jobLauncher.run(jobRegistry.getJob("dailyVideoAggregationJob"), jobParameters);
    }
}
