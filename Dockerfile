# Allows you to run this app easily as a docker container.
# See README.md for more details.
#
# 1. Build the image with: docker build --no-cache -t test/evstats:latest --build-arg offlinekey="offline key" .
# 2. Run the image with: docker run --rm -ti -p8080:8080 test/evstats
#
# Uses Docker Multi-stage builds: https://docs.docker.com/build/building/multi-stage/

# The "Build" stage. Copies the entire project into the container, into the /vaadin-embedded-jetty-gradle/ folder, and builds it.
#FROM openjdk:21 AS BUILD
#COPY . /app/
#WORKDIR /app/
#ARG offlinekey
#ENV VAADIN_OFFLINE_KEY=$offlinekey
#RUN ./mvnw clean test package -Pproduction
# At this point, we have the app (executable jar file):  /app/target/evstats-1.0-SNAPSHOT.jar

# The "Run" stage. Start with a clean image, and copy over just the app itself, omitting gradle, npm and any intermediate build files.
FROM openjdk:21
COPY target/evstats-1.0-SNAPSHOT.jar /app/
WORKDIR /app/
EXPOSE 8080
ENTRYPOINT java -jar evstats-1.0-SNAPSHOT.jar 8080