# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml pom.xml
COPY src src

RUN mvn package -DskipTests

# Stage 2: Run (using Quarkus UBI)
FROM quay.io/quarkus/quarkus-micro-image:2.0

WORKDIR /app

COPY --from=builder /app/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder /app/target/quarkus-app/*.jar /deployments/
COPY --from=builder /app/target/quarkus-app/app/ /deployments/app/
COPY --from=builder /app/target/quarkus-app/quarkus/ /deployments/quarkus/

ENV QUARKUS_HTTP_HOST=0.0.0.0
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/deployments/quarkus-run.jar"]