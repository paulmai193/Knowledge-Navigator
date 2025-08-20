FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/backend-service-1.0.0.jar app.jar

EXPOSE 8001

CMD ["java", "-jar", "app.jar"]