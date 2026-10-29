
package com.kkargas.enerlytics.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AggregationServiceTest {

    @Autowired
    private AggregationService aggregationService;

    private static final Long DATASET_ID = 1L;

    private static final long EXPECTED_RECORDS = 2_049_280L;

    @Test
    void shouldAggregateByMonth() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.MONTH,
                        AggregationType.SUM
                );

        assertEquals(12, results.size());
        assertEquals("January", results.get(0).period());
        assertEquals("December", results.get(11).period());

        assertEquals(EXPECTED_RECORDS, countMeasurements(results));

        assertTrue(results.stream().allMatch(
                r -> r.kitchen() >= 0
                        && r.laundry() >= 0
                        && r.heating() >= 0
        ));
    }

    @Test
    void shouldAggregateBySeason() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.SEASON,
                        AggregationType.SUM
                );

        assertEquals(4, results.size());
        assertEquals("Winter", results.get(0).period());
        assertEquals("Autumn", results.get(3).period());

        assertEquals(EXPECTED_RECORDS, countMeasurements(results));
    }

    @Test
    void shouldAggregateByDayOfWeek() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.DAY_OF_WEEK,
                        AggregationType.AVG
                );

        assertEquals(7, results.size());
        assertEquals("Monday", results.get(0).period());
        assertEquals("Sunday", results.get(6).period());

        assertEquals(EXPECTED_RECORDS, countMeasurements(results));
    }

    @Test
    void shouldAggregateByDayPeriod() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.DAY_PERIOD,
                        AggregationType.AVG
                );

        assertEquals(5, results.size());
        assertEquals("Night", results.get(0).period());
        assertEquals("Evening", results.get(4).period());

        assertEquals(EXPECTED_RECORDS, countMeasurements(results));
    }

    private long countMeasurements(
            List<AggregationResult> results
    ) {
        return results.stream()
                .mapToLong(AggregationResult::measurementCount)
                .sum();
    }
}
