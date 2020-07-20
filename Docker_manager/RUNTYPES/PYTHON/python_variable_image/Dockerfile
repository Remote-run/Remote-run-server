ARG image


FROM $image


RUN apt update
RUN apt install -y python3
RUN apt install -y python3-pip

COPY requirements.txt /tmp/requirements.txt

RUN pip3 install -r /tmp/requirements.txt

WORKDIR /app/

RUN mkdir /app/save_data
RUN ln -s save_data /save

ENV runfile="main.py"
ENV args=""

CMD python3 $runfile $args


