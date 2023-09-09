FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
EXPOSE 8083
ARG APP_NAME=ms-zeeven.jar
ADD target/*.jar ms-zeeven.jar

ENTRYPOINT ["java", "-jar","--add-opens", "java.base/java.time=ALL-UNNAMED", "-Dspring.profiles.active=recette", "/ms-zeeven.jar"]
