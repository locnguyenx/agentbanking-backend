FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY build.gradle .
COPY src src
COPY gradlew .
COPY gradle gradle
RUN ./gradlew :mock-server:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/mock-server/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
