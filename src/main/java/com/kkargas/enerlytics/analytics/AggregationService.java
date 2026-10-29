package com.kkargas.enerlytics.analytics;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AggregationService {

    private final JdbcTemplate jdbcTemplate;

    public AggregationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Retain the existing API for the dashboard and previously written tests.
    public List<AggregationResult> aggregate(
            Long datasetId, TimeGrouping grouping, AggregationType type) {
        return aggregate(datasetId, grouping, type, null, null);
    }

    // Inclusive date interval. Null boundaries mean no restriction.
    public List<AggregationResult> aggregate(
            Long datasetId,
            TimeGrouping grouping,
            AggregationType type,
            LocalDate dateFrom,
            LocalDate dateTo) {

        Objects.requireNonNull(datasetId, "datasetId");
        Objects.requireNonNull(grouping, "grouping");
        Objects.requireNonNull(type, "type");

        if (datasetId <= 0) {
            throw new IllegalArgumentException("Dataset ID must be positive");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Date From must not be after Date To");
        }

        String expression = getGroupExpression(grouping);
        String function = type.name(); // enum, not raw user SQL

        StringBuilder sql = new StringBuilder("""
                SELECT
                    %s AS group_key,
                    COUNT(*) AS measurement_count,
                    %s(kitchen_wh) AS kitchen,
                    %s(laundry_wh) AS laundry,
                    %s(heating_wh) AS heating
                FROM energy_measurements
                WHERE dataset_id = ?
                """.formatted(expression, function, function, function));

        java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
        parameters.add(datasetId);

        // measured_at contains UTC-referenced epoch milliseconds.
        // dateTo is inclusive in the UI, therefore SQL compares against
        // midnight of the NEXT day as an exclusive upper bound.
        if (dateFrom != null) {
            sql.append(" AND measured_at >= ?");
            parameters.add(dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        if (dateTo != null) {
            sql.append(" AND measured_at < ?");
            parameters.add(dateTo.plusDays(1).atStartOfDay()
                    .toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        sql.append(" GROUP BY group_key ORDER BY group_key");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new AggregationResult(
                        getPeriodLabel(grouping, rs.getInt("group_key")),
                        rs.getLong("measurement_count"),
                        rs.getDouble("kitchen"),
                        rs.getDouble("laundry"),
                        rs.getDouble("heating")),
                parameters.toArray());
    }

    private String getGroupExpression(TimeGrouping grouping) {
        String month = "CAST(strftime('%m', measured_at / 1000, 'unixepoch') AS INTEGER)";
        String weekday = "CAST(strftime('%w', measured_at / 1000, 'unixepoch') AS INTEGER)";
        String hour = "CAST(strftime('%H', measured_at / 1000, 'unixepoch') AS INTEGER)";

        return switch (grouping) {
            case MONTH -> month;
            case SEASON -> """
                    CASE
                        WHEN %s IN (12, 1, 2) THEN 1
                        WHEN %s IN (3, 4, 5) THEN 2
                        WHEN %s IN (6, 7, 8) THEN 3
                        ELSE 4
                    END
                    """.formatted(month, month, month);
            case DAY_OF_WEEK -> """
                    CASE WHEN %s = 0 THEN 7 ELSE %s END
                    """.formatted(weekday, weekday);
            case DAY_PERIOD -> """
                    CASE
                        WHEN %s >= 21 OR %s < 5 THEN 1
                        WHEN %s < 9 THEN 2
                        WHEN %s < 13 THEN 3
                        WHEN %s < 17 THEN 4
                        ELSE 5
                    END
                    """.formatted(hour, hour, hour, hour, hour);
        };
    }

    private String getPeriodLabel(TimeGrouping grouping, int key) {
        String[] labels = switch (grouping) {
            case SEASON -> new String[] {"Winter", "Spring", "Summer", "Autumn"};
            case MONTH -> new String[] {
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"};
            case DAY_OF_WEEK -> new String[] {
                    "Monday", "Tuesday", "Wednesday", "Thursday",
                    "Friday", "Saturday", "Sunday"};
            case DAY_PERIOD -> new String[] {
                    "Night", "Early Morning", "Morning", "Afternoon", "Evening"};
        };
        if (key < 1 || key > labels.length) {
            throw new IllegalStateException("Unexpected grouping key: " + key);
        }
        return labels[key - 1];
    }
}
