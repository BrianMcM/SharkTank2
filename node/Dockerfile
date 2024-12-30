FROM openjdk:16-jdk-slim

WORKDIR /app

COPY /target/*.jar /app/module.jar

EXPOSE 8081

CMD ["java", "-jar", "/app/module.jar"]