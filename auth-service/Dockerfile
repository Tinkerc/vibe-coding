# Multi-stage build for Auth Service
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install wget for health check
RUN apk add --no-cache wget

# Copy jar from build stage
COPY --from=build /build/target/auth-service-*.jar app.jar

# Create key storage directory with secure permissions
RUN mkdir -p /opt/auth-service/keys && \
    chmod 700 /opt/auth-service/keys && \
    chown -R 1000:1000 /opt/auth-service

# Create non-root user
RUN addgroup -S auth && \
    adduser -S auth -u 1000 -G auth -h /app -s /bin/sh

# Change ownership
RUN chown -R auth:auth /app

# Switch to non-root user
USER auth

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options optimized for container environment
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+PrintGCDetails", \
    "-XX:+PrintGCDateStamps", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}", \
    "-jar", \
    "app.jar"]
