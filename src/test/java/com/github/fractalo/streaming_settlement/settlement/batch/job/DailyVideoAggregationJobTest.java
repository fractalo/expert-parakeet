package com.github.fractalo.streaming_settlement.settlement.batch.job;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBatchTest
@SpringBootTest
class DailyVideoAggregationJobTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    @Qualifier(DailyVideoAggregationJobConfig.JOB_NAME)
    private Job dailyVideoAggregationJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(dailyVideoAggregationJob);
    }

    @AfterEach
    void cleanUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    void runJob() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate("baseDate", LocalDate.of(2024, 11, 7))
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        Assertions.assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Job 수행 시간 계산
        LocalDateTime jobStartTime = jobExecution.getStartTime();
        LocalDateTime jobEndTime = jobExecution.getEndTime();
        System.out.println("Job execution time: " + Duration.between(jobStartTime, jobEndTime).toMillis() + " ms");

        // Step 수행 시간 계산
        List<StepExecution> stepExecutions = jobExecution.getStepExecutions().stream().toList();
        for (StepExecution stepExecution : stepExecutions) {
            LocalDateTime stepStartTime = stepExecution.getStartTime();
            LocalDateTime stepEndTime = stepExecution.getEndTime();
            System.out.println("Step [" + stepExecution.getStepName() + "] execution time: "
                    + Duration.between(stepStartTime, stepEndTime).toMillis() + " ms");
        }

    }
}