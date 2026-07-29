# ---- build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY . .

RUN ./gradlew bootJar --no-daemon

# ---- run stage ----
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
