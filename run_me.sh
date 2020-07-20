#!/bin/bash

if [ ! -d ./save_data ]
  then
    mkdir -p ./save_data/{save,run,build_helpers/{java/m2_repo, build_hole}}
fi

export SAVE_DATA_SYS_PATH=$(realpath ./save_data)

docker network create ticketNetwork >> /dev/null
docker-compose build
docker-compose up