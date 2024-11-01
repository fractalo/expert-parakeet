package com.github.fractalo.streaming_settlement.domain;

import com.github.fractalo.streaming_settlement.dto.VideoWatchingContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "idx_member_id_video_watch_history_id", columnList = "member_id, video_watch_history_id"),
        @Index(name = "idx_member_id_video_id_video_watch_history_id", columnList = "member_id, video_id, video_watch_history_id"),
        @Index(name = "idx_video_id_view_started_at", columnList = "video_id, view_started_at")
})
public class VideoWatchHistory {
    private static final long ACTIVE_WINDOW_MS = 60 * 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_watch_history_id")
    private Long id;

    @NotNull
    private Instant viewStartedAt = Instant.now();

    @Setter
    @NotNull
    private Instant lastViewedAt = viewStartedAt;

    @Setter
    @NotNull
    @PositiveOrZero
    private Long lastPlaybackPositionMs = 0L;

    @NotNull
    private Boolean isStopped = false;

    @NotNull
    @PositiveOrZero
    private Long watchTimeMs = 0L;

    @NotNull
    @Column(name = "video_player_uuid")
    private String videoPlayerUUID;

    private String ipAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    public VideoWatchHistory(Video video, Member member, String ipAddress, VideoWatchingContext context) {
        this.video = video;
        this.member = member;
        this.ipAddress = ipAddress;
        this.videoPlayerUUID = context.videoPlayerUUID();
        this.lastPlaybackPositionMs = context.playbackPositionMs();
    }

    public VideoWatchHistory(Instant viewStartedAt,
                             Video video, Member member, String ipAddress, VideoWatchingContext context) {
        this.lastViewedAt = this.viewStartedAt = viewStartedAt;
        this.video = video;
        this.member = member;
        this.ipAddress = ipAddress;
        this.videoPlayerUUID = context.videoPlayerUUID();
        this.lastPlaybackPositionMs = context.playbackPositionMs();
    }

    public boolean isActive() {
        return Duration.between(lastViewedAt, Instant.now()).toMillis() < ACTIVE_WINDOW_MS;
    }

    public void updateHistory(Boolean isStopped) {
        if (isStopped) {
            stopHistory();
        } else {
            if (this.isStopped) {
                restartHistory();
            } else {
                accumulateWatchTime();
            }
        }
    }

    private void incrementWatchTime(Long milliSeconds) {
        watchTimeMs += Math.min(milliSeconds, ACTIVE_WINDOW_MS);
    }

    private void stopHistory() {
        if (this.isStopped) return;
        accumulateWatchTime();
        this.isStopped = true;
    }

    private void accumulateWatchTime() {
        final Instant now = Instant.now();
        incrementWatchTime(Duration.between(lastViewedAt, now).toMillis());
        lastViewedAt = now;
    }

    private void restartHistory() {
        lastViewedAt = Instant.now();
        this.isStopped = false;
    }
}
