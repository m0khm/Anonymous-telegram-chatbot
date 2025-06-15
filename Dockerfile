# ─── STAGE 1: сборка ─────────────────────────
FROM maven:3.8.3-openjdk-17 AS builder
WORKDIR /app

# Копируем pom.xml и создаём стандартные папки для исходников
COPY pom.xml .
RUN mkdir -p src/main/java/org/example

# Копируем ваши Java-файлы в Maven-структуру
COPY AnonymousChatBot.java src/main/java/org/example/AnonymousChatBot.java
COPY Main.java            src/main/java/org/example/Main.java

# Заранее скачиваем зависимости, затем собираем jar
RUN mvn dependency:go-offline -B
RUN mvn clean package -DskipTests -B

# ─── STAGE 2: рантайм ────────────────────────
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Копируем собранный артефакт
COPY --from=builder /app/target/first-tg-bot-1.0-SNAPSHOT.jar bot.jar

# Папка для скачанных фото/аудио
RUN mkdir -p /app/downloads

# Точка, откуда библиотека java-dotenv будет читать .env
ENV DOTENV_CONFIG_PATH=/app/.env

# Запуск бота
ENTRYPOINT ["java","-jar","/app/bot.jar"]
