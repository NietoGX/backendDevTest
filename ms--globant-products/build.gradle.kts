plugins {
	java
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.globant.david"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// WebFlux for reactive WebClient
	implementation("org.springframework.boot:spring-boot-starter-webflux")

	// Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Cache
	implementation("org.springframework.boot:spring-boot-starter-cache")

	// Actuator for metrics and health checks
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Resilience4j for Circuit Breaker
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
	implementation("io.github.resilience4j:resilience4j-reactor:2.3.0")

	// Lombok for reducing boilerplate
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Cache provider (Caffeine)
	implementation("com.github.ben-manes.caffeine:caffeine")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.wiremock:wiremock-standalone:3.9.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Disable parallel test execution
	systemProperty("junit.jupiter.execution.parallel.enabled", "false")
	maxParallelForks = 1
}
