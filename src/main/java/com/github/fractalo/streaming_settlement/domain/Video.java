package com.github.fractalo.streaming_settlement.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long id;

    @NotNull
    private String title;

    @NotNull
    @PositiveOrZero
    private Long viewCount = 0L;

    @NotNull
    @PositiveOrZero
    private Long advertisementVideoViewCount = 0L;

    @NotNull
    @PositiveOrZero
    private Long durationMs;

    @NotNull
    @CreationTimestamp
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "member_id")
    private Member uploader;

    public void increaseViewCount() {
        ++viewCount;
    }

    public void increaseAdVideoViewCount() {
        ++advertisementVideoViewCount;
    }

}
