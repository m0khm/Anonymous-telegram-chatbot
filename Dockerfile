# ──────────────── STAGE 1: сборка ────────────────
FROM maven:3.8.8-openjdk-17 AS builder

# Рабочая директория внутри контейнера
WORKDIR /app

# Сначала копируем только pom.xml, чтобы закэшировать зависимости
COPY pom.xml .

# Загрузим зависимости (не копируя код)
RUN mvn dependency:go-offline -B

# Теперь копируем исходники и собираем
COPY src ./src
RUN mvn clean package -DskipTests -B

# ──────────────── STAGE 2: рантайм ────────────────
FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Копируем из билдера только собранный JAR (имя поменяйте под своё в pom.xml)
COPY --from=builder /app/target/first-tg-bot-1.0-SNAPSHOT.jar bot.jar

# Каталог для сохранённых файлов (фото, голос, логи и т.п.)
RUN mkdir -p /app/downloads

# Точка монтирования .env (см. docker-compose.yml)
ENV DOTENV_CONFIG_PATH=/app/.env

# По умолчанию запускаем jar
ENTRYPOINT ["java", "-jar", "/app/bot.jar"]
