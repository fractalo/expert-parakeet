package com.github.fractalo.streaming_settlement.settlement;

import com.github.fractalo.streaming_settlement.domain.DailyVideoSettlement;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.DailyVideoSettlementRepository;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DailyVideoSettlementStepConfig {

    private static final int CHUNK_SIZE = 50;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final DailyVideoSettlementRepository dailyVideoSettlementRepository;

    @Bean
    public Step dailyVideoSettlementStep() {
        return new StepBuilder("dailyVideoSettlementStep", jobRepository)
                .<DailyVideoSettlementInput, DailyVideoSettlement>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dailyVideoSettlementInputReader(null))
                .processor(dailyVideoSettlementProcessor(null))
                .writer(dailyVideoSettlementWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DailyVideoSettlementInput> dailyVideoSettlementInputReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JpaPagingItemReaderBuilder<DailyVideoSettlementInput>()
                .name("dailyVideoSettlementInputReader")
                .queryString("""
                        SELECT new com.github.fractalo.streaming_settlement.settlement.DailyVideoSettlementInput(
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
                        ORDER BY v.id ASC
                        """)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(Map.of(
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1),
                        "startOfNextDay", startOfNextDay
                ))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<DailyVideoSettlementInput, DailyVideoSettlement> dailyVideoSettlementProcessor(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        ViewCountRangeBasedAmountCalculator videoSettlementAmountCalculator =
                new ViewCountRangeBasedAmountCalculator(List.of(
                        new ViewCountUnitPriceRange(1, 1),
                        new ViewCountUnitPriceRange(10_0000, 1.1),
                        new ViewCountUnitPriceRange(50_0000, 1.3),
                        new ViewCountUnitPriceRange(100_0000, 1.5)
                ));

        ViewCountRangeBasedAmountCalculator adVideoSettlementAmountCalculator =
                new ViewCountRangeBasedAmountCalculator(List.of(
                        new ViewCountUnitPriceRange(1, 10),
                        new ViewCountUnitPriceRange(10_0000, 12),
                        new ViewCountUnitPriceRange(50_0000, 15),
                        new ViewCountUnitPriceRange(100_0000, 20)
                ));

        return settlementInput -> {
            long videoSettlementAmount = (long) videoSettlementAmountCalculator.calculate(
                    settlementInput.yesterdayViewCount(), settlementInput.todayViewCount()
            );
            long adVideoSettlementAmount = (long) adVideoSettlementAmountCalculator.calculate(
                    settlementInput.yesterdayAdVideoViewCount(), settlementInput.todayAdVideoViewCount()
            );
            Video videoReference = videoRepository.getReferenceById(settlementInput.videoId());
            return new DailyVideoSettlement(videoReference, baseDate, videoSettlementAmount, adVideoSettlementAmount);
        };
    }

    @Bean
    public RepositoryItemWriter<DailyVideoSettlement> dailyVideoSettlementWriter() {
        return new RepositoryItemWriterBuilder<DailyVideoSettlement>()
                .repository(dailyVideoSettlementRepository)
                .methodName("save")
                .build();
    }
}
