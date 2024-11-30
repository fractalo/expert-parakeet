package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;


@SpringBatchTest
@SpringBootTest
class DailyVideoStatisticsStepTest {

    @Autowired
    private JpaPagingItemReader<DailyVideoStatisticsInput> dailyVideoStatisticsInputReader;

    @Autowired
    private JdbcPagingItemReader<DailyVideoStatisticsInput> dailyVideoStatisticsInputZeroOffsetReader;

    public StepExecution getStepExecution() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate("baseDate", LocalDate.of(2024, 11, 7))
                .toJobParameters();

        return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @Test
    public void testDailyVideoStatisticsInputReader() throws Exception {
        dailyVideoStatisticsInputReader.open(new ExecutionContext());
        int chunkSize = DailyVideoStatisticsStepConfig.CHUNK_SIZE;

        for (int j = 0; j < chunkSize; ++j) {
            DailyVideoStatisticsInput input = dailyVideoStatisticsInputReader.read();
            System.out.println("input = " + input);
        }

//        for (int i = 0; i < 100; ++i) {
//            for (int j = 0; j < chunkSize; ++j) {
//                dailyVideoStatisticsInputReader.read();
//            }
//        }
    }

    @Test
    public void testDailyVideoStatisticsInputZeroOffsetReader() throws Exception {
        dailyVideoStatisticsInputZeroOffsetReader.open(new ExecutionContext());

        int chunkSize = DailyVideoStatisticsStepConfig.CHUNK_SIZE;

        for (int j = 0; j < chunkSize; ++j) {
            DailyVideoStatisticsInput input = dailyVideoStatisticsInputZeroOffsetReader.read();
            System.out.println("input = " + input);
        }

//        for (int i = 0; i < 100; ++i) {
//            for (int j = 0; j < chunkSize; ++j) {
//                dailyVideoStatisticsInputZeroOffsetReader.read();
//            }
//        }

    }
}