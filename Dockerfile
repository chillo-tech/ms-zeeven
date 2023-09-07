FROM sapmachine:jdk-ubuntu-17
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ADD target/*.jar ms-zeeven.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/ms-zeeven.jar"]
