# Multi-stage build
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy the pom.xml file first (for better caching)
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/sahayak-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application runs on
EXPOSE 8080

# Set environment variable for the port (Cloud Run will set this)
ENV PORT=8080

# Run the application with optimized JVM settings for containers
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
