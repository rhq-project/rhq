CREATE KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE rhq;

CREATE TABLE raw_metrics (
    schedule_id int,
    time timestamp,
    value double,
    PRIMARY KEY (schedule_id, time)
);

CREATE TABLE one_hour_metrics (
    schedule_id int,
    time timestamp,
    type int,
    value double,
    PRIMARY KEY (schedule_id, time, type)
);

CREATE TABLE metrics_work_queue (
    bucket varchar,
    time timestamp,
    schedule_id int,
    null_col boolean,
    PRIMARY KEY (bucket, time, schedule_id)
);
