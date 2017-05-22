CREATE KEYSPACE IF NOT EXISTS issue_tracker WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

CREATE TABLE IF NOT EXISTS issue_tracker.issues (
  id timeuuid,
  summary varchar,
  description text,
  date_updated varchar,
  issue_status text,
  PRIMARY KEY ((date_updated), id)
) WITH CLUSTERING ORDER BY (id ASC);