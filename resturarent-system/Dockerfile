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

# Configure JVM ergonomics for containerized environments (Render 512MB RAM limit)
# - UseContainerSupport: respects cgroup memory limits
# - MaxRAMPercentage=65.0: caps heap at ~330MB of 512MB, leaving ~180MB for Metaspace/native memory
# - UseSerialGC: minimal GC metadata overhead (saves ~100MB vs default G1GC on small containers)
# - Xss256k: reduces per-thread stack size from 1MB to 256KB
# - MaxMetaspaceSize=160m: prevents unbounded class metadata growth
# - ExitOnOutOfMemoryError: guarantees clean container restart on critical OOM
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0 -XX:+UseSerialGC -Xss256k -XX:MaxMetaspaceSize=160m -XX:+ExitOnOutOfMemoryError"

# Run the application with configured JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
