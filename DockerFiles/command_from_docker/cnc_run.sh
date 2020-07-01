#!/bin/bash
docker build . -t ros_command:latest

docker run -it --rm \
-v /var/run/docker.sock:/var/run/docker.sock \
-v runvol:/runvol \
-build-arg SharedDir=testvol \
-build-arg SharedDir=testvol \
ros_command:latest


jdbc:postgresql://localhost:54320/postgres