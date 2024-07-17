# Use the official OpenJDK 17 image as the base image (it contains Java 17 needed for app)
FROM openjdk:17-alpine

# Set the working directory to /app in the container and automatically go there
WORKDIR /app

# Copy the application JAR file to /app
# First arg points to file on our system and second file is the destination inside container
COPY build/libs/softtrainer-backend-*.jar app.jar
COPY .env keystore.p12 docker-entrypoint.sh /app/

RUN chmod +x docker-entrypoint.sh

# DOCUMENT that app will work on port 8443 for incoming traffic
EXPOSE 8443

# Run the application when the container starts
ENTRYPOINT ["/app/docker-entrypoint.sh"]
