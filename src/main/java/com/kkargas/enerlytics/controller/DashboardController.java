
package com.kkargas.enerlytics.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.repository.DatasetRepository;

@Controller
public class DashboardController {

    private final DatasetRepository datasetRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public DashboardController(
            DatasetRepository datasetRepository,
            JdbcTemplate jdbcTemplate) {

        this.datasetRepository = datasetRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {

        List<Dataset> datasets = datasetRepository.findAll(
                Sort.by(Sort.Direction.ASC, "id")
        );

        long totalDatasets = datasets.size();

        Long totalMeasurements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM energy_measurements",
                Long.class
        );

        Long firstTimestamp = jdbcTemplate.query(
                "SELECT MIN(measured_at) FROM energy_measurements",
                rs -> rs.next()
                        ? (Long) rs.getObject(1)
                        : null
        );

        Long lastTimestamp = jdbcTemplate.query(
                "SELECT MAX(measured_at) FROM energy_measurements",
                rs -> rs.next()
                        ? (Long) rs.getObject(1)
                        : null
        );
        
        Long activeDatasetId = datasets.isEmpty()
                ? null
                : datasets.get(0).getId();

        String lastImportStatus = jdbcTemplate.query(
                """
                SELECT status
                FROM import_jobs
                ORDER BY id DESC
                LIMIT 1
                """,
                rs -> rs.next()
                        ? rs.getString("status")
                        : "NO IMPORTS"
        );

        String datasetName = datasets.isEmpty()
                ? "No datasets available"
                : datasets.get(0).getName();

        model.addAttribute("totalDatasets", totalDatasets);
        model.addAttribute("totalMeasurements", totalMeasurements);
        model.addAttribute("datasetName", datasetName);
        model.addAttribute("datasets", datasets);

        model.addAttribute(
                "firstMeasurement",
                formatTimestamp(firstTimestamp)
        );

        model.addAttribute(
                "lastMeasurement",
                formatTimestamp(lastTimestamp)
        );
        model.addAttribute("activeDatasetId", activeDatasetId);

        model.addAttribute("lastImportStatus", lastImportStatus);

        return "Dashboard";
    }

    private String formatTimestamp(Long epochMillis) {

        if (epochMillis == null) {
            return "No data";
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis),
                ZoneOffset.UTC
        );

        return dateTime.format(DATE_FORMAT);
    }
}
