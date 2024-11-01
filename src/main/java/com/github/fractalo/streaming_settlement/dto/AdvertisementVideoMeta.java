package com.github.fractalo.streaming_settlement.dto;

import com.github.fractalo.streaming_settlement.domain.AdvertisementVideo;
import com.github.fractalo.streaming_settlement.domain.Video;

import java.util.List;

public record AdvertisementVideoMeta(
        Long id,
        String title,
        Long durationMs
) {
    public AdvertisementVideoMeta(AdvertisementVideo video) {
        this(
                video.getId(),
                video.getTitle(),
                video.getDurationMs()
        );
    }
}
