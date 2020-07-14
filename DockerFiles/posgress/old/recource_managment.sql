
-- Tenke æ vente med det her til en virkende prototype

CREATE TABLE available_resources(
    server_id    serial primary key,
    cpus         int                 not null,
    ram_gb       int                 not null,
    gpu          int                 not null
);

CREATE TABLE resource_usage_tickets(
    id           serial primary key  not null,
    server_id    int references available_resources(server_id),
    cpus         int                 not null,
    ram_gb       int                 not null,
    gpu          int                 not null
);


CREATE OR REPLACE FUNCTION check_recources()
RETURNS  TRIGGER AS $$



    $$ LANGUAGE plpgsql

