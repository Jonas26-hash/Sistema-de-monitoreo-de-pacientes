# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY backend/ms-config-server/pom.xml pom.xml
COPY backend/ms-config-server/src src
COPY backend/ms-config-server/.mvn .mvn

RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
COPY backend/central-config central-config

ENV CENTRAL_CONFIG_PATH=file:/app/central-config/config-properties

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
