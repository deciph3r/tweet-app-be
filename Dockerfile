FROM openjdk:8-jdk-slim

LABEL maintainer="ahamed.abdullah00772@gmail.com"
LABEL author="Ahamed Abdullah"

EXPOSE 8080

WORKDIR /usr/local/bin/

COPY /target/tweet-app-0.0.1-SNAPSHOT.jar tweet-app.jar

CMD ["java","-jar","tweet-app.jar"]