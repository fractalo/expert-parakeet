package com.github.fractalo.streaming_settlement.settlement;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ViewCountRangeBasedFeeCalculatorTest {

    @Test
    void calculatesAsExpected() {
        //given
        ViewCountRangeBasedFeeCalculator calculator = new ViewCountRangeBasedFeeCalculator(List.of(
                new ViewCountUnitPriceRange(1, 1),
                new ViewCountUnitPriceRange(10_0000, 1.1),
                new ViewCountUnitPriceRange(50_0000, 1.3),
                new ViewCountUnitPriceRange(100_0000, 1.5)
        ));

        //when
        long fee = (long) calculator.calculate(55_0000, 115_0000);

        //then
        Assertions.assertThat(fee).isEqualTo(81_0000);
    }

}