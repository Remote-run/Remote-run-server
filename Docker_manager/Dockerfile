FROM ubuntu:latest

ENV SQLURL="jdbc:postgresql://localhost:54320/docker"
ENV POSGRESS_USER="postgres"
ENV POSGRESS_PASSWORD="mysecretpassword"

ENV SAVE_DIR_PATH="not_set"

RUN apt update
RUN apt install -y curl
RUN curl -sSL https://get.docker.com/ | sh

