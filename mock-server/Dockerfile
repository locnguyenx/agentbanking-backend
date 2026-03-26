FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY mock-server/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]