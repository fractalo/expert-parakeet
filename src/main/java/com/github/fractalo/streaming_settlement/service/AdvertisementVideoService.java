package com.github.fractalo.streaming_settlement.service;

import com.github.fractalo.streaming_settlement.domain.AdvertisementVideo;
import com.github.fractalo.streaming_settlement.domain.AdvertisementVideoExposure;
import com.github.fractalo.streaming_settlement.domain.DailyVideoMetrics;
import com.github.fractalo.streaming_settlement.domain.Video;
import com.github.fractalo.streaming_settlement.dto.AdvertisementVideoMeta;
import com.github.fractalo.streaming_settlement.dto.AdvertisementVideoWatchingContext;
import com.github.fractalo.streaming_settlement.repository.AdvertisementVideoExposureRepository;
import com.github.fractalo.streaming_settlement.repository.AdvertisementVideoRepository;
import com.github.fractalo.streaming_settlement.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@RequiredArgsConstructor
public class AdvertisementVideoService {
    private final VideoRepository videoRepository;
    private final AdvertisementVideoRepository adVideoRepository;
    private final AdvertisementVideoExposureRepository adVideoExposureRepository;
    private final DailyVideoMetricsService dailyVideoMetricsService;

    @Transactional
    public List<AdvertisementVideoMeta> getAdvertisementVideoMeta(AdvertisementVideoWatchingContext context) {
        Video video = videoRepository.findById(context.videoId())
                .orElseThrow(() -> new IllegalArgumentException("video not found"));

        int seed = Objects.hash(context.hashCode(), System.currentTimeMillis());
        Optional<AdvertisementVideo> adVideoOptional = getRandomAdvertisementVideo(seed);

        if (adVideoOptional.isEmpty()) {
            return new ArrayList<>();
        }

        AdvertisementVideo adVideo = adVideoOptional.get();

        adVideoExposureRepository
                .findByVideoIdAndAdvertisementVideoId(video.getId(), adVideo.getId())
                .orElseGet(() -> adVideoExposureRepository.save(new AdvertisementVideoExposure(video, adVideo)));

        return List.of(new AdvertisementVideoMeta(adVideo));
    }

    @Transactional
    public void increaseViewCount(Long adVideoId, Long videoId, Long memberId, String ipAddress) {
        AdvertisementVideoExposure adVideoExposure = adVideoExposureRepository
                .findByVideoIdAndAdvertisementVideoIdWithLock(videoId, adVideoId)
                .orElseThrow();

        Video video = adVideoExposure.getVideo();
        if (video.getUploader().getId().equals(memberId)) {
            throw new IllegalArgumentException("cannot increase view count of own video");
        }

        DailyVideoMetrics dailyVideoMetrics = dailyVideoMetricsService.createOrGetDailyVideoMetricsForUpdate(video);

        video.increaseAdVideoViewCount();
        adVideoExposure.increaseViewCount();
        dailyVideoMetrics.updateAdVideoViewCount(video);
    }

    @Transactional(readOnly = true)
    public Optional<AdvertisementVideo> getRandomAdvertisementVideo(long seed) {
        int count = (int) adVideoRepository.count();
        if (count == 0) return Optional.empty();

        int randomNumber = new Random(seed).nextInt();
        int randomIndex = randomNumber % count;
        Pageable pageable = PageRequest.of(randomIndex, 1);

        List<AdvertisementVideo> adVideos = adVideoRepository.findAll(pageable).getContent();
        return adVideos.isEmpty() ? Optional.empty() : Optional.of(adVideos.getFirst());
    }

    public List<Long> createPositionMsList(Video video) {
        Long durationMs = video.getDurationMs();
        final long positionIntervalMs = 5 * 60 * 1000;

        List<Long> positionMsList = new ArrayList<>();
        for (int i = 1; i <= durationMs / positionIntervalMs; ++i) {
            positionMsList.add(i * positionIntervalMs);
        }
        return positionMsList;
    }
}
