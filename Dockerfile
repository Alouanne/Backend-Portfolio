# -------- Stage 1: Build the JAR inside Docker --------
FROM maven:3.9.6-eclipse-temurin-17 as builder

WORKDIR /app

# Copy the full source code into the container
COPY . .

# Build the JAR (skipping tests is optional)
RUN mvn clean package -DskipTests

# -------- Stage 2: Run the JAR --------
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Start the app
CMD ["java", "-jar", "app.jar"]
