package com.kkargas.enerlytics.controller;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.repository.DatasetRepository;

@Controller
public class AnalyticsPageController {

    private final DatasetRepository datasetRepository;

    public AnalyticsPageController(DatasetRepository datasetRepository) {
        this.datasetRepository = datasetRepository;
    }

    @GetMapping("/analytics")
    public String analytics(@RequestParam(required = false) Long datasetId, Model model) {
        List<Dataset> datasets = datasetRepository.findAll(Sort.by("id"));

        Long selectedId = datasets.stream()
                .filter(dataset -> dataset.getId().equals(datasetId))
                .map(Dataset::getId)
                .findFirst()
                .orElseGet(() -> datasets.isEmpty() ? null : datasets.get(0).getId());

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDatasetId", selectedId);
        return "Analytics";
    }
}
