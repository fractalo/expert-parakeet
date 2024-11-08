package com.github.fractalo.streaming_settlement.settlement;

import com.github.fractalo.streaming_settlement.domain.DailyVideoStatistics;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.repository.DailyVideoStatisticsRepository;
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
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DailyVideoStatisticsStepConfig {

    private static final int CHUNK_SIZE = 50;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final VideoRepository videoRepository;
    private final SettlementConst settlementConst;
    private final DailyVideoStatisticsRepository dailyVideoStatisticsRepository;

    @Bean
    public Step dailyVideoStatisticsStep() {
        return new StepBuilder("dailyVideoStatisticsStep", jobRepository)
                .<DailyVideoStatisticsInput, DailyVideoStatistics>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dailyVideoStatisticsInputReader(null))
                .processor(dailyVideoStatisticsProcessor(null))
                .writer(dailyVideoStatisticsWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DailyVideoStatisticsInput> dailyVideoStatisticsInputReader(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        Instant startOfDay = baseDate.atStartOfDay(settlementConst.ZONE_ID).toInstant();
        Instant startOfNextDay = baseDate.plusDays(1).atStartOfDay(settlementConst.ZONE_ID).toInstant();

        return new JpaPagingItemReaderBuilder<DailyVideoStatisticsInput>()
                .name("dailyVideoStatisticsInputReader")
                .queryString("""
                        SELECT new com.github.fractalo.streaming_settlement.settlement.DailyVideoStatisticsInput(
                            v.id,
                            COALESCE(today_vms.viewCount - yesterday_vms.viewCount, today_vms.viewCount),
                            COALESCE(SUM(vwh.watchTimeMs), 0),
                            COALESCE(yesterday_vs.weeklyViewCount, 0),
                            COALESCE(yesterday_vs.monthlyViewCount, 0),
                            COALESCE(yesterday_vs.weeklyWatchTimeMs, 0),
                            COALESCE(yesterday_vs.monthlyWatchTimeMs, 0)
                        )
                        FROM Video v
                            LEFT JOIN VideoWatchHistory vwh
                                ON vwh.video.id = v.id
                                AND vwh.viewStartedAt >= :startOfDay AND vwh.viewStartedAt < :startOfNextDay
                            LEFT JOIN DailyVideoMetricsSnapshot today_vms
                                ON today_vms.video.id = v.id
                                AND today_vms.date = :today
                            LEFT JOIN DailyVideoMetricsSnapshot yesterday_vms
                                ON yesterday_vms.video.id = v.id
                                AND yesterday_vms.date = :yesterday
                            LEFT JOIN DailyVideoStatistics yesterday_vs
                                ON yesterday_vs.video.id = v.id
                                AND yesterday_vs.date = :yesterday
                        WHERE v.createdAt < :startOfNextDay
                        GROUP BY v.id
                        ORDER BY v.id ASC
                        """)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .parameterValues(Map.of(
                        "startOfDay", startOfDay,
                        "startOfNextDay", startOfNextDay,
                        "today", baseDate,
                        "yesterday", baseDate.minusDays(1)
                ))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<DailyVideoStatisticsInput, DailyVideoStatistics> dailyVideoStatisticsProcessor(
            @Value("#{jobParameters[baseDate]}") LocalDate baseDate
    ) {
        return statisticsInput -> {
            Video videoReference = videoRepository.getReferenceById(statisticsInput.videoId());
            return new DailyVideoStatistics(videoReference, baseDate, statisticsInput);
        };
    }

    @Bean
    public RepositoryItemWriter<DailyVideoStatistics> dailyVideoStatisticsWriter() {
        return new RepositoryItemWriterBuilder<DailyVideoStatistics>()
                .repository(dailyVideoStatisticsRepository)
                .methodName("save")
                .build();
    }
}
