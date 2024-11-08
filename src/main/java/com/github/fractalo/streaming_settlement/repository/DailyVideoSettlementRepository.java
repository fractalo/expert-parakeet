package com.github.fractalo.streaming_settlement.repository;

import com.github.fractalo.streaming_settlement.domain.DailyVideoSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyVideoSettlementRepository extends JpaRepository<DailyVideoSettlement, Long> {
}
