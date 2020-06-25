#!/bin/bash

docker build . -t server_test:latest --build-arg SharedDir=/testABC

docker run -it --rm \
-v testvol:/testABC \
-v runvol:/runvol \
-p 127.0.0.1:8080:8080 \
server_test:latest

