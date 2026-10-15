# ---------- Stage 1: Build ----------
FROM gradle:8.14-jdk21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle ./gradle
RUN gradle --no-daemon build -x test || true

# Copy source and build jar
COPY . .
RUN gradle --no-daemon clean bootJar

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
ARG JAR_PATH=/app/build/libs
COPY --from=builder ${JAR_PATH}/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]