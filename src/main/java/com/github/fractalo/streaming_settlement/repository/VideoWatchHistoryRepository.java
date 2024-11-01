package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.VideoWatchHistory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface VideoWatchHistoryRepository extends JpaRepository<VideoWatchHistory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VideoWatchHistory> findFirstByMemberIdOrderByIdDesc(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VideoWatchHistory> findFirstByIpAddressOrderByIdDesc(String ipAddress);

    Optional<VideoWatchHistory> findFirstByMemberIdAndVideoIdOrderByIdDesc(Long memberId, Long videoId);
}
