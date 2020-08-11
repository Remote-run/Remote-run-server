
/*
 This is the different resourse keys which describes an ammounnt or compute resource boud to a key
 */
CREATE TABLE IF NOT EXISTS resource_keys(
    id              text primary key not null,
    gpu             int,
    cpu             int,
    gig_ram         int,
    timeout_Seconds int
);

/*
    when a request comes in, the config is parsed and the request is places in this table,

*/
CREATE TABLE IF NOT EXISTS tickets(
    id           uuid primary key    not null,
    return_mail  text                not null,

    run_type     text,
    run_priority int,

    timestamp     bigint DEFAULT extract(epoch from now()),
    status        text DEFAULT ('WAITING'),
    resource_key  text references resource_keys(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS compute_nodes(
    id              uuid primary key    not null,
    last_check_in   bigint DEFAULT extract(epoch from now()),
    resource_key    text references resource_keys(id) ON DELETE RESTRICT
);


/*
 These are the tickets that are to be started and the compute node id of who to start it
 */
CREATE TABLE IF NOT EXISTS active_ticket(
    ticket_id           uuid references tickets(id) UNIQUE,
    runner              uuid references compute_nodes(id) ON DELETE CASCADE
);



CREATE TABLE IF NOT EXISTS out(
    id          uuid references tickets(id) ON DELETE CASCADE UNIQUE,
    exit_reason text,
    mail_sent   boolean DEFAULT FALSE,
    kill_at     bigint DEFAULT  extract(epoch from now() + interval '7 days')
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


DELETE FROM resource_keys k
WHERE EXISTS (SELECT k.id FROM resource_keys k WHERE k.id NOT IN (SELECT  resource_key FROM compute_nodes  UNION ALL SELECT resource_key FROM tickets));



SELECT a.ticket_id
FROM active_ticket a
left join tickets t on t.id = a.ticket_id
WHERE a.runner like
ORDER BY t.run_priority ,t.timestamp ;



SELECT o.id, t.return_mail
FROM out o
left join tickets t on t.id = o.id
WHERE mail_sent =FALSE;

SELECT compute_nodes.id, count(t.id)
FROM compute_nodes
LEFT JOIN active_ticket a ON compute_nodes.id = a.runner
LEFT JOIN tickets t ON t.id = a.ticket_id
WHERE status LIKE 'WAITING'
GROUP BY compute_nodes.id
ORDER BY count(t.id)


UPDATE tickets
SET status='WAITING'
WHERE id IN (SELECT ticket_id FROM active_ticket where runner=)
*?
 */