package com.kkargas.enerlytics.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kkargas.enerlytics.model.Dataset;
import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.repository.DatasetRepository;
import com.kkargas.enerlytics.repository.ImportJobRepository;

@Controller
public class DatasetsPageController {

    private final DatasetRepository datasetRepository;
    private final ImportJobRepository importJobRepository;

    public DatasetsPageController(DatasetRepository datasetRepository,
                                  ImportJobRepository importJobRepository) {
        this.datasetRepository = datasetRepository;
        this.importJobRepository = importJobRepository;
    }

    @GetMapping("/datasets")
    public String datasets(@RequestParam(required = false) Long datasetId, Model model) {
        List<Dataset> datasets = datasetRepository.findAll(Sort.by("id"));

        // Sort in Java so this also works with the existing ImportJobRepository.
        List<ImportJob> jobs = importJobRepository.findAll();
        jobs.sort(Comparator.comparing(ImportJob::getId).reversed());

        Dataset selected = datasets.stream()
                .filter(d -> d.getId().equals(datasetId))
                .findFirst()
                .orElseGet(() -> datasets.isEmpty() ? null : datasets.get(0));

        long totalRegisteredRows = datasets.stream()
                .mapToLong(d -> d.getRecordCount() == null ? 0 : d.getRecordCount())
                .sum();

        long completedJobs = jobs.stream()
                .filter(j -> j.getStatus() != null &&
                             "COMPLETED".equals(j.getStatus().name()))
                .count();

        long failedJobs = jobs.stream()
                .filter(j -> j.getStatus() != null &&
                             "FAILED".equals(j.getStatus().name()))
                .count();

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDataset", selected);
        model.addAttribute("selectedDatasetId", selected == null ? null : selected.getId());
        model.addAttribute("totalRegisteredRows", totalRegisteredRows);
        model.addAttribute("totalDatasets", datasets.size());
        model.addAttribute("totalJobs", jobs.size());
        model.addAttribute("completedJobs", completedJobs);
        model.addAttribute("failedJobs", failedJobs);
        model.addAttribute("importJobs", jobs);
        return "Datasets";
    }
}
