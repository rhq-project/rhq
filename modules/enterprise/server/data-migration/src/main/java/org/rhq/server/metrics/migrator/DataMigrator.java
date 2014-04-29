/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.server.metrics.migrator;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.server.metrics.CallTimeDAO;
import org.rhq.server.metrics.TraitsDAO;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.server.metrics.migrator.workers.AggregateDataMigrator;
import org.rhq.server.metrics.migrator.workers.CallTimeDataMigrator;
import org.rhq.server.metrics.migrator.workers.CallableMigrationWorker;
import org.rhq.server.metrics.migrator.workers.DeleteAllData;
import org.rhq.server.metrics.migrator.workers.RawDataMigrator;
import org.rhq.server.metrics.migrator.workers.TraitDataMigrator;

import com.datastax.driver.core.Session;


/**
 * @author Stefan Negrea
 *
 */
public class DataMigrator {

    public enum DatabaseType {
        Postgres, Oracle
    }

    public enum Task {
        Migrate, Estimate
    }

    private interface RunnableWithException extends Runnable {
        Exception getException();
    }

    private final Log log = LogFactory.getLog(DataMigrator.class);

    public static final double UNDER_ESTIMATION_FACTOR = .15;
    public static final int SQL_TIMEOUT = 6000000;
    public static final int MAX_NUMBER_OF_FAILURES = 5;

    private final DataMigratorConfiguration config;
    private long estimation;

    public DataMigrator(EntityManager entityManager, Session session, DatabaseType databaseType) {
        this(entityManager, session, databaseType, false);
    }

    public DataMigrator(EntityManager entityManager, Session session, DatabaseType databaseType,
        boolean experimentalDataSource) {

        config = new DataMigratorConfiguration(entityManager, session, databaseType, experimentalDataSource);
        config.setDeleteDataImmediatelyAfterMigration(false);
        config.setDeleteAllDataAtEndOfMigration(false);
        config.setRunRawDataMigration(true);
        config.setRun1HAggregateDataMigration(true);
        config.setRun6HAggregateDataMigration(true);
        config.setRun1DAggregateDataMigration(true);
        config.setRunCallTimeMigration(true);
        config.setRunTraitMigration(true);
    }

    public void runRawDataMigration(boolean value) {
        config.setRunRawDataMigration(value);
    }

    public void run1HAggregateDataMigration(boolean value) {
        config.setRun1HAggregateDataMigration(value);
    }

    public void run6HAggregateDataMigration(boolean value) {
        config.setRun6HAggregateDataMigration(value);
    }

    public void run1DAggregateDataMigration(boolean value) {
        config.setRun1DAggregateDataMigration(value);
    }

    public void runTraitMigration(boolean value) {
        config.setRunTraitMigration(value);
    }

    public void runCallTimeMigration(boolean value) {
        config.setRunCallTimeMigration(value);
    }

    public void deleteDataImmediatelyAfterMigration() {
        config.setDeleteDataImmediatelyAfterMigration(true);
        config.setDeleteAllDataAtEndOfMigration(false);
    }

    public void deleteAllDataAtEndOfMigration() {
        config.setDeleteAllDataAtEndOfMigration(true);
        config.setDeleteDataImmediatelyAfterMigration(false);
    }

    public void preserveData() {
        config.setDeleteAllDataAtEndOfMigration(false);
        config.setDeleteDataImmediatelyAfterMigration(false);
    }

    public long estimate() throws Exception {
        this.estimation = 0;
        if (config.isRunRawDataMigration()) {
            retryOnFailure(new RawDataMigrator(config), Task.Estimate);
        }

        if (config.isRun1HAggregateDataMigration()) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.ONE_HOUR, config), Task.Estimate);
        }

        if (config.isRun6HAggregateDataMigration()) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.SIX_HOUR, config), Task.Estimate);
        }

        if (config.isRun1DAggregateDataMigration()) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.TWENTY_FOUR_HOUR, config), Task.Estimate);
        }

        if (config.isRunTraitMigration()) {
            retryOnFailure(new TraitDataMigrator(TraitsDAO.TABLE, config.getTraitTTLDays(), config), Task.Estimate);
        }

        if (config.isRunCallTimeMigration()) {
            retryOnFailure(new CallTimeDataMigrator(CallTimeDAO.TABLE, config.getCallTimeDays(), config), Task.Estimate);
        }

        if (config.isDeleteAllDataAtEndOfMigration()) {
            retryOnFailure(new DeleteAllData(config), Task.Estimate);
        }

        estimation = (long) (estimation + estimation * UNDER_ESTIMATION_FACTOR);

        return estimation;
    }

    public void migrateData() throws Exception {
        if (config.isRunRawDataMigration()) {
            retryOnFailure(new RawDataMigrator(config), Task.Migrate);
        }

        if (config.isRun1HAggregateDataMigration()) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.ONE_HOUR, config), Task.Migrate);
        }

        if (config.isRun6HAggregateDataMigration()) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.SIX_HOUR, config), Task.Migrate);
        }

        if (config.isRun1DAggregateDataMigration()) {
            retryOnFailure(new AggregateDataMigrator(MetricsTable.TWENTY_FOUR_HOUR, config), Task.Migrate);
        }

        if (config.isRunTraitMigration()) {
            retryOnFailure(new TraitDataMigrator(TraitsDAO.TABLE, config.getTraitTTLDays(), config), Task.Migrate);
        }

        if (config.isRunCallTimeMigration()) {
            retryOnFailure(new CallTimeDataMigrator(CallTimeDAO.TABLE, config.getCallTimeDays(), config), Task.Migrate);
        }

        if (config.isDeleteAllDataAtEndOfMigration()) {
            retryOnFailure(new DeleteAllData(config), Task.Migrate);
        }
    }

    public void deleteOldData() throws Exception {
        if (config.isDeleteAllDataAtEndOfMigration()) {
            retryOnFailure(new DeleteAllData(config), Task.Migrate);
        }
    }

    /**
     * Retries the migration {@link #MAX_NUMBER_OF_FAILURES} times before
     * failing the migration operation.
     *
     * @param migrator
     * @throws Exception
     */
    private Thread retryOnFailure(final CallableMigrationWorker migrator, final Task task)
        throws Exception {

        RunnableWithException runnable = new RunnableWithException() {
            private Exception exception;

            @Override
            public void run() {
                int numberOfFailures = 0;
                Exception caughtException = null;

                log.info(migrator.getClass());

                while (numberOfFailures < MAX_NUMBER_OF_FAILURES) {
                    try {
                        if (task == Task.Estimate) {
                            estimation += migrator.estimate();
                        } else {
                            migrator.migrate();
                        }
                        return;
                    } catch (Exception e) {
                        log.error("Migrator " + migrator.getClass() + " failed. Retrying!", e);

                        caughtException = e;
                        numberOfFailures++;
                    }
                }

                this.exception = caughtException;
            }

            @Override
            public Exception getException() {
                return this.exception;
            }
        };

        Thread localThread = new Thread(runnable);
        localThread.start();
        localThread.join();

        if (runnable.getException() != null) {
            throw runnable.getException();
        }

        return localThread;
    }

    public class DataMigratorConfiguration {

        private final EntityManager entityManager;
        private final Session session;
        private final DatabaseType databaseType;
        private final boolean experimentalDataSource;

        int TTL_DEFAULT = 180;

        private boolean deleteDataImmediatelyAfterMigration;
        private boolean deleteAllDataAtEndOfMigration;

        private boolean runRawDataMigration;
        private boolean run1HAggregateDataMigration;
        private boolean run6HAggregateDataMigration;
        private boolean run1DAggregateDataMigration;
        private boolean runTraitMigration = true;
        private boolean runCallTimeMigration = true;
        private int traitTTLDays = TTL_DEFAULT;
        private int callTimeDays = TTL_DEFAULT;

        public DataMigratorConfiguration(EntityManager entityManager, Session session, DatabaseType databaseType,
            boolean experimentalDataSource) {
            this.entityManager = entityManager;
            this.session = session;
            this.databaseType = databaseType;
            this.experimentalDataSource = experimentalDataSource;
        }

        public boolean isDeleteDataImmediatelyAfterMigration() {
            return deleteDataImmediatelyAfterMigration;
        }

        private void setDeleteDataImmediatelyAfterMigration(boolean deleteDataImmediatelyAfterMigration) {
            this.deleteDataImmediatelyAfterMigration = deleteDataImmediatelyAfterMigration;
        }

        public boolean isDeleteAllDataAtEndOfMigration() {
            return deleteAllDataAtEndOfMigration;
        }

        private void setDeleteAllDataAtEndOfMigration(boolean deleteAllDataAtEndOfMigration) {
            this.deleteAllDataAtEndOfMigration = deleteAllDataAtEndOfMigration;
        }

        public boolean isRunRawDataMigration() {
            return runRawDataMigration;
        }

        private void setRunRawDataMigration(boolean runRawDataMigration) {
            this.runRawDataMigration = runRawDataMigration;
        }

        public boolean isRun1HAggregateDataMigration() {
            return run1HAggregateDataMigration;
        }

        private void setRun1HAggregateDataMigration(boolean run1hAggregateDataMigration) {
            run1HAggregateDataMigration = run1hAggregateDataMigration;
        }

        public boolean isRun6HAggregateDataMigration() {
            return run6HAggregateDataMigration;
        }

        private void setRun6HAggregateDataMigration(boolean run6hAggregateDataMigration) {
            run6HAggregateDataMigration = run6hAggregateDataMigration;
        }

        public boolean isRun1DAggregateDataMigration() {
            return run1DAggregateDataMigration;
        }

        private void setRun1DAggregateDataMigration(boolean run1dAggregateDataMigration) {
            run1DAggregateDataMigration = run1dAggregateDataMigration;
        }

        public boolean isRunTraitMigration() {
            return runTraitMigration;
        }

        public void setRunTraitMigration(boolean runTraitMigration) {
            this.runTraitMigration = runTraitMigration;
        }

        public boolean isRunCallTimeMigration() {
            return runCallTimeMigration;
        }

        public void setRunCallTimeMigration(boolean runCallTimeMigration) {
            this.runCallTimeMigration = runCallTimeMigration;
        }

        public int getTraitTTLDays() {
            return traitTTLDays;
        }

        public void setTraitTTLDays(int traitTTLDays) {
            this.traitTTLDays = traitTTLDays;
        }

        public int getCallTimeDays() {
            return callTimeDays;
        }

        public void setCallTimeDays(int callTimeDays) {
            this.callTimeDays = callTimeDays;
        }

        public EntityManager getEntityManager() {
            return entityManager;
        }

        public Session getSession() {
            return session;
        }

        public DatabaseType getDatabaseType() {
            return databaseType;
        }

        public boolean isExperimentalDataSource() {
            return experimentalDataSource;
        }

    }

}
