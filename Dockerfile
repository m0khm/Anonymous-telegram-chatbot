# Stage 1: сборка
FROM maven:3.8.3-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: рантайм
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

COPY --from=builder /app/target/first-tg-bot-1.0-SNAPSHOT.jar bot.jar
RUN mkdir -p /app/downloads

ENV DOTENV_CONFIG_PATH=/app/.env
ENTRYPOINT ["java", "-jar", "/app/bot.jar"]
