FROM openjdk:17-jdk-slim
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ARG JAR_FILE=target/*.jar
ADD ${JAR_FILE} ms-zeeven.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=recette", "/ms-zeeven.jar"]
