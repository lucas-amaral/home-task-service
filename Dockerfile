# ── Build stage ──────────────────────────────────────────────────────────────
FROM gradle:8.14-jdk17 AS build
WORKDIR /app

# Cache dependencies separately from source for faster rebuilds
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/home-task-service.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
