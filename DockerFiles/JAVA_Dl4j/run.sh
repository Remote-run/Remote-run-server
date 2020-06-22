#!/bin/bash

docker image build . -t java_gpu_test

docker run --gpus all -it --rm \
-build-arg SharedDir=testvol \
-v ~/Development/projects/gpu_java_tf_test:/testABC \
java_gpu_test:latest

