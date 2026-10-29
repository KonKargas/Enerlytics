
package com.kkargas.enerlytics.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

class AggregationCalculationTest {

    @TempDir
    Path tempDirectory;

    private JdbcTemplate jdbcTemplate;
    private AggregationService aggregationService;

    private static final Long DATASET_ID = 1L;

    @BeforeEach
    void setUp() {

        SQLiteDataSource dataSource = new SQLiteDataSource();

        dataSource.setUrl(
                "jdbc:sqlite:" +
                tempDirectory.resolve("analytics-test.db")
        );

        jdbcTemplate = new JdbcTemplate(dataSource);

        aggregationService =
                new AggregationService(jdbcTemplate);

        jdbcTemplate.execute("""
                CREATE TABLE energy_measurements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dataset_id INTEGER NOT NULL,
                    measured_at INTEGER NOT NULL,
                    kitchen_wh REAL NOT NULL,
                    laundry_wh REAL NOT NULL,
                    heating_wh REAL NOT NULL
                )
                """);

        insertMeasurement(
                LocalDateTime.of(2025, 1, 1, 10, 0),
                10, 20, 30
        );

        insertMeasurement(
                LocalDateTime.of(2025, 1, 1, 11, 0),
                30, 40, 50
        );

        insertMeasurement(
                LocalDateTime.of(2025, 2, 1, 22, 0),
                20, 10, 40
        );

        insertMeasurement(
                LocalDateTime.of(2025, 6, 1, 15, 0),
                40, 30, 20
        );
    }

    private void insertMeasurement(
            LocalDateTime timestamp,
            double kitchen,
            double laundry,
            double heating
    ) {

        long epochMillis = timestamp
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli();

        jdbcTemplate.update(
                """
                INSERT INTO energy_measurements (
                    dataset_id,
                    measured_at,
                    kitchen_wh,
                    laundry_wh,
                    heating_wh
                ) VALUES (?, ?, ?, ?, ?)
                """,
                DATASET_ID,
                epochMillis,
                kitchen,
                laundry,
                heating
        );
    }

    @Test
    void shouldCalculateMonthlySum() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.MONTH,
                        AggregationType.SUM
                );

        assertEquals(3, results.size());

        AggregationResult january = results.get(0);

        assertEquals("January", january.period());
        assertEquals(2, january.measurementCount());

        assertEquals(40.0, january.kitchen(), 0.001);
        assertEquals(60.0, january.laundry(), 0.001);
        assertEquals(80.0, january.heating(), 0.001);
    }

    @Test
    void shouldCalculateMonthlyAverage() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.MONTH,
                        AggregationType.AVG
                );

        AggregationResult january = results.get(0);

        assertEquals("January", january.period());

        assertEquals(20.0, january.kitchen(), 0.001);
        assertEquals(30.0, january.laundry(), 0.001);
        assertEquals(40.0, january.heating(), 0.001);
    }

    @Test
    void shouldCalculateSeasonalSum() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.SEASON,
                        AggregationType.SUM
                );

        assertEquals(2, results.size());

        AggregationResult winter = results.get(0);

        assertEquals("Winter", winter.period());
        assertEquals(3, winter.measurementCount());

        assertEquals(60.0, winter.kitchen(), 0.001);
        assertEquals(70.0, winter.laundry(), 0.001);
        assertEquals(120.0, winter.heating(), 0.001);

        AggregationResult summer = results.get(1);

        assertEquals("Summer", summer.period());
        assertEquals(1, summer.measurementCount());

        assertEquals(40.0, summer.kitchen(), 0.001);
        assertEquals(30.0, summer.laundry(), 0.001);
        assertEquals(20.0, summer.heating(), 0.001);
    }

    @Test
    void shouldGroupByDayPeriod() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.DAY_PERIOD,
                        AggregationType.SUM
                );

        assertEquals(3, results.size());

        AggregationResult night = results.get(0);
        AggregationResult morning = results.get(1);
        AggregationResult afternoon = results.get(2);

        assertEquals("Night", night.period());
        assertEquals(1, night.measurementCount());

        assertEquals("Morning", morning.period());
        assertEquals(2, morning.measurementCount());

        assertEquals("Afternoon", afternoon.period());
        assertEquals(1, afternoon.measurementCount());
    }

    @Test
    void shouldGroupByDayOfWeek() {

        List<AggregationResult> results =
                aggregationService.aggregate(
                        DATASET_ID,
                        TimeGrouping.DAY_OF_WEEK,
                        AggregationType.SUM
                );

        assertEquals(3, results.size());

        assertEquals("Wednesday", results.get(0).period());
        assertEquals("Saturday", results.get(1).period());
        assertEquals("Sunday", results.get(2).period());
    }

    @Test
    void shouldPreserveTotalMeasurementCount() {

        for (TimeGrouping grouping : TimeGrouping.values()) {

            List<AggregationResult> results =
                    aggregationService.aggregate(
                            DATASET_ID,
                            grouping,
                            AggregationType.SUM
                    );

            long total = results.stream()
                    .mapToLong(AggregationResult::measurementCount)
                    .sum();

            assertEquals(4, total);
        }
    }
}
