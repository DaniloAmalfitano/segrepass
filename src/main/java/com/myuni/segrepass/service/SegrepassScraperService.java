package com.myuni.segrepass.service;

import com.myuni.segrepass.dto.SummaryDto;
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
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(15);


    public List<ExamDto> fetchExams(WebDriver driver) {
        try {
            logger.info("Recupero esami con driver esistente");
            navigateToEsamiSostenuti(driver);
            List<ExamDto> exams = parseExams(driver);
            logger.info("Estratti {} esami", exams.size());
            return exams;
        } catch (Exception e) {
            logger.error("Errore durante scraping: {}", e.getMessage(), e);
            throw new RuntimeException("Scraping fallito: " + e.getMessage(), e);
        }
    }

    public SummaryDto fetchSummary(WebDriver driver) {
        try {
            logger.info("Recupero summary con driver esistente");
            navigateToRiepilogoEsamiECrediti(driver);
            SummaryDto summary = parseSummary(driver);
            logger.info("Summary recuperato");
            return summary;
        } catch (Exception e) {
            logger.error("Errore durante scraping: {}", e.getMessage(), e);
            throw new RuntimeException("Scraping fallito: " + e.getMessage(), e);
        }
    }


    public WebDriver setupDriver() {
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

    public void login(WebDriver driver, String username, String password) {
        logger.info("Inizio login");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);

        try {
            WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("codice_fiscale")));
            usernameField.clear();
            usernameField.sendKeys(username);

            WebElement passwordField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("password")));
            passwordField.clear();
            passwordField.sendKeys(password);

            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("cfSubmit")));
            loginButton.click();

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("link_1")));
            logger.info("Login completato");
        } catch (Exception e) {
            logger.error("Login fallito: {}", e.getMessage(), e);
            throw new RuntimeException("Autenticazione fallita", e);
        }
    }


    private void navigateToEsamiSostenuti(WebDriver driver) {
        logger.info("Navigazione: Dati Carriera -> Esami Sostenuti");
        WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);

        try {
            WebElement datiCarriera = wait.until(ExpectedConditions.elementToBeClickable(By.id("link_1")));
            datiCarriera.click();
            logger.info("Cliccato su Dati Carriera");

            WebElement esamiSostenuti = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='azione=esamiSostenuti']")));
            esamiSostenuti.click();
            logger.info("Cliccato su Esami Sostenuti");

            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table")));

        } catch (Exception e) {
            logger.error("Errore navigazione menu: {}", e.getMessage(), e);
            throw new RuntimeException("Navigazione a Esami Sostenuti fallita", e);
        }
    }

public void navigateToRiepilogoEsamiECrediti(WebDriver driver) {
    logger.info("Navigazione: Dati Carriera -> Riepilogo esami e crediti");
    WebDriverWait wait = new WebDriverWait(driver, WAIT_TIMEOUT);

    try {
        WebElement datiCarriera = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("link_1")));
        datiCarriera.click();

        WebElement riepilogo = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='azione=riepilogoEsamiCrediti']")));
        riepilogo.click();

        WebElement calcola = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@type='submit' and @name='buttonAction' and @value='Calcola']")));
        calcola.click();

        // Aspetta che la tabella del riepilogo sia caricata, cercando il primo elemento da parsare"
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[contains(normalize-space(.), 'Crediti Maturati')]")));

        logger.info("Riepilogo caricato");
    } catch (Exception e) {
        throw new RuntimeException("Navigazione a riepilogo esami e crediti fallita", e);
    }
}


    private List<ExamDto> parseExams(WebDriver driver) {
        List<ExamDto> exams = new ArrayList<>();
        logger.info("Parsing esami");

        try {
            List<WebElement> rows = driver.findElements(By.xpath("//tbody/tr"));
            logger.info("Trovate {} righe", rows.size());

            for (WebElement row : rows) {
                try {
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    if (cells.size() < 5) {
                        continue;
                    }

                    String courseCode = cells.get(0).getText().trim();
                    String courseName = cells.get(1).getText().trim();
                    String grade = cells.get(2).getText().trim();
                    String cfu = cells.get(3).getText().trim();
                    String date = cells.get(4).getText().trim();

                    if (courseName.isEmpty()) {
                        continue;
                    }

                    exams.add(new ExamDto(courseCode,courseName, cfu, grade, date));
                } catch (Exception e) {
                    logger.warn("Errore parsing riga: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Errore parsing tabella esami: {}", e.getMessage(), e);
        }
        return exams;
    }
    private SummaryDto parseSummary(WebDriver driver) {
        SummaryDto summary = new SummaryDto();
        logger.info("Parsing riepilogo esami e crediti");

        try {
            List<WebElement> rows = driver.findElements(By.cssSelector("table.tabella tbody tr"));
            logger.info("Trovate {} righe nel riepilogo", rows.size());

            for (WebElement row : rows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                if (cells.size() < 2) {
                    continue;
                }


                for (int i = 0; i + 1 < cells.size(); i += 2) {
                    String label = cells.get(i).getText()
                            .trim()
                            .replace('\u00A0', ' ')
                            .replaceAll("\\s+", " ")
                            .toLowerCase();

                    String value = cells.get(i + 1).getText()
                            .trim()
                            .replace('\u00A0', ' ')
                            .replaceAll("\\s+", " ");

                    if (label.isEmpty() || value.isEmpty()) {
                        continue;
                    }

                    switch (label) {
                        case "crediti maturati" -> summary.setCfuMatured(value);
                        case "crediti mancanti" -> summary.setCfuMissing(value);
                        case "crediti totali" -> summary.setCfuTotal(value);

                        case "esami sostenuti" -> summary.setExamsTaken(value);
                        case "esami in media" -> summary.setAverageExams(value);

                        case "media ponderata su 30" -> summary.setWeightedAverage(value);
                        case "media aritmetica su 30" -> summary.setArithmeticAverage(value);
                        case "media ponderata su 110" -> summary.setWeightedAverageOn110(value);
                        case "media aritmetica su 110" -> summary.setArithmeticAverageOn110(value);

                        case "numero di lodi" -> summary.setNumberOfHonors(value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Errore parsing riepilogo: {}", e.getMessage(), e);
        }

        return summary;
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
