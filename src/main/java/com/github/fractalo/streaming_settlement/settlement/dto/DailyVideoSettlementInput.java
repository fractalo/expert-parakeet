package com.github.fractalo.streaming_settlement.settlement.dto;

public record DailyVideoSettlementInput(
        Long videoId,
        Long todayViewCount,
        Long todayAdVideoViewCount,
        Long yesterdayViewCount,
        Long yesterdayAdVideoViewCount
) { }
