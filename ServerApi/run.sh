#!/bin/bash

docker build . -t server_test:latest

docker run -it --rm \
-v /home/trygve/Development/projects/Run-on-server/Docker_manager/SAVE_DATA/run:/runvol \
-v send_volume:/send \
-p 127.0.0.1:8080:8080 \
--net=host \
--name=api_server \
server_test:latest
#/bin/bash
#catalina.sh run &


