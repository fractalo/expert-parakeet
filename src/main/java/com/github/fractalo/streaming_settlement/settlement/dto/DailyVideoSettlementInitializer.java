package com.github.fractalo.streaming_settlement.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;


public record DailyVideoSettlementInitializer(
        @NotNull Long videoId,
        @NotNull LocalDate date,
        @NotNull @PositiveOrZero Long videoSettlementAmount,
        @NotNull @PositiveOrZero Long advertisementSettlementAmount
) {}
