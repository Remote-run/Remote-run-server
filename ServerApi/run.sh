#!/bin/bash

docker build . -t server_test:latest

docker run -it --rm \
-build-arg SharedDir=/testABC \
-v testvol:/testABC \
-p 127.0.0.1:8080:8080 \
server_test:latest

