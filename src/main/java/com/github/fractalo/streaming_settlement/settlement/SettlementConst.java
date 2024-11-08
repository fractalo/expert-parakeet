package com.github.fractalo.streaming_settlement.settlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class SettlementConst {
    public final ZoneId ZONE_ID;

    public SettlementConst(@Value("${settlement.timezone}") String timezone) {
        ZONE_ID = ZoneId.of(timezone);
    }
}
