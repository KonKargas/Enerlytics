package com.kkargas.enerlytics.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.kkargas.enerlytics.analytics.AggregationResult;
import com.kkargas.enerlytics.analytics.AggregationService;
import com.kkargas.enerlytics.analytics.AggregationType;
import com.kkargas.enerlytics.analytics.TimeGrouping;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AggregationService aggregationService;

    public AnalyticsController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping
    public List<AggregationResult> getAnalytics(
            @RequestParam Long datasetId,
            @RequestParam(defaultValue = "MONTH") TimeGrouping grouping,
            @RequestParam(defaultValue = "SUM") AggregationType type,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Date From must not be after Date To");
        }

        return aggregationService.aggregate(datasetId, grouping, type, dateFrom, dateTo);
    }
}
