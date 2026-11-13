# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Кешируем зависимости: этот слой пересобирается только при изменении pom.xml
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Копируем исходники и Maven wrapper
COPY .mvn ./.mvn
COPY mvnw ./
COPY src ./src
RUN chmod +x ./mvnw

# Тесты прогоняются отдельно (нужен Docker для Testcontainers), в образе пропускаем
RUN mvn -B -ntp package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime

# Non-root user
RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring spring

WORKDIR /app

COPY --from=builder /build/target/poseloom-backend-*.jar app.jar

# Только настройки JVM. Профиль и порт не задаём: они приходят из окружения
# запуска (compose), а дефолты живут в application.yml.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
