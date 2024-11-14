package com.github.fractalo.streaming_settlement.settlement.dto;

public record DailyVideoStatisticsInput(
        Long videoId,
        Long viewCountIncrement,
        Long watchTimeMsIncrement,
        Long yesterdayWeeklyViewCount,
        Long yesterdayMonthlyViewCount,
        Long yesterdayWeeklyWatchTimeMs,
        Long yesterdayMonthlyWatchTimeMs
) { }
