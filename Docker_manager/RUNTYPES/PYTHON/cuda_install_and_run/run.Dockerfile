# the dir containing the java files to run is mounted to /app
# the out dir is mounted to /save
#

FROM tensorflow/tensorflow:latest-gpu

WORKDIR /app/

RUN mkdir /app/save_data
RUN ln -s save_data /save

RUN pip install tensorflow_datasets

CMD python3 text_clasification.py


