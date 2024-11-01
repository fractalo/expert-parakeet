package com.github.fractalo.streaming_settlement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "idx_video_id_advertisement_video_id", columnList = "video_id, advertisement_video_id", unique = true),
})
public class AdvertisementVideoExposure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advertisement_video_exposure_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_video_id")
    private AdvertisementVideo advertisementVideo;

    @NotNull
    @PositiveOrZero
    private Long viewCount = 0L;

    public AdvertisementVideoExposure(Video video, AdvertisementVideo advertisementVideo) {
        this.video = video;
        this.advertisementVideo = advertisementVideo;
    }

    public void increaseViewCount() {
        ++viewCount;
    }
}
