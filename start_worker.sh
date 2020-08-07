#!/bin/bash
mkdir -p ./save_data/send >> /dev/null
mkdir -p ./save_data/run >> /dev/null


docker-compose  -f ./DockerFiles/worker.docker-compose.yml --project-directory ./  --env-file ./ConfigFiles/main.env up --build -V