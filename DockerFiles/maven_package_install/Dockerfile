# the dir containing the java files to run is mounted to /app
# the out dir is mounted to /save
#

FROM cuda_openjdk:latest

WORKDIR /app/

RUN mkdir -p /save_data
RUN ln -s save_data /save
CMD mvn install && mvn package && mvn -e exec:java | tee /save/sdout
