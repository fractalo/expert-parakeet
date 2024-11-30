package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.Video;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @EntityGraph(attributePaths = {"uploader"})
    Optional<Video> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.id = :id")
    Optional<Video> findByIdWithLock(Long id);

    Page<Video> findByCreatedAtBefore(Instant createdAt, Pageable pageable);

    Optional<Video> findFirstByCreatedAtBeforeOrderByIdAsc(Instant createdAt);
    Optional<Video> findFirstByCreatedAtBeforeOrderByIdDesc(Instant createdAt);

    Page<Video> findByIdBetween(Long minId, Long maxId, Pageable pageable);
}
