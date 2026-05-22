#syntax=docker/dockerfile:1.7

#----- Stage 1: build -----
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace


# Copy gradle wrapper + settings + module builde files first rarely changed
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY common/build.gradle.kts ./common/
COPY core-domain/build.gradle.kts ./core-domain/
COPY subscriber-api/build.gradle.kts ./subscriber-api/

# run download deps with build files only
RUN ./gradlew :subscriber-api:denpendencies --no-daemon || true

# actual sources will change with every commit
COPY common/src ./common/src
COPY core-domain/src ./core-domain/src
COPY subscriber-api/src ./subscriber-api/src

# Build jar and skip test CI will run test
RUN ./gradlew :subscriber-api:bootJar --no-daemon -x test

# extract Sprint Boot's layered jar for Optimal Docker caching
RUN mkdir -p /workspace/extracted && \
    cd /workspace/extracted && \
    java -Djarmode=layertools -jar /workspace/subscriber-api/build/libs/*.jar extract


# ----- Stage2 Runtime ------ \
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# NON root user
RUN addgroup -S app && adduser -S app -G app
USER app

# Copy each spring boot layer separately
COPY --from=builder --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app:app /workspace/extracted/application/ ./

EXPOSE 8080

# JVM tuning for containers uses memory limits container is given
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]