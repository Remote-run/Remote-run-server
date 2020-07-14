ARG image
ARG runfile
ARG args

FROM $image

WORKDIR /app/

RUN mkdir /app/save_data
RUN ln -s save_data /save

RUN pip install -r requirements.txt

CMD python3 $runfile $args


