#FROM nvidia/cuda:11.0-cudnn8-runtime-ubuntu18.04
FROM nvidia/cuda:10.2-cudnn7-runtime-ubuntu18.04

RUN apt update && apt install -y software-properties-common
RUN add-apt-repository -y ppa:openjdk-r/ppa
RUN apt update && apt install -y openjdk-14-jdk
RUN apt update && apt install -y maven


