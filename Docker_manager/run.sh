#!/bin/bash

if [ ! -d ./SAVE_DATA ]
  then
    mkdir -p ./SAVE_DATA/{save,run,m2}
fi

save_data_path=$(realpath ./SAVE_DATA)

docker build . -t ros_command:latest

docker run -it --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $save_data_path:/save_data \
--build-arg SAVE_DIR_PATH=$save_data_path\
--name docker_manager\
ros_command:latest