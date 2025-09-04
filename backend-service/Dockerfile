FROM public.ecr.aws/amazoncorretto/amazoncorretto:17 AS build

WORKDIR /app

# Copy Gradle wrapper and build files for better caching
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew build --no-daemon -x test

# Runtime stage with JRE-only image
FROM public.ecr.aws/amazoncorretto/amazoncorretto:17-headless

WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser
EXPOSE 8001

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]