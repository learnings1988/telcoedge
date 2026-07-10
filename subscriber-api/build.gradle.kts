plugins {
	id("org.springframework.boot")
}

dependencies {
	implementation(project(":common"))
	implementation(project(":core-domain"))

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("net.logstash.logback:logstash-logback-encoder:7.4")

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")
	testImplementation("org.springframework.boot:spring-boot-testcontainers:3.3.4")
	testImplementation("org.testcontainers:junit-jupiter:1.21.4")
	testImplementation("org.testcontainers:testcontainers:1.21.4")
	testImplementation("org.testcontainers:postgresql:1.21.4")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}