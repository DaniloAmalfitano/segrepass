package com.myuni.segrepass.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myuni.segrepass.dto.ExamDto;
import com.myuni.segrepass.service.SegrepassScraperService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScrapingController {
    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    @Autowired
    private SegrepassScraperService scraperService;

    @GetMapping("/libretto")
    public List<ExamDto> getLibretto() {
        logger.info("Richiesta libretto ricevuta");
        return scraperService.fetchExams();
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
