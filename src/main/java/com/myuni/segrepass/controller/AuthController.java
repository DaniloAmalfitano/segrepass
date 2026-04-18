package com.myuni.segrepass.controller;

import com.myuni.segrepass.dto.LoginRequestDto;
import com.myuni.segrepass.dto.RefreshRequestDto;
import com.myuni.segrepass.dto.TokenResponseDto;
import com.myuni.segrepass.service.JwtService;
import com.myuni.segrepass.service.SegrepassScraperService;
import com.myuni.segrepass.service.SessionManager;
import com.myuni.segrepass.service.UserSession;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private SegrepassScraperService scraperService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SessionManager sessionManager;

    @PostMapping("/login")
    public TokenResponseDto login(@RequestBody LoginRequestDto request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username e password sono obbligatori");
        }

        logger.info("Richiesta login ricevuta per: {}", request.getUsername());

        WebDriver driver = null;
        try {
            driver = scraperService.setupDriver();
            driver.get("https://www.segrepass1.unina.it/Welcome.do");
            scraperService.login(driver, request.getUsername(), request.getPassword());

            // Login OK: salva driver in SessionManager (NON quittare il driver!)
            String sessionId = sessionManager.createSession(request.getUsername(), driver);
            driver = null; // Non distruggere il driver, è gestito da SessionManager

            // Genera token JWT
            String accessToken = jwtService.generateAccessToken(request.getUsername());
            String refreshToken = jwtService.generateRefreshToken(request.getUsername());

            long expiresIn = 900000 / 1000; // converti ms a secondi

            TokenResponseDto response = new TokenResponseDto(
                    accessToken,
                    refreshToken,
                    expiresIn,
                    "Bearer"
            );

            logger.info("Login OK per: {} - SessionId: {}", request.getUsername(), sessionId);
            return response;

        } catch (RuntimeException ex) {
            logger.error("Login fallito: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");

        } catch (Exception ex) {
            logger.error("Errore interno login: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il login");

        } finally {
            // Se login fallisce e driver non è stato salvato, quittalo
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {}
            }
        }
    }

    @PostMapping("/refresh")
    public TokenResponseDto refresh(@RequestBody RefreshRequestDto request) {
        if (request == null || isBlank(request.getRefreshToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken obbligatorio");
        }

        logger.info("Richiesta refresh ricevuta");

        try {
            if (!jwtService.isTokenValid(request.getRefreshToken())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token scaduto o invalido");
            }

            String username = jwtService.extractUsername(request.getRefreshToken());
            String newAccessToken = jwtService.generateAccessToken(username);

            long expiresIn = 900000 / 1000;

            TokenResponseDto response = new TokenResponseDto(
                    newAccessToken,
                    request.getRefreshToken(),
                    expiresIn,
                    "Bearer"
            );

            logger.info("Refresh OK per: {}", username);
            return response;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error("Errore durante refresh: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il refresh");
        }
    }
    @PostMapping("/logout")
    public Map<String, String> logout() {
        String username = extractUsernameFromJwt();
        logger.info("Logout ricevuto per: {}", username);

        try {
            UserSession session = sessionManager.getSessionByUsername(username);
            if (session != null) {
                sessionManager.closeUserSession(session.getSessionId());
            }
            return Map.of("message", "Logout successful");
        } catch (Exception e) {
            logger.error("Errore durante il logout: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno durante il logout");
        }
    }

    private String extractUsernameFromJwt() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT non valido");
        }
        return username;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}