#!/bin/bash
docker build . -t ros_command:latest

docker run -it --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v runvol:/runvol \
ros_command:latest