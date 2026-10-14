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
# Java 21 runtime (the jar targets 17, which runs unchanged on 21) so virtual threads are available.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/home-task-service.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
# Tuned for a fast boot on a small instance (Render free: 0.1 CPU, 512 MB), same as finch-service.
ENV JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:MaxRAMPercentage=70"

ENTRYPOINT ["java", "-jar", "app.jar"]
