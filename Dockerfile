FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -q

# ============ Runtime stage ============
FROM eclipse-temurin:21-jre-alpine

# Installa Chromium e dependenze per Selenium
RUN apk add --no-cache chromium chromium-chromedriver

ENV CHROME_BIN=/usr/bin/chromium-browser
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver


WORKDIR /app

# Copia jar compilato
COPY --from=builder /app/target/*.jar app.jar

# Esponi porta
EXPOSE 8080

# Startup
ENTRYPOINT ["java", "-jar", "app.jar"]
