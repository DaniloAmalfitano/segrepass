package com.myuni.segrepass.service;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myuni.segrepass.dto.ExamDto;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class SegrepassScraperService {
    private static final Logger logger = LoggerFactory.getLogger(SegrepassScraperService.class);
    private static final String SEGREPASS_URL = "https://www.segrepass1.unina.it/Welcome.do";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);

    @Value("${segrepass.username:}")
    private String username;

    @Value("${segrepass.password:}")
    private String password;

    public List<ExamDto> fetchExams() {
        List<ExamDto> exams = new ArrayList<>();
        WebDriver driver = null;

        try {
            driver = setupDriver();
            logger.info("Driver avviato, navigazione a {}", SEGREPASS_URL);

            driver.get(SEGREPASS_URL);
            login(driver);

            navigateToEsamiSostenuti(driver);
            exams = parseExams(driver);

            logger.info("Estratti {} esami", exams.size());
        } catch (Exception e) {
            logger.error("Errore durante scraping: {}", e.getMessage(), e);
            throw new RuntimeException("Scraping fallito: " + e.getMessage(), e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                    logger.info("Driver chiuso");
                } catch (Exception e) {
                    logger.warn("Errore chiusura driver: {}", e.getMessage());
                }
            }
        }

        return exams;
    }

    private WebDriver setupDriver() {
        logger.info("Setup ChromeDriver");

        String chromeBin = System.getenv().getOrDefault("CHROME_BIN", "/usr/bin/chromium-browser");
        String chromeDriverPath = System.getenv().getOrDefault("CHROMEDRIVER_PATH", "/usr/bin/chromedriver");

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
        options.setBinary(chromeBin);
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        return new ChromeDriver(options);
    }


    private void login(WebDriver driver) {
        logger.info("Inizio login");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);

        try {
            // Aspetta il campo username
            WebElement usernameField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("username"))
            );
            usernameField.sendKeys(username);

            // Trova password
            WebElement passwordField = driver.findElement(By.id("password"));
            passwordField.sendKeys(password);

            // Submit login
            WebElement loginButton = driver.findElement(By.id("login-button"));
            loginButton.click();

            // Aspetta redirect dopo login (verifica che dashboard sia caricata)
            wait.until(ExpectedConditions.urlContains("/dashboard"));
            logger.info("Login completato");
        } catch (Exception e) {
            logger.error("Login fallito: {}", e.getMessage());
            throw new RuntimeException("Autenticazione fallita", e);
        }
    }

    private void navigateToEsamiSostenuti(WebDriver driver) {
        logger.info("Navigazione: Dati carriera -> Esami sostenuti");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);

        // 1) Menu "Dati carriera"
        WebElement datiCarriera = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(normalize-space(.), 'Dati carriera')]")
                )
        );
        datiCarriera.click();

        // 2) Voce "Esami sostenuti"
        WebElement esamiSostenuti = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//a[contains(normalize-space(.), 'Esami sostenuti')]")
                )
        );
        esamiSostenuti.click();

        // 3) Attendi pagina/tabella esami (selettore da adattare al DOM reale)
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//table[contains(@class,'table') or @id='exams-table']")
        ));
    }


    private List<ExamDto> parseExams(WebDriver driver) {
        List<ExamDto> exams = new ArrayList<>();
        logger.info("Parsing esami");

        try {
            // Selettore della tabella esami (adattare al sito reale)
            List<WebElement> rows = driver.findElements(By.xpath("//table[@id='exams-table']//tbody/tr"));
            logger.info("Trovate {} righe", rows.size());

            for (WebElement row : rows) {
                try {
                    String courseName = row.findElement(By.xpath("./td[1]")).getText();
                    String cfu = row.findElement(By.xpath("./td[2]")).getText();
                    String grade = row.findElement(By.xpath("./td[3]")).getText();
                    String date = row.findElement(By.xpath("./td[4]")).getText();

                    ExamDto exam = new ExamDto(courseName, cfu, grade, date);
                    exams.add(exam);
                    logger.debug("Esame estratto: {}", exam);
                } catch (Exception e) {
                    logger.warn("Errore parsing riga: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Errore parsing tabella esami: {}", e.getMessage());
        }

        return exams;
    }
}
