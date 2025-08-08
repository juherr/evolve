# Stage 1: Build the application
FROM maven:3.8-openjdk-17 AS builder

WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download Maven dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src/ src/

# Build the application, skipping tests
RUN ./mvnw package -DskipTests=true

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/steve.jar /app/steve.jar

# Expose the application port
EXPOSE 8080

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "/app/steve.jar"]
