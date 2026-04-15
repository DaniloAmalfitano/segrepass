package com.myuni.segrepass.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myuni.segrepass.dto.ExamDto;
import com.myuni.segrepass.dto.RequestLibretto;
import com.myuni.segrepass.service.SegrepassScraperService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScrapingController {
    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    @Autowired
    private SegrepassScraperService scraperService;

    @PostMapping("/libretto")
    public List<ExamDto> getLibretto(@RequestBody RequestLibretto request) {
        logger.info("Richiesta libretto ricevuta");
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("username e password sono obbligatori");
        }
        return scraperService.fetchExams(request.getUsername(), request.getPassword());
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
