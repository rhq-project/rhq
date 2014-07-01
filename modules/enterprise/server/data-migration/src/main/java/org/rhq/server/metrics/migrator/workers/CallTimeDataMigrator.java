package org.rhq.server.metrics.migrator.workers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.server.metrics.CallTimeConfiguration;
import org.rhq.server.metrics.CallTimeDAO;
import org.rhq.server.metrics.StorageSession;
import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;
import org.rhq.server.metrics.migrator.DataMigrator.Task;
import org.rhq.server.metrics.migrator.datasources.ExistingDataSource;

/**
 * Migrate call time data.
 */
public class CallTimeDataMigrator extends AbstractMigrationWorker implements CallableMigrationWorker {

    private final Log log = LogFactory.getLog(CallTimeDataMigrator.class);

    private final String table;
    private final CallTimeDAO dao;

    public CallTimeDataMigrator(String table, int ttlDays, DataMigratorConfiguration config) throws Exception {
        super(config);
        this.table = table;
        CallTimeConfiguration ctconfig = new CallTimeConfiguration();
        ctconfig.setTTLDays(ttlDays);
        ctconfig.setIdempotentInsert(true);
        this.dao = new CallTimeDAO(new StorageSession(config.getSession()), ctconfig);
    }

    @Override
    public long estimate() throws Exception {
        long recordCount = this.getRowCount(MigrationQuery.COUNT_CALL_TIME_DATA.getQuery());
        log.debug("Retrieved record count for table " + table + " -- " + recordCount);

        Telemetry telemetry = this.performMigration(Task.Estimate);
        long estimatedTimeToMigrate = telemetry.getMigrationTime();

        long estimation = (recordCount / (long) MAX_RECORDS_TO_LOAD_FROM_SQL / (long) NUMBER_OF_BATCHES_FOR_ESTIMATION)
            * estimatedTimeToMigrate;

        estimation += telemetry.getNonMigrationTime();

        return estimation;
    }

    public void migrate() throws Exception {
        performMigration(Task.Migrate);
        if (config.isDeleteDataImmediatelyAfterMigration()) {
            deleteTableData(MigrationQuery.DELETE_CALL_TIME_DATA_VALUE.getQuery());
            deleteTableData(MigrationQuery.DELETE_CALL_TIME_DATA_KEY.getQuery());
        }
    }

    private Telemetry performMigration(Task task) throws Exception {
        Telemetry telemetry = new Telemetry();
        telemetry.getGeneralTimer().start();

        long numberOfBatchesMigrated = 0;

        List<Object[]> existingData;
        int failureCount;

        int lastMigratedRecord = 0;
        ExistingDataSource dataSource = getExistingDataSource(MigrationQuery.SELECT_CALL_TIME_DATA.getQuery(), task);
        dataSource.initialize();

        telemetry.getMigrationTimer().start();
        while (true) {
            existingData = dataSource.getData(lastMigratedRecord, MAX_RECORDS_TO_LOAD_FROM_SQL);

            if (existingData.size() == 0) {
                break;
            }

            lastMigratedRecord += existingData.size();

            failureCount = 0;
            while (failureCount < MAX_NUMBER_OF_FAILURES) {
                try {
                    insertDataToCassandra(existingData);
                    break;
                } catch (Exception e) {
                    log.error("Failed to insert " + table
                        + " data. Attempting to insert the current batch of data one more time");
                    log.error(e);

                    failureCount++;
                    if (failureCount == MAX_NUMBER_OF_FAILURES) {
                        throw e;
                    }
                }
            }

            log.info("- " + table + " - " + lastMigratedRecord + " -");

            numberOfBatchesMigrated++;
            if (Task.Estimate.equals(task) && numberOfBatchesMigrated >= NUMBER_OF_BATCHES_FOR_ESTIMATION) {
                break;
            }
        }

        telemetry.getMigrationTimer().stop();

        dataSource.close();
        telemetry.getGeneralTimer().stop();

        return telemetry;
    }

    private void insertDataToCassandra(List<Object[]> existingData) throws Exception {
        Collection<Future> futures = new ArrayList<Future>();
        for (Object[] o : existingData) {
            // "select schedule_id, call_destination, begin_time, end_time, minimum, maximum, total, \"count\"" +
            int i = 0;
            int scheduleId = ((Number)o[i++]).intValue();
            String dest = o[i++].toString();
            Date begin = date(o[i++]);
            Date end = date(o[i++]);
            double min = ((Number)o[i++]).doubleValue();
            double max = ((Number)o[i++]).doubleValue();
            int total = ((Number)o[i++]).intValue();
            long count = ((Number)o[i++]).longValue();
            CallTimeData ctd = new CallTimeData(scheduleId);
            ctd.addAggregatedCallData(dest, begin, end, min, max, total, count);
            futures.add(dao.insert(ctd));
        }
        for (Future f : futures) {
            f.get();
        }
    }
    
}
