package com.github.fractalo.streaming_settlement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "unq_video_id_date", columnList = "video_id, date", unique = true)
})
public class DailyVideoMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_video_metrics_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @NotNull
    private LocalDate date;

    @NotNull
    @PositiveOrZero
    private Long viewCountIncrement = 0L;

    @NotNull
    @PositiveOrZero
    private Long cumulativeViewCount;

    @NotNull
    @PositiveOrZero
    private Long advertisementVideoViewCountIncrement = 0L;

    @NotNull
    @PositiveOrZero
    private Long cumulativeAdvertisementVideoViewCount;


    public DailyVideoMetrics(Video video, LocalDate date) {
        this.video = video;
        this.date = date;
        cumulativeViewCount = video.getViewCount();
        cumulativeAdvertisementVideoViewCount = video.getAdvertisementVideoViewCount();
    }

    public void updateViewCount(Video video) {
        Long viewCount = video.getViewCount();
        if (this.video.getId().equals(video.getId())
                && cumulativeViewCount < viewCount) {
            long increment = viewCount - cumulativeViewCount;
            viewCountIncrement += increment;
            cumulativeViewCount = viewCount;
        }
    }

    public void updateAdVideoViewCount(Video video) {
        Long advertisementVideoViewCount = video.getAdvertisementVideoViewCount();
        if (this.video.getId().equals(video.getId())
                && cumulativeAdvertisementVideoViewCount < advertisementVideoViewCount) {
            long increment = advertisementVideoViewCount - cumulativeAdvertisementVideoViewCount;
            advertisementVideoViewCountIncrement += increment;
            cumulativeAdvertisementVideoViewCount = advertisementVideoViewCount;
        }
    }

}
