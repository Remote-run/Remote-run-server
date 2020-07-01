docker run -d --rm \
    --name postgres_img \
    -e POSTGRES_PASSWORD=mysecretpassword \
    -e PGDATA=/var/lib/postgresql/data/pgdata \
    -v db_data:/var/lib/postgresql/data \
    -p 54320:5432 \
    postgres