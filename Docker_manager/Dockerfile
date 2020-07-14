FROM ubuntu:latest

RUN apt update
RUN apt install -y curl
RUN curl -sSL https://get.docker.com/ | sh
RUN apt install -y openjdk-14-jre

ENV SQLURL="jdbc:postgresql://localhost:54320/docker"
ENV POSGRESS_USER="docker"
ENV POSGRESS_PASSWORD="mysecretpassword"

ENV SAVE_DATA_SYS_PATH="not_set"
COPY ./target/Docker_manager-1.0-SNAPSHOT-jar-with-dependencies.jar /app/docker_manager.jar

CMD java -jar /app/docker_manager.jar

