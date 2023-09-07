FROM amazoncorretto:17-alpine3.17-jdk
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ADD target/*.jar ms-zeeven.jar
ENV JAVA_OPTS="--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
ENTRYPOINT ["java", "-jar", "-DjvmArguments='--add-opens java.base/java.lang=ALL-UNNAMED' -Dspring.profiles.active=prod", "/ms-zeeven.jar"]
