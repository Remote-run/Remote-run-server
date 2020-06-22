FROM cuda_openjdk:latest

ARG SharedDir
RUN mkdir -p /run/save_data
RUN ln -s /run/save_data /$SharedDir
WORKDIR /code

ADD ./deeplearning4j-example-sample-1.0.0-beta7-bin.jar .
CMD java -cp /deeplearning4j-example-sample-1.0.0-beta7-bin.jar org.example.LeNetMNIST | tee /$SharedDir/sdout
