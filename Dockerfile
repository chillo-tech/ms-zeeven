FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ADD target/*.jar ms-zeeven.jar
ENV JAVA_OPTS="--add-opens java.base/java.time=ALL-UNNAMED"
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/ms-zeeven.jar"]
