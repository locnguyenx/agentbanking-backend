FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY services/auth-iam-service/build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]