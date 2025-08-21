FROM public.ecr.aws/amazoncorretto/amazoncorretto:17 AS build

# Install Gradle
RUN yum update -y && yum install -y wget unzip
RUN wget https://services.gradle.org/distributions/gradle-8.5-bin.zip
RUN unzip gradle-8.5-bin.zip && mv gradle-8.5 /opt/gradle
ENV PATH="/opt/gradle/bin:${PATH}"

WORKDIR /app

# Copy build file
COPY build.gradle .

# Copy source code
COPY src src

# Build the application
RUN gradle build --no-daemon

# Runtime stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:17

WORKDIR /app

COPY --from=build /app/build/libs/backend-service-1.0.0.jar app.jar

EXPOSE 8001

CMD ["java", "-jar", "app.jar"]