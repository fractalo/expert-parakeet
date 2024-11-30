package com.github.fractalo.streaming_settlement.settlement.batch.step;

import com.github.fractalo.streaming_settlement.domain.DailyVideoSettlement;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.DailyVideoSettlementRepository;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInitializer;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInput;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import com.github.fractalo.streaming_settlement.settlement.calculator.ViewCountRangeBasedAmountCalculator;
import com.github.fractalo.streaming_settlement.settlement.calculator.ViewCountUnitPriceRange;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInitializer;
import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DailyVideoSettlementStepConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final DailyVideoSettlementRepository dailyVideoSettlementRepository;
    private final DataSource dataSource;

    private final ViewCountRangeBasedAmountCalculator videoSettlementAmountCalculator =
            new ViewCountRangeBasedAmountCalculator(List.of(
                    new ViewCountUnitPriceRange(1, 1),
                    new ViewCountUnitPriceRange(10_0000, 1.1),
                    new ViewCountUnitPriceRange(50_0000, 1.3),
                    new ViewCountUnitPriceRange(100_0000, 1.5)
            ));

    private final ViewCountRangeBasedAmountCalculator adVideoSettlementAmountCalculator =
            new ViewCountRangeBasedAmountCalculator(List.of(
                    new ViewCountUnitPriceRange(1, 10),
                    new ViewCountUnitPriceRange(10_0000, 12),
                    new ViewCountUnitPriceRange(50_0000, 15),
                    new ViewCountUnitPriceRange(100_0000, 20)
            ));

    @Bean
    public Step dailyVideoSettlementStep() {
        return new StepBuilder("dailyVideoSettlementStep", jobRepository)
                .<DailyVideoSettlementInput, DailyVideoSettlementInitializer>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dailyVideoSettlementInputZeroOffsetReader(null))
                .processor(dailyVideoSettlementInitProcessor(null))
                .writer(dailyVideoSettlementBatchWriter())
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
                        SELECT new com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoSettlementInput(
                            v.id,
                            today_vms.viewCount,
                            today_vms.advertisementVideoViewCount,
                            COALESCE(yesterday_vms.viewCount, 0),
                            COALESCE(yesterday_vms.advertisementVideoViewCount, 0)
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
    public JdbcPagingItemReader<DailyVideoSettlementInput> dailyVideoSettlementInputZeroOffsetReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JdbcPagingItemReaderBuilder<DailyVideoSettlementInput>()
                .name("dailyVideoSettlementInputZeroOffsetReader")
                .selectClause("""
                        v.video_id,
                        dvms_today.view_count AS todayViewCount,
                        dvms_today.advertisement_video_view_count AS todayAdVideoViewCount,
                        COALESCE(dvms_yesterday.view_count, 0) AS yesterdayViewCount,
                        COALESCE(dvms_yesterday.advertisement_video_view_count, 0) AS yesterdayAdVideoViewCount
                        """)
                .fromClause("""
                        video v
                        LEFT JOIN
                            daily_video_metrics_snapshot dvms_today
                            ON dvms_today.video_id = v.video_id
                            AND dvms_today.date = :today
                        LEFT JOIN
                            daily_video_metrics_snapshot dvms_yesterday
                            ON dvms_yesterday.video_id = v.video_id
                            AND dvms_yesterday.date = :yesterday
                        """)
                .whereClause("""
                        v.created_at < :startOfNextDay
                        """)
                .sortKeys(Map.of("v.video_id", Order.ASCENDING))
                .pageSize(CHUNK_SIZE)
                .dataSource(dataSource)
                .parameterValues(Map.of(
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1),
                        "startOfNextDay", startOfNextDay.toString()
                ))
                .rowMapper(new DataClassRowMapper<>(DailyVideoSettlementInput.class))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<DailyVideoSettlementInput, DailyVideoSettlement> dailyVideoSettlementProcessor(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {

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

    @Bean
    @StepScope
    public ItemProcessor<DailyVideoSettlementInput, DailyVideoSettlementInitializer> dailyVideoSettlementInitProcessor(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {

        return settlementInput -> {
            long videoSettlementAmount = (long) videoSettlementAmountCalculator.calculate(
                    settlementInput.yesterdayViewCount(), settlementInput.todayViewCount()
            );
            long adVideoSettlementAmount = (long) adVideoSettlementAmountCalculator.calculate(
                    settlementInput.yesterdayAdVideoViewCount(), settlementInput.todayAdVideoViewCount()
            );
            return new DailyVideoSettlementInitializer(settlementInput.videoId(), baseDate, videoSettlementAmount, adVideoSettlementAmount);
        };
    }

    @Bean
    public JdbcBatchItemWriter<DailyVideoSettlementInitializer> dailyVideoSettlementBatchWriter() {
        return new JdbcBatchItemWriterBuilder<DailyVideoSettlementInitializer>()
                .sql("""
                        INSERT INTO daily_video_settlement(
                            video_id, date, video_settlement_amount, advertisement_settlement_amount
                        ) VALUES (
                            :videoId, :date, :videoSettlementAmount, :advertisementSettlementAmount
                        )
                        """)
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }
}
