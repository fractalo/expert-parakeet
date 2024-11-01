package com.github.fractalo.streaming_settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record VideoWatchingContext(
        @NotNull String videoPlayerUUID,
        @PositiveOrZero Long playbackPositionMs,
        @NotNull Boolean isStopped
) { }
