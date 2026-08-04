
# ---- Build Stage ----
 
# Start from an image that has both Maven and Java 21 (Temurin JDK) installed.
# This stage is only for compiling/building the app — it's discarded after.
FROM maven:3.9.9-eclipse-temurin-21 AS builder
 
# Set the working directory inside the container for this stage.
WORKDIR /build
 
# Copy only pom.xml first (not the source code yet).
# This lets Docker cache the downloaded dependencies as a separate layer,
# so they aren't re-downloaded every time source code changes.
COPY pom.xml .
 
# Download all project dependencies ahead of time, using the cached pom.xml layer.
RUN mvn dependency:go-offline        # cache deps first
 
# Now copy the actual source code into the container.
COPY src ./src
 
# Compile the code and package it into a runnable .jar file.
# -DskipTests skips running tests during the Docker build (tests should run in CI instead).
RUN mvn clean package -DskipTests
 
# ---- Runtime Stage ----
 
# Start a fresh image with just the JRE — none of the Maven build tools
# from Stage 1 are included here, keeping the final image smaller.
FROM eclipse-temurin:21-jre
 
# Set the working directory for the running application.
WORKDIR /app
 
# Copy only the built .jar file from the "builder" stage above into this image.
COPY --from=builder /build/target/*.jar app.jar
 
# Tell Docker the app listens on port 6002 inside the container.
EXPOSE 6001
 
# The command that runs when the container starts: launches the Spring Boot app.
ENTRYPOINT ["java", "-jar", "app.jar"]
