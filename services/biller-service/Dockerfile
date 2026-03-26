FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY services/biller-service/build/libs/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]