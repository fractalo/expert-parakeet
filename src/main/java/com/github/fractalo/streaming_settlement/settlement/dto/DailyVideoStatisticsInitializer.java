package com.github.fractalo.streaming_settlement.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Getter
public class DailyVideoStatisticsInitializer {

    @NotNull
    private final Long videoId;

    @NotNull
    private final LocalDate date;

    @NotNull
    @PositiveOrZero
    private final Long viewCountIncrement;

    @NotNull
    @PositiveOrZero
    private final Long weeklyViewCount;

    @NotNull
    @PositiveOrZero
    private final Long monthlyViewCount;

    @NotNull
    @PositiveOrZero
    private final Long watchTimeIncrementMs;

    @NotNull
    @PositiveOrZero
    private final Long weeklyWatchTimeMs;

    @NotNull
    @PositiveOrZero
    private final Long monthlyWatchTimeMs;

    public DailyVideoStatisticsInitializer(DailyVideoStatisticsInput input, LocalDate date) {
        this.videoId = input.videoId();
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
