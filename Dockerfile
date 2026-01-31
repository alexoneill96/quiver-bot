# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1000 quiverbot && \
    adduser -u 1000 -G quiverbot -s /bin/sh -D quiverbot
USER quiverbot

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:3000/health || exit 1

# Railway sets PORT dynamically
ENV PORT=3000
EXPOSE ${PORT}

# Run the application with Railway's PORT
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
