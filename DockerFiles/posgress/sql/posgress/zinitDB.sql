
CREATE TYPE run_phase AS ENUM ('WAITING', 'RUNNING', 'STOPPED', 'DONE');


/*
    when a request comes in, the config is parsed and the request is places in this table,

*/
CREATE TABLE IF NOT EXISTS tickets(
    id           uuid primary key    not null,
    return_mail  text                not null,

    run_type     text,
    run_priority int,

    timestamp     int DEFAULT extract(epoch from now()),
    status        run_phase DEFAULT ('WAITING')
);

/*
    when there are free capacity in the system a request to this table is made and
    the most recent entry of the highest priority is started
 */
CREATE VIEW  priority_run_que as
    SELECT id, run_priority
    from tickets
    order by run_priority ,timestamp;

CREATE VIEW  running as
    SELECT id, status
    FROM tickets
    WHERE status = 'RUNNING';


/*
    this is a table of whats currently running
    will make more sence when recource usage is implemented

CREATE TABLE active(
    id             int references tickets(id),
    done           bool                            not null
);

 */
/*

 */
CREATE TABLE IF NOT EXISTS out(
    id         int references tickets(id) ON DELETE CASCADE,
    kill_at    int DEFAULT  extract(epoch from now() + interval '7 days')
);

