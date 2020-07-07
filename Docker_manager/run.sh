#!/bin/bash

if [ ! -d ./SAVE_DATA ]
  then
    mkdir -p ./SAVE_DATA/{save,run,m2}
fi

save_data_path=$(realpath ./SAVE_DATA)

docker build . -t docker_manager:latest

docker run -it --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $save_data_path:/save_data \
-v send_volume:/send \
--build-arg SAVE_DIR_PATH=$save_data_path\
--name docker_manager\
docker_manager:latest