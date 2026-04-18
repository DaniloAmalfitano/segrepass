package com.myuni.segrepass.service;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;


@Service
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    //Timeout di 30 minuti per inattività
    private static final long SESSION_TIMEOUT_30 = 30*60*1000;

    //Timeout a 15 secondi per testing
    //private static final long SESSION_TIMEOUT_30 = 15_000;
    @Scheduled(fixedRate = 60_000)
    public void scheduledCleanupExpiredSessions() {
        cleanupExpiredSessions();
    }

    public synchronized String CreateSession(String username, WebDriver driver) {
        for (Map.Entry<String, UserSession> entry : sessions.entrySet()) {
            UserSession existingSession = entry.getValue();

            if (existingSession.getUsername().equals(username)) {
                if (!IsSessionExpired(existingSession)) {
                    existingSession.updateLastAccessed();
                    logger.info("Utente {} già loggato, restituisco sessionId esistente: {}", username, entry.getKey());

                    safeQuit(driver);

                    return entry.getKey();
                } else {
                    logger.info("Sessione scaduta per utente {}, la chiudo e ne creo una nuova", username);
                    CloseUserSession(entry.getKey());
                    break;
                }
            }
        }

        String sessionId = java.util.UUID.randomUUID().toString();
        UserSession session = new UserSession(sessionId, username, driver, LocalDateTime.now(), LocalDateTime.now());
        sessions.put(sessionId, session);
        logger.info("Sessione creata per utente {}: {}", username, sessionId);
        return sessionId;
    }

    public UserSession getSession(String sessionId) {
        UserSession session = sessions.get(sessionId);
        if (session == null) {
            logger.warn("Sessione non trovata: {}", sessionId);
            return null;
        }
        if (IsSessionExpired(session)) {
            logger.info("Sessione scaduta: {}", sessionId);
            CloseUserSession(sessionId);
            return null;
        }

        session.updateLastAccessed();
        return session;
    }

    public void CloseUserSession(String sessionId) {
        UserSession session = sessions.remove(sessionId);
        if (session == null) {
            logger.warn("Sessione non trovata: {}", sessionId);
            return;
        }

        // Se una request sta usando la sessione, aspetta qui
        synchronized (session) {
            try {
                if (session.getWebDriver() != null) {
                    session.getWebDriver().quit();
                }
            } catch (Exception e) {
                logger.warn("Errore chiusura driver per sessione {}: {}", sessionId, e.getMessage());
            }
        }
    }
    private void safeQuit(WebDriver driver) {
        if (driver == null) return;
        try {
            driver.quit();
        } catch (Exception e) {
            logger.warn("Errore chiusura driver non usato: {}", e.getMessage());
        }
    }

    private boolean IsSessionExpired(UserSession session) {
        long elapsedTime = System.currentTimeMillis() - session.getLastAccessedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return  elapsedTime >= SESSION_TIMEOUT_30;
    }

    public void cleanupExpiredSessions() {
        List<String> expiredSessions = new ArrayList<>();

        sessions.forEach((sessionId, session) -> {
            if (IsSessionExpired(session)) {
                expiredSessions.add(sessionId);
            }
        });

        expiredSessions.forEach(this::CloseUserSession);
        if (!expiredSessions.isEmpty()) {
            logger.info("Pulite {} sessioni scadute", expiredSessions.size());
        }
    }
    public int getActiveSessions() {
        return sessions.size();
    }

}
