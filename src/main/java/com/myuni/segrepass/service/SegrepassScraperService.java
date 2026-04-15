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
            WebElement usernameField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.name("codice_fiscale"))
            );
            usernameField.clear();
            usernameField.sendKeys(username);

            WebElement passwordField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.name("password"))
            );
            passwordField.clear();
            passwordField.sendKeys(password);

            WebElement loginButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("cfSubmit"))
            );
            loginButton.click();

            // Aspetta che compaia il menu post-login
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
            WebElement datiCarriera = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("link_1"))
            );
            datiCarriera.click();
            logger.info("Cliccato su Dati Carriera");

            WebElement esamiSostenuti = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("a[href*='azione=esamiSostenuti']")
                    )
            );
            esamiSostenuti.click();
            logger.info("Cliccato su Esami Sostenuti");

            // Attendi che si carichi la pagina con la tabella
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table")));

        } catch (Exception e) {
            logger.error("Errore navigazione menu: {}", e.getMessage(), e);
            throw new RuntimeException("Navigazione a Esami Sostenuti fallita", e);
        }
    }


    private List<ExamDto> parseExams(WebDriver driver) {
        List<ExamDto> exams = new ArrayList<>();
        logger.info("Parsing esami");

        try {
            // Righe reali della tabella esami
            List<WebElement> rows = driver.findElements(By.xpath("//tbody/tr"));
            logger.info("Trovate {} righe", rows.size());

            for (WebElement row : rows) {
                try {
                    List<WebElement> cells = row.findElements(By.tagName("td"));
                    if (cells.size() < 5) {
                        continue;
                    }

                    // HTML reale:
                    // td[1]=codice, td[2]=nome esame, td[3]=voto, td[4]=cfu, td[5]=data
                    String courseName = cells.get(1).getText().trim();
                    String grade = cells.get(2).getText().trim();
                    String cfu = cells.get(3).getText().trim();
                    String date = cells.get(4).getText().trim();

                    if (courseName.isEmpty()) {
                        continue;
                    }

                    exams.add(new ExamDto(courseName, cfu, grade, date));
                } catch (Exception e) {
                    logger.warn("Errore parsing riga: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Errore parsing tabella esami: {}", e.getMessage(), e);
        }

        return exams;
    }
}
