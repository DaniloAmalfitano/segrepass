package com.myuni.segrepass.service;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.WebDriver;


import java.time.LocalDateTime;

@Getter
@Setter
public class UserSession {
    private String sessionId;
    private String username;
    private WebDriver webDriver;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;


    public UserSession(String sessionId, String username, WebDriver webDriver, LocalDateTime createdAt, LocalDateTime lastAccessedAt) {
        this.sessionId = sessionId;
        this.username = username;
        this.webDriver = webDriver;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
    }
    public UserSession(String sessionId, String username, WebDriver webDriver) {
        this.sessionId = sessionId;
        this.username = username;
        this.webDriver = webDriver;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
