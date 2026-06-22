# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copy the maven executable and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make the mvnw script executable (in case it lost its execute permission)
RUN chmod +x ./mvnw

# Resolve maven dependencies (this step is cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy the project source code
COPY src src

# Package the application (skip tests to speed up deployment builds)
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the minimal production image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built .jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
