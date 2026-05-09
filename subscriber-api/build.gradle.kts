plugins {
	id("org.springframework.boot")
}

dependencies {
	implementation(project(":common"))
	implementation(project(":core-domain"))

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("net.logstash.logback:logstash-logback-encoder:7.4")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}