FROM cuda_openjdk:latest

ARG SharedDir
RUN mkdir -p /save_data
RUN ln -s /save_data /$SharedDir

WORKDIR /app/*
#CMD mvn install && mvn package && mvn -e exec:java | tee /$SharedDir/sdout
