FROM openjdk:17-jdk-slim

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN ./gradlew build

CMD ["java", "-jar", "build/libs/bot-template-app.jar"]