plugins {
    id("org.springframework.boot")
}


dependencies {
    implementation(project(":common"))
    implementation(project(":core-domain"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")
    testImplementation("org.springframework.boot:spring-boot-testcontainers:3.3.4")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37");
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun"){
        jvmArgs("--enable-preview")
    }
}



