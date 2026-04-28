# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml pom.xml
COPY src src
RUN mvn package -DskipTests

# Stage 2: Run
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24
WORKDIR /app
COPY --from=builder /app/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder /app/target/quarkus-app/*.jar /deployments/
COPY --from=builder /app/target/quarkus-app/app/ /deployments/app/
COPY --from=builder /app/target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]