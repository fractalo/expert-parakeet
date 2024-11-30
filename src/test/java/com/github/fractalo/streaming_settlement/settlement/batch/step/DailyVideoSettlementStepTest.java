package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInput;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput;
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
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
class DailyVideoSettlementStepTest {
    @Autowired
    private JpaPagingItemReader<DailyVideoSettlementInput> dailyVideoSettlementInputReader;

    @Autowired
    private JdbcPagingItemReader<DailyVideoSettlementInput> dailyVideoSettlementInputZeroOffsetReader;

    public StepExecution getStepExecution() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate("baseDate", LocalDate.of(2024, 11, 7))
                .toJobParameters();

        return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @Test
    public void testDailyVideoSettlementInputReader() throws Exception {
        dailyVideoSettlementInputReader.open(new ExecutionContext());
        int chunkSize = DailyVideoStatisticsStepConfig.CHUNK_SIZE;

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < chunkSize; ++j) {
                dailyVideoSettlementInputReader.read();
            }
        }
    }

    @Test
    public void testDailyVideoSettlementInputZeroOffsetReader() throws Exception {
        dailyVideoSettlementInputZeroOffsetReader.open(new ExecutionContext());

        int chunkSize = DailyVideoStatisticsStepConfig.CHUNK_SIZE;

        for (int i = 0; i < 100; ++i) {
            for (int j = 0; j < chunkSize; ++j) {
                dailyVideoSettlementInputZeroOffsetReader.read();
            }
        }

    }

}