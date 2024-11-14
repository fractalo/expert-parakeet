package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.Member;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.domain.VideoWatchHistory;
import com.github.fractalo.streaming_settlement.dto.VideoWatchingContext;
import com.github.fractalo.streaming_settlement.repository.MemberRepository;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import com.github.fractalo.streaming_settlement.repository.VideoWatchHistoryRepository;
import com.github.fractalo.streaming_settlement.settlement.constant.SettlementConst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoWatchHistoryService {
    private final VideoWatchHistoryRepository videoWatchHistoryRepository;
    private final VideoRepository videoRepository;
    private final MemberRepository memberRepository;
    private final SettlementConst settlementConst;

    @Transactional
    public void updateOrInsertWatchHistory(Long videoId, VideoWatchingContext context, Long memberId, String ipAddress) {
        Member member = memberRepository.findById(memberId)
                .orElse(null);
        if (member == null && (ipAddress == null || ipAddress.isBlank())) {
            throw new IllegalArgumentException("viewer identifier (member or ipAddress) is required");
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("videoId " + videoId + " not found"));

        VideoWatchHistory history = getLatestWatchHistory(memberId, ipAddress)
                .orElse(null);

        if (canUpdateHistory(history, videoId, context)) {
            if (isWithinSameSettlementDay(history)) {
                history.updateHistory(context.isStopped());
                history.setLastPlaybackPositionMs(context.playbackPositionMs());
            } else {
                VideoWatchHistory newHistory =
                        new VideoWatchHistory(history.getLastViewedAt(), video, member, ipAddress, context);
                newHistory.updateHistory(context.isStopped());
                videoWatchHistoryRepository.save(newHistory);
            }
        } else if (!context.isStopped()) {
            if (history != null) {
                history.updateHistory(true);
            }
            VideoWatchHistory newHistory = new VideoWatchHistory(video, member, ipAddress, context);
            videoWatchHistoryRepository.save(newHistory);
        }
    }

    private Optional<VideoWatchHistory> getLatestWatchHistory(Long memberId, String ipAddress) {
        if (memberId != null) {
            return videoWatchHistoryRepository.findFirstByMemberIdOrderByIdDesc(memberId);
        } else {
            return videoWatchHistoryRepository.findFirstByIpAddressOrderByIdDesc(ipAddress);
        }
    }

    private boolean canUpdateHistory(VideoWatchHistory history, Long videoId, VideoWatchingContext context) {
        return history != null
                && history.isActive()
                && history.getVideo().getId().equals(videoId)
                && history.getVideoPlayerUUID().equals(context.videoPlayerUUID());
    }

    private boolean isWithinSameSettlementDay(VideoWatchHistory history) {
        LocalDate startDate = history.getViewStartedAt().atZone(settlementConst.ZONE_ID).toLocalDate();
        LocalDate lastDate = history.getLastViewedAt().atZone(settlementConst.ZONE_ID).toLocalDate();
        return startDate.equals(lastDate);
    }
}
