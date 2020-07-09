docker run -it --rm \
    --name postgres_img \
    -e POSTGRES_PASSWORD=mysecretpassword \
    -e POSTGRES_USER=docker \
    -v /home/trygve/Development/projects/Run-on-server/DockerFiles/posgress/sql/posgress:/docker-entrypoint-initdb.d \
    -p 54320:5432 \
    --name postgresDB \
    postgres

  #    -v /home/trygve/Development/projects/Run-on-server/DockerFiles/posgress/sql/posgress:/docker-entrypoint-initdb.d \
