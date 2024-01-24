FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ARG JAR_FILE=target/ms-zeeven.jar

ADD  ${JAR_FILE} ms-zeeven.jar
ENTRYPOINT ["java", "-jar","--add-opens", "java.base/java.time=ALL-UNNAMED", "-Dspring.profiles.active=prod", "/ms-zeeven.jar"]
