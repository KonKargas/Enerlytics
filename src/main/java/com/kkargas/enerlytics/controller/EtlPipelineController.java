package com.kkargas.enerlytics.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.kkargas.enerlytics.importing.ImportStatus;
import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.repository.ImportJobRepository;

@Controller
public class EtlPipelineController {

    private static final int RECENT_JOB_LIMIT = 30;
    private final ImportJobRepository importJobRepository;

    public EtlPipelineController(ImportJobRepository importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @GetMapping("/etl")
    public String pipeline(Model model) {
    		List<ImportJob> allJobs = importJobRepository.findAllWithDataset();
        allJobs.sort(Comparator.comparing(ImportJob::getId,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        long completed = allJobs.stream()
                .filter(job -> job.getStatus() == ImportStatus.COMPLETED).count();
        long failed = allJobs.stream()
                .filter(job -> job.getStatus() == ImportStatus.FAILED).count();
        long processing = allJobs.stream()
                .filter(job -> job.getStatus() == ImportStatus.PROCESSING).count();

        long imported = allJobs.stream()
                .filter(job -> job.getStatus() == ImportStatus.COMPLETED)
                .mapToLong(job -> safe(job.getImportedRows())).sum();
        long skipped = allJobs.stream()
                .filter(job -> job.getStatus() == ImportStatus.COMPLETED)
                .mapToLong(job -> safe(job.getSkippedRows())).sum();

        model.addAttribute("totalJobs", allJobs.size());
        model.addAttribute("completedJobs", completed);
        model.addAttribute("failedJobs", failed);
        model.addAttribute("processingJobs", processing);
        model.addAttribute("importedRows", imported);
        model.addAttribute("skippedRows", skipped);
        model.addAttribute("recentJobs", allJobs.stream()
                .limit(RECENT_JOB_LIMIT).toList());
        return "EtlPipeline";
    }

    private static long safe(Long value) {
        return value == null ? 0 : value;
    }
}
