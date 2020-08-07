# the dir containing the java files to run is mounted to /app
# the out dir is mounted to /save
#

FROM cuda_openjdk:latest

WORKDIR /app/

RUN mkdir /app/save_data
RUN ln -s save_data /save
COPY ./java/m2_repo /root/.m2/repository

CMD mvn compile && mvn exec:java | tee /save/sdout


