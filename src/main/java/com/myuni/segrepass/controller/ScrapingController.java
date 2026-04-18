package com.myuni.segrepass.controller;

import com.myuni.segrepass.dto.LoginResponseDto;
import com.myuni.segrepass.dto.SummaryDto;
import com.myuni.segrepass.service.SessionManager;
import com.myuni.segrepass.service.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myuni.segrepass.dto.ExamDto;
import com.myuni.segrepass.dto.RequestLibretto;
import com.myuni.segrepass.service.SegrepassScraperService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScrapingController {
    private static final Logger logger = LoggerFactory.getLogger(ScrapingController.class);

    @Autowired
    private SegrepassScraperService scraperService;

    @Autowired
    private SessionManager sessionManager;

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody RequestLibretto request) {
        logger.info("Richiesta login ricevuta per: {}", request.getUsername());

        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("username e password sono obbligatori");
        }

        try {
            org.openqa.selenium.WebDriver driver = scraperService.setupDriver();
            driver.get("https://www.segrepass1.unina.it/Welcome.do");
            scraperService.login(driver, request.getUsername(), request.getPassword());

            String sessionId = sessionManager.createSession(request.getUsername(), driver);

            logger.info("Login successful per: {}", request.getUsername());
            return new LoginResponseDto(sessionId, request.getUsername(), "Login successful");

        } catch (Exception e) {
            logger.error("Login fallito: {}", e.getMessage(), e);
            throw new RuntimeException("Login fallito: " + e.getMessage(), e);
        }
    }

    @PostMapping("/libretto")
    public List<ExamDto> getLibretto(@RequestHeader("X-Session-ID") String sessionId) {
        logger.info("Richiesta libretto ricevuta per sessione: {}", sessionId);

        UserSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Sessione non valida o scaduta");
        }

        try {
            synchronized (session) {
                return scraperService.fetchExams(session.getWebDriver());
            }
        } catch (Exception e) {
            logger.error("Errore durante fetch esami: {}", e.getMessage(), e);
            throw new RuntimeException("Errore durante fetch esami: " + e.getMessage(), e);
        }
    }

    @PostMapping("/summary")
    public SummaryDto getSummary(@RequestHeader("X-Session-ID") String sessionId) {
        logger.info("Richiesta summary ricevuta per sessione: {}", sessionId);

        UserSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Sessione non valida o scaduta");
        }

        try {
            synchronized (session) {
                return scraperService.fetchSummary(session.getWebDriver());
            }
        } catch (Exception e) {
            logger.error("Errore durante fetch summary: {}", e.getMessage(), e);
            throw new RuntimeException("Errore durante fetch summary: " + e.getMessage(), e);
        }
    }

    /**
     * Logout - chiude la sessione
     */
    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader("X-Session-ID") String sessionId) {
        logger.info("Logout ricevuto per sessione: {}", sessionId);
        sessionManager.closeUserSession(sessionId);
        return Map.of("message", "Logout successful");
    }


    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
