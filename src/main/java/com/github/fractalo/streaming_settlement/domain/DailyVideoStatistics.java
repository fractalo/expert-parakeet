package com.github.fractalo.streaming_settlement.domain;

import com.github.fractalo.streaming_settlement.settlement.DailyVideoStatisticsInput;
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
        @Index(name = "unq_video_id_date", columnList = "video_id, date", unique = true)
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
    private Long watchTimeMsIncrement;

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
        this.watchTimeMsIncrement = input.watchTimeMsIncrement();

        if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
            this.weeklyViewCount = input.viewCountIncrement();
            this.weeklyWatchTimeMs = input.watchTimeMsIncrement();
        } else {
            this.weeklyViewCount = input.yesterdayWeeklyViewCount() + input.viewCountIncrement();
            this.weeklyWatchTimeMs = input.yesterdayWeeklyWatchTimeMs() + input.watchTimeMsIncrement();
        }

        if (date.getDayOfMonth() == 1) {
            this.monthlyViewCount = input.viewCountIncrement();
            this.monthlyWatchTimeMs = input.watchTimeMsIncrement();
        } else {
            this.monthlyViewCount = input.yesterdayMonthlyViewCount() + input.viewCountIncrement();
            this.monthlyWatchTimeMs = input.yesterdayMonthlyWatchTimeMs() + input.watchTimeMsIncrement();
        }
    }

}
