package com.kkargas.enerlytics.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingsController {

    @Value("${spring.application.name:Enerlytics}")
    private String applicationName;

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("databaseEngine", "SQLite");
        model.addAttribute("dataStorage", "Local database");
        return "Settings";
    }
}
