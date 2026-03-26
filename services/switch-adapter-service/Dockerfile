FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY services/switch-adapter-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]