FROM public.ecr.aws/amazoncorretto/amazoncorretto:17 AS build

WORKDIR /app

# Copy Gradle wrapper and build files for better caching
COPY gradle gradle
COPY ./gradlew build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew build --no-daemon -x test

# Runtime stage with JRE-only image
FROM public.ecr.aws/amazoncorretto/amazoncorretto:17-al2-native-headless

WORKDIR /app

# Create group and user (both id 10001) without installing packages
# /etc/group: name:passwd:GID:
# /etc/passwd: name:passwd:UID:GID:GECOS:home:shell
RUN echo 'appuser:x:10001:' >> /etc/group \
 && echo 'appuser:x:10001:10001:App User:/home/appuser:/sbin/nologin' >> /etc/passwd \
 && mkdir -p /home/appuser \
 && chown -R 10001:10001 /home/appuser

# Copy the jar and set ownership in one layer
COPY --chown=10001:10001 --from=build /app/build/libs/*.jar /app/app.jar

# Drop privileges
USER appuser
ENV HOME=/home/appuser

EXPOSE 8001

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]