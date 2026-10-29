package com.kkargas.enerlytics.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.kkargas.enerlytics.analytics.AggregationResult;
import com.kkargas.enerlytics.analytics.AggregationService;
import com.kkargas.enerlytics.analytics.AggregationType;
import com.kkargas.enerlytics.analytics.TimeGrouping;
import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.repository.DatasetRepository;

@Controller
public class ReportsController {

    private final DatasetRepository datasetRepository;
    private final AggregationService aggregationService;

    public ReportsController(DatasetRepository datasetRepository,
                             AggregationService aggregationService) {
        this.datasetRepository = datasetRepository;
        this.aggregationService = aggregationService;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) Long datasetId,
                          @RequestParam(defaultValue = "MONTH") TimeGrouping grouping,
                          @RequestParam(defaultValue = "SUM") AggregationType type,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                          @RequestParam(required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                          Model model) {
        validateRange(dateFrom, dateTo);
        List<Dataset> datasets = datasetRepository.findAll(Sort.by("id"));
        Dataset selected = selectDataset(datasetId, datasets);
        List<AggregationResult> rows = selected == null ? List.of() :
                aggregationService.aggregate(selected.getId(), grouping, type, dateFrom, dateTo);

        long count = rows.stream().mapToLong(AggregationResult::measurementCount).sum();
        // A weighted average is required because groups have unequal measurement counts.
        double kitchen = total(rows, type, 0);
        double laundry = total(rows, type, 1);
        double heating = total(rows, type, 2);

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDataset", selected);
        model.addAttribute("grouping", grouping);
        model.addAttribute("type", type);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("results", rows);
        model.addAttribute("unit", type == AggregationType.SUM ? "kWh" : "Wh / reading");
        model.addAttribute("divisor", type == AggregationType.SUM ? 1000.0 : 1.0);
        model.addAttribute("measurements", count);
        model.addAttribute("kitchenTotal", kitchen / (type == AggregationType.SUM ? 1000.0 : 1.0));
        model.addAttribute("laundryTotal", laundry / (type == AggregationType.SUM ? 1000.0 : 1.0));
        model.addAttribute("heatingTotal", heating / (type == AggregationType.SUM ? 1000.0 : 1.0));
        return "Reports";
    }

    @GetMapping(value = "/reports/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam Long datasetId,
            @RequestParam(defaultValue = "MONTH") TimeGrouping grouping,
            @RequestParam(defaultValue = "SUM") AggregationType type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        validateRange(dateFrom, dateTo);
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));
        List<AggregationResult> rows = aggregationService.aggregate(
                dataset.getId(), grouping, type, dateFrom, dateTo);
        boolean sum = type == AggregationType.SUM;
        double divisor = sum ? 1000.0 : 1.0;
        String unit = sum ? "kWh" : "Wh_per_reading";
        StringBuilder csv = new StringBuilder("Period,Measurements,Kitchen_")
                .append(unit).append(",Laundry_").append(unit)
                .append(",Heating_").append(unit).append("\r\n");
        for (AggregationResult r : rows) {
            csv.append(r.period()).append(',')
               .append(r.measurementCount()).append(',')
               .append(format(r.kitchen() / divisor)).append(',')
               .append(format(r.laundry() / divisor)).append(',')
               .append(format(r.heating() / divisor)).append("\r\n");
        }
        String filename = "enerlytics-report-" + dataset.getId() + "-"
                + grouping.name().toLowerCase(Locale.ROOT) + "-"
                + type.name().toLowerCase(Locale.ROOT) + ".csv";
        // UTF-8 BOM lets Excel reliably read CSV files with a UTF-8 encoding.
        byte[] bytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private static double total(List<AggregationResult> rows, AggregationType type, int meter) {
        double sum = 0;
        long measurements = 0;
        for (AggregationResult r : rows) {
            double value = switch (meter) {
                case 0 -> r.kitchen();
                case 1 -> r.laundry();
                default -> r.heating();
            };
            if (type == AggregationType.AVG) {
                sum += value * r.measurementCount();
                measurements += r.measurementCount();
            } else {
                sum += value;
            }
        }
        return type == AggregationType.AVG ? (measurements == 0 ? 0 : sum / measurements) : sum;
    }

    private static Dataset selectDataset(Long id, List<Dataset> datasets) {
        if (id == null) return datasets.isEmpty() ? null : datasets.get(0);
        return datasets.stream().filter(d -> d.getId().equals(id)).findFirst().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found"));
    }

    private static void validateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date From must not be after Date To");
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
