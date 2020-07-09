#!/bin/bash

if [ ! -d ./SAVE_DATA ]
  then
    mkdir -p ./SAVE_DATA/{save,run,m2/repository}
fi

save_data_path=$(realpath ./SAVE_DATA)
docker_files_path=$(realpath ./RUNTYPES)
t=$(realpath ./target)
# -v $save_data_path:/save_data \
docker build . -t docker_manager:latest

docker run -it --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v /home/trygve/Development/projects/run_on_server_savedata:/save_data \
-v $docker_files_path:/runtypes \
-v send_volume:/send \
-v $t:/app \
--net=host \
--name docker_manager \
docker_manager:latest
#/bin/bash