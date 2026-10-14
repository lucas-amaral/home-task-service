import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.5.13"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
}

group = "com.amaral.home"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql")
    implementation("com.h2database:h2")
    implementation("com.zaxxer:HikariCP")
    // Flyway owns the production schema (src/main/resources/db/migration)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    // Web Push (VAPID) for the reminders sent to the children's phones
    implementation("nl.martijndwars:web-push:5.1.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
    testImplementation("com.h2database:h2")
    // Real PostgreSQL for PostgresMigrationTest, no Docker needed (same as finch-service)
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.9.0"))
    testRuntimeOnly("io.zonky.test.postgres:embedded-postgres-binaries-darwin-arm64v8")
    testRuntimeOnly("io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64")
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.fromTarget("17"))
    }
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Pinned because tools/VapidKeyGenerator also has a main(); without it bootJar can't pick one.
springBoot {
    mainClass.set("com.amaral.hometask.HomeTaskServiceApplicationKt")
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("home-task-service.jar")
}

// Prints a new pair of VAPID keys for Web Push: ./gradlew generateVapidKeys
tasks.register<JavaExec>("generateVapidKeys") {
    group = "application"
    description = "Generates the VAPID key pair used to send push notifications"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.amaral.hometask.tools.VapidKeyGeneratorKt")
}
