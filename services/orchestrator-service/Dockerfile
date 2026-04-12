FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY services/orchestrator-service/build/libs/orchestrator-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
