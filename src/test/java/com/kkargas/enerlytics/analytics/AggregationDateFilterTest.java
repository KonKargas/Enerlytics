package com.kkargas.enerlytics.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

class AggregationDateFilterTest {

    @TempDir
    Path tempDirectory;

    private AggregationService service;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDirectory.resolve("filter-test.db"));
        jdbc = new JdbcTemplate(dataSource);
        service = new AggregationService(jdbc);
        jdbc.execute("""
                CREATE TABLE energy_measurements (
                    dataset_id INTEGER,
                    measured_at INTEGER,
                    kitchen_wh REAL,
                    laundry_wh REAL,
                    heating_wh REAL
                )
                """);

        insert(LocalDateTime.of(2007, 1, 1, 0, 0), 10);
        insert(LocalDateTime.of(2007, 1, 31, 23, 59), 20);
        insert(LocalDateTime.of(2007, 2, 1, 0, 0), 30);
        insert(LocalDateTime.of(2008, 1, 1, 0, 0), 40);
    }

    private void insert(LocalDateTime dateTime, double kitchen) {
        jdbc.update("""
                INSERT INTO energy_measurements
                (dataset_id, measured_at, kitchen_wh, laundry_wh, heating_wh)
                VALUES (?, ?, ?, ?, ?)
                """, 1L, dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
                kitchen, 0, 0);
    }

    @Test
    void shouldIncludeFullLastDay() {
        List<AggregationResult> rows = service.aggregate(
                1L, TimeGrouping.MONTH, AggregationType.SUM,
                LocalDate.of(2007, 1, 1), LocalDate.of(2007, 1, 31));

        assertEquals(1, rows.size());
        assertEquals("January", rows.get(0).period());
        assertEquals(2, rows.get(0).measurementCount());
        assertEquals(30, rows.get(0).kitchen(), 0.001);
    }

    @Test
    void shouldPreserveUnfilteredBehavior() {
        List<AggregationResult> rows = service.aggregate(
                1L, TimeGrouping.MONTH, AggregationType.SUM);
        assertEquals(2, rows.size());
        assertEquals(3, rows.get(0).measurementCount());
        assertEquals(70, rows.get(0).kitchen(), 0.001);
    }

    @Test
    void shouldRejectReversedRange() {
        assertThrows(IllegalArgumentException.class, () -> service.aggregate(
                1L, TimeGrouping.MONTH, AggregationType.SUM,
                LocalDate.of(2008, 1, 1), LocalDate.of(2007, 1, 1)));
    }
}
