package com.myuni.segrepass.controller;

import com.myuni.segrepass.dto.SummaryDto;
import com.myuni.segrepass.service.SessionManager;
import com.myuni.segrepass.service.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myuni.segrepass.dto.ExamDto;
import com.myuni.segrepass.service.SegrepassScraperService;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ScrapingController {
    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    @Autowired
    private SegrepassScraperService scraperService;

    @Autowired
    private SessionManager sessionManager;

    @PostMapping("/libretto")
    public List<ExamDto> getLibretto() {
        String username = extractUsernameFromJwt();
        logger.info("Richiesta libretto ricevuta per: {}", username);

        UserSession session = sessionManager.getSessionByUsername(username);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessione non valida o scaduta");
        }

        try {
            synchronized (session) {
                session.updateLastAccessed();
                return scraperService.fetchExams(session.getWebDriver());
            }
        } catch (Exception e) {
            logger.error("Errore durante fetch esami: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il recupero del libretto");
        }
    }

    @PostMapping("/summary")
    public SummaryDto getSummary() {
        String username = extractUsernameFromJwt();
        logger.info("Richiesta summary ricevuta per: {}", username);

        UserSession session = sessionManager.getSessionByUsername(username);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessione non valida o scaduta");
        }

        try {
            synchronized (session) {
                session.updateLastAccessed();
                return scraperService.fetchSummary(session.getWebDriver());
            }
        } catch (Exception e) {
            logger.error("Errore durante fetch summary: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il recupero del summary");
        }
    }


    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    private String extractUsernameFromJwt() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT non valido");
        }
        return username;
    }
}
