# the dir containing the java files to run is mounted to /app
# the out dir is mounted to /save
#

FROM cuda_openjdk:latest

WORKDIR /app/

CMD mvn dependency:sources & mvn dependency:resolve-plugins
