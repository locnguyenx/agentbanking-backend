FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY common common
COPY gateway gateway
COPY gradlew .
COPY gradle gradle
RUN ./gradlew :gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
