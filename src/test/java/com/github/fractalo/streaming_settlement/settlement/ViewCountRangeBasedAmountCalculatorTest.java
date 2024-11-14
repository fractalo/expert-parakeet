package com.github.fractalo.streaming_settlement.settlement;

import com.github.fractalo.streaming_settlement.settlement.calculator.ViewCountRangeBasedAmountCalculator;
import com.github.fractalo.streaming_settlement.settlement.calculator.ViewCountUnitPriceRange;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ViewCountRangeBasedAmountCalculatorTest {

    @Test
    void calculatesAsExpected() {
        //given
        ViewCountRangeBasedAmountCalculator calculator = new ViewCountRangeBasedAmountCalculator(List.of(
                new ViewCountUnitPriceRange(1, 1),
                new ViewCountUnitPriceRange(10_0000, 1.1),
                new ViewCountUnitPriceRange(50_0000, 1.3),
                new ViewCountUnitPriceRange(100_0000, 1.5)
        ));

        //when
        long amount = (long) calculator.calculate(55_0000, 115_0000);

        //then
        Assertions.assertThat(amount).isEqualTo(81_0000);
    }

}