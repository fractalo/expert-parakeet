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
public class DailyVideoSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_video_settlement_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @NotNull
    private LocalDate date;

    @NotNull
    @PositiveOrZero
    private Long videoSettlementAmount;

    @NotNull
    @PositiveOrZero
    private Long advertisementSettlementAmount;

    public DailyVideoSettlement(Video video, LocalDate date, Long videoSettlementAmount, Long advertisementSettlementAmount) {
        this.video = video;
        this.date = date;
        this.videoSettlementAmount = videoSettlementAmount;
        this.advertisementSettlementAmount = advertisementSettlementAmount;
    }

}
