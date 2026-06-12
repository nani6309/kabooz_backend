# ================================================================
# Multi-stage Docker build for Kabooz Spring Boot backend
# Stage 1: Build the JAR with Maven
# Stage 2: Run with a minimal JRE image
# ================================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy only pom.xml first (layer cache — deps only re-downloaded on pom change)
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn -B -DskipTests clean package -q

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S kabooz && adduser -S kabooz -G kabooz
USER kabooz

# Copy only the fat JAR from the build stage
COPY --from=build /app/target/kabooz-backend-1.0.0.jar app.jar

# Render injects PORT env var; Spring Boot reads it via server.port=${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
