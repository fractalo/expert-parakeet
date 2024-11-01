package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.AdvertisementVideoExposure;
import com.github.fractalo.streaming_settlement.domain.Video;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdvertisementVideoExposureRepository extends JpaRepository<AdvertisementVideoExposure, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ave
        from AdvertisementVideoExposure ave join fetch ave.video
        where ave.video.id = :videoId and ave.advertisementVideo.id = :adVideoId
    """)
    Optional<AdvertisementVideoExposure> findByVideoIdAndAdvertisementVideoIdWithLock(Long videoId, Long adVideoId);

    Optional<AdvertisementVideoExposure> findByVideoIdAndAdvertisementVideoId(Long videoId, Long adVideoId);
}
