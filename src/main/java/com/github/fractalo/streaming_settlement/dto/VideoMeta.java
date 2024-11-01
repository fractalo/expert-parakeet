package com.github.fractalo.streaming_settlement.dto;

import com.github.fractalo.streaming_settlement.domain.Video;

import java.util.List;

public record VideoMeta(
        Long id,
        String title,
        Long viewCount,
        Long durationMs,
        Long createdAt,
        SimpleMember uploader,
        List<Long> advertisementVideoPositionMsList,
        Long playbackPositionMs
) {
    public VideoMeta(Video video,
                     SimpleMember uploader,
                     List<Long> advertisementVideoPositionMsList,
                     Long playbackPositionMs) {
        this(
                video.getId(),
                video.getTitle(),
                video.getViewCount(),
                video.getDurationMs(),
                video.getCreatedAt().toEpochMilli(),
                uploader,
                advertisementVideoPositionMsList,
                playbackPositionMs
        );
    }
}
