package com.github.fractalo.streaming_settlement.domain;

import com.github.fractalo.streaming_settlement.settlement.dto.DailyVideoStatisticsInput;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "unq_video_id_date", columnList = "video_id, date", unique = true),
        @Index(name = "idx_view_count_increment", columnList = "view_count_increment"),
        @Index(name = "idx_weekly_view_count", columnList = "weekly_view_count"),
        @Index(name = "idx_monthly_view_count", columnList = "monthly_view_count"),
        @Index(name = "idx_watch_time_increment_ms", columnList = "watch_time_increment_ms"),
        @Index(name = "idx_weekly_watch_time_ms", columnList = "weekly_watch_time_ms"),
        @Index(name = "idx_monthly_watch_time_ms", columnList = "monthly_watch_time_ms"),
})
public class DailyVideoStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_video_statistics_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @NotNull
    private LocalDate date;

    @NotNull
    @PositiveOrZero
    private Long viewCountIncrement;

    @NotNull
    @PositiveOrZero
    private Long weeklyViewCount;

    @NotNull
    @PositiveOrZero
    private Long monthlyViewCount;

    @NotNull
    @PositiveOrZero
    private Long watchTimeIncrementMs;

    @NotNull
    @PositiveOrZero
    private Long weeklyWatchTimeMs;

    @NotNull
    @PositiveOrZero
    private Long monthlyWatchTimeMs;


    public DailyVideoStatistics(Video video, LocalDate date, DailyVideoStatisticsInput input) {
        this.video = video;
        this.date = date;
        this.viewCountIncrement = input.viewCountIncrement();
        this.watchTimeIncrementMs = input.watchTimeIncrementMs();

        if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
            this.weeklyViewCount = input.viewCountIncrement();
            this.weeklyWatchTimeMs = input.watchTimeIncrementMs();
        } else {
            this.weeklyViewCount = input.yesterdayWeeklyViewCount() + input.viewCountIncrement();
            this.weeklyWatchTimeMs = input.yesterdayWeeklyWatchTimeMs() + input.watchTimeIncrementMs();
        }

        if (date.getDayOfMonth() == 1) {
            this.monthlyViewCount = input.viewCountIncrement();
            this.monthlyWatchTimeMs = input.watchTimeIncrementMs();
        } else {
            this.monthlyViewCount = input.yesterdayMonthlyViewCount() + input.viewCountIncrement();
            this.monthlyWatchTimeMs = input.yesterdayMonthlyWatchTimeMs() + input.watchTimeIncrementMs();
        }
    }

}
