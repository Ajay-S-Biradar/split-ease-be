# Step 1 — Build the app
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Step 2 — Run the app
FROM openjdk:17-jdk-slim
WORKDIR /app
# Based on settings.gradle.kts, the JAR name is splitease-backend-0.0.1-SNAPSHOT.jar
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
