alter table rhq.raw_metrics with compression = {'sstable_compression': ''};
alter table rhq.one_hour_metrics with compression = {'sstable_compression': ''};
alter table rhq.six_hour_metrics with compression = {'sstable_compression': ''};
alter table rhq.twenty_four_hour_metrics with compression = {'sstable_compression': ''};
alter table rhq.metrics_index with compression = {'sstable_compression': ''};
alter table rhq.schema_version with compression = {'sstable_compression': ''};

alter table system_auth.credentials with compression = {'sstable_compression': ''};
alter table system_auth.permissions with compression = {'sstable_compression': ''};
alter table system_auth.users with compression = {'sstable_compression': ''};
alter table system_auth.users with compression = {'sstable_compression': ''};

alter table system_traces.events with compression = {'sstable_compression': ''};
alter table system_traces.sessions with compression = {'sstable_compression': ''};
alter table system_traces.sessions with compression = {'sstable_compression': ''};

alter table system."HintsColumnFamily" with compression = {'sstable_compression': ''};
alter table system."IndexInfo" with compression = {'sstable_compression': ''};
alter table system."LocationInfo" with compression = {'sstable_compression': ''};
alter table system."Migrations" with compression = {'sstable_compression': ''};
alter table system."NodeIdInfo" with compression = {'sstable_compression': ''};
alter table system."Schema" with compression = {'sstable_compression': ''};
alter table system.batchlog with compression = {'sstable_compression': ''};
alter table system.hints with compression = {'sstable_compression': ''};
alter table system.local with compression = {'sstable_compression': ''};
alter table system.peer_events with compression = {'sstable_compression': ''};
alter table system.peers with compression = {'sstable_compression': ''};
alter table system.range_xfers with compression = {'sstable_compression': ''};
alter table system.schema_columnfamilies with compression = {'sstable_compression': ''};
alter table system.schema_columns with compression = {'sstable_compression': ''};
alter table system.schema_keyspaces with compression = {'sstable_compression': ''};
