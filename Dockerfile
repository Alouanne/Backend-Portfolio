# Use a base image with Java
FROM eclipse-temurin:17-jdk

# Set the working directory
WORKDIR /app

# Copy the built jar file (make sure this matches your real jar name)
COPY target/*.jar app.jar

# Expose port (Spring Boot default)
EXPOSE 8080

# Command to run your Spring Boot app
CMD ["java", "-jar", "app.jar"]
