FROM ubuntu:latest

RUN apt update
RUN apt install -y curl
RUN curl -sSL https://get.docker.com/ | sh
RUN apt install -y openjdk-14-jre

ENV SQLURL="jdbc:postgresql://localhost:54320/docker"
ENV POSGRESS_USER="docker"
ENV POSGRESS_PASSWORD="mysecretpassword"

ENV SAVE_DIR_PATH="not_set"
#ENV DOCKER_FILES_PATH="not_set"

CMD java -jar /app/Docker_manager-1.0-SNAPSHOT-jar-with-dependencies.jar

