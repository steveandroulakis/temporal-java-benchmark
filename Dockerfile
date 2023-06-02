# Use an official Gradle image from the Docker Hub
FROM gradle:7.3.0-jdk11 AS build

# Set the working directory
WORKDIR /home/gradle/project

# Copy the Gradle configuration files first for leveraging Docker cache
# and avoid the downloading of dependencies on each build
COPY build.gradle settings.gradle gradlew ./

# Copy the gradle wrapper JAR and properties files
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Now run gradle assemble to download dependencies and build the application
RUN chmod +x ./gradlew
RUN ./gradlew build

# Use a JDK base image for running the gradle task
FROM adoptopenjdk:11-jdk-hotspot

WORKDIR /app

# Copy the build output from the builder stage
COPY --from=build /home/gradle/project/build /app/build

# Copy the source code
COPY --from=build /home/gradle/project/src /app/src

# Copy the gradlew and settings files
COPY --from=build /home/gradle/project/gradlew /home/gradle/project/settings.gradle /home/gradle/project/build.gradle /app/
COPY --from=build /home/gradle/project/gradle /app/gradle

# Expose the application port
EXPOSE 9090

# Set an environment variable TASK that defaults to startWorker
ENV TASK=startWorker
ENV NUM_WORKFLOWS=1

# Run the specified task
CMD ["sh", "-c", "./gradlew $TASK -PnumWorkflows=$NUM_WORKFLOWS"]
