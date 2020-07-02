#!/bin/bash
docker build . -t ros_command:latest

docker run -it --rm \

-v /var/run/docker.sock:/var/run/docker.sock \
-v /home/trygve/Development/projects/run_on_server_savedata/run:/runvol \
ros_command:latest