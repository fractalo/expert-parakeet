package com.github.fractalo.streaming_settlement.settlement.calculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ViewCountRangeBasedAmountCalculator {
    private final List<ViewCountUnitPriceRange> ranges;

    public double calculate(long processedViewCount, long viewCount) {
        Iterator<ViewCountUnitPriceRange> iterator = ranges.iterator();
        if (!iterator.hasNext()) {
            return 0;
        }

        double totalSettlementAmount = 0;
        ViewCountUnitPriceRange currentRange = iterator.next();

        processedViewCount = Math.max(currentRange.minViewCount() - 1, processedViewCount);

        while (processedViewCount < viewCount) {
            double unitPrice = currentRange.unitPrice();
            final long rangeViewCount;

            if (iterator.hasNext()) {
                ViewCountUnitPriceRange nextRange = iterator.next();
                rangeViewCount = Math.min(nextRange.minViewCount() - 1, viewCount);
                currentRange = nextRange;
            } else {
                rangeViewCount = viewCount;
            }

            if (rangeViewCount > processedViewCount) {
                double settlementAmount = (rangeViewCount - processedViewCount) * unitPrice;
                totalSettlementAmount += settlementAmount;
                processedViewCount = rangeViewCount;
            }
        }

        return totalSettlementAmount;
    }

}
