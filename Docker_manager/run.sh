#!/bin/bash

if [ ! -d ./SAVE_DATA ]
  then
    mkdir -p ./SAVE_DATA/{save,run,build_helpers/java/m2_repo}
fi

save_data_path=$(realpath ./SAVE_DATA)
docker_files_path=$(realpath ./RUNTYPES)
t=$(realpath ./target)
# -v $save_data_path:/save_data \
docker build . -t docker_manager:latest

docker run -it --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v $PATH/save_data:/save_data \
-v $PATH/RUNTYPES:/runtypes \
-e SAVE_DATA_SYS_PATH=$PATH/save_data \
-e "SQLURL=jdbc:postgresql://10.10.10.6:5432/docker" \
--name docker_manager \
docker_manager:latest
#/bin/bash