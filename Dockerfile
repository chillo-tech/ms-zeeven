FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ADD target/*.jar ms-zeeven.jar
ENV JAVA_OPTS="--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod -Dspring-boot.run.jvmArguments='--add-opens java.base/java.lang=ALL-UNNAMED'", "/ms-zeeven.jar"]
