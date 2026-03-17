# Step 1 — Build the app
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Step 2 — Run the app
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Based on settings.gradle.kts, the JAR name is splitease-backend-0.0.1-SNAPSHOT.jar
COPY --from=build /app/build/libs/splitease-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
