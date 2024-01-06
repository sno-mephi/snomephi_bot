FROM openjdk:17-jdk-slim

ARG HOST_USER_UID=1000
ARG HOST_USER_GID=1000

RUN groupadd -g $HOST_USER_GID notroot                    && \
    useradd -l -u $HOST_USER_UID -g $HOST_USER_GID notroot

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN ./gradlew build

CMD ["java", "-jar", "build/libs/bot-template-app.jar"]