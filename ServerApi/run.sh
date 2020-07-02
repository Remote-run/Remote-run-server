#!/bin/bash

docker build . -t server_test:latest

docker run -it --rm \
-v /home/trygve/Development/projects/run_on_server_savedata/run:/runvol \
-p 127.0.0.1:8080:8080 \
--net=host \
server_test:latest
#/bin/bash
#catalina.sh run &


