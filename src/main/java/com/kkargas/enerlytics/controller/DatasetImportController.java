package com.kkargas.enerlytics.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kkargas.enerlytics.model.ImportJob;
import com.kkargas.enerlytics.service.WebDatasetImportService;

@Controller
public class DatasetImportController {
    private final WebDatasetImportService service;

    public DatasetImportController(WebDatasetImportService service) { this.service = service; }

    @PostMapping("/datasets/import")
    public String upload(@RequestParam("datasetName") String name,
                         @RequestParam("file") MultipartFile file,
                         RedirectAttributes messages) {
        try {
            ImportJob job = service.importFile(name, file);
            messages.addFlashAttribute("importSuccess",
                    "Import completed: " + job.getImportedRows() + " accepted, " +
                    job.getSkippedRows() + " skipped.");
        } catch (IOException | RuntimeException ex) {
            messages.addFlashAttribute("importError", "Import failed: " + ex.getMessage());
        }
        return "redirect:/datasets";
    }
}
