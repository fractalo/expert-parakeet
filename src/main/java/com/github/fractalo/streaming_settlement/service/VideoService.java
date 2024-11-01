package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.Member;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.domain.VideoWatchHistory;
import com.github.fractalo.streaming_settlement.dto.SimpleMember;
import com.github.fractalo.streaming_settlement.dto.VideoMeta;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.repository.VideoWatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final AdvertisementVideoService advertisementVideoService;
    private final VideoWatchHistoryRepository videoWatchHistoryRepository;

    @Transactional(readOnly = true)
    public VideoMeta getVideoMeta(Long videoId, Long memberId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Long playbackPositionMs = getLastPlaybackPositionMs(videoId, memberId);

        SimpleMember uploader = new SimpleMember(video.getUploader());

        List<Long> positionMsList = advertisementVideoService.createPositionMsList(video);

        return new VideoMeta(video, uploader, positionMsList, playbackPositionMs);
    }

    @Transactional
    public void increaseViewCount(Long videoId, Long memberId, String ipAddress) {
        Video video = videoRepository.findByIdWithLock(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (video.getUploader().getId().equals(memberId)) {
            throw new IllegalArgumentException("cannot increase view count of own video");
        }

        video.increaseViewCount();
    }

    @Transactional(readOnly = true)
    public Long getLastPlaybackPositionMs(Long videoId, Long memberId) {
        if (videoId == null || memberId == null) {
            return null;
        }
        return videoWatchHistoryRepository.findFirstByMemberIdAndVideoIdOrderByIdDesc(memberId, videoId)
                .map(VideoWatchHistory::getLastPlaybackPositionMs)
                .orElse(null);
    }


}
