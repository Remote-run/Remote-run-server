docker run -it --rm \
    -e POSTGRES_PASSWORD=mysecretpassword \
    -e POSTGRES_USER=docker \
    -v /home/trygve/Development/projects/Run-on-server/DockerFiles/posgress/sql/posgress:/docker-entrypoint-initdb.d \
    -p 5432:5432 \
    --name postgresDB \
    postgres

  #    -v /home/trygve/Development/projects/Run-on-server/DockerFiles/posgress/sql/posgress:/docker-entrypoint-initdb.d \
