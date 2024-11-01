package com.github.fractalo.streaming_settlement.dto;

import jakarta.validation.constraints.NotNull;

public record AdvertisementVideoWatchingContext(
        @NotNull Long videoId,
        @NotNull Long advertisementPositionMs,
        @NotNull String videoPlayerUUID
) { }
