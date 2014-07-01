/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.server.metrics.migrator.workers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StatelessSession;

import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;

/**
 * @author Stefan Negrea
 *
 */
public class DeleteAllData extends AbstractMigrationWorker implements CallableMigrationWorker {

    private final Log log = LogFactory.getLog(DeleteAllData.class);

    public DeleteAllData(DataMigratorConfiguration config) {
        super(config);
    }

    public void migrate() {
        org.hibernate.Query nativeQuery;
        StatelessSession session = getSQLSession();

        if (config.isRun1HAggregateDataMigration()) {
            session.getTransaction().begin();
            nativeQuery = session.createSQLQuery(MigrationQuery.DELETE_1H_DATA.toString());
            nativeQuery.executeUpdate();
            session.getTransaction().commit();
            log.info("- RHQ_MEASUREMENT_DATA_NUM_1H - Cleaned -");
        }

        if (config.isRun6HAggregateDataMigration()) {
            session.getTransaction().begin();
            nativeQuery = session.createSQLQuery(MigrationQuery.DELETE_6H_DATA.toString());
            nativeQuery.executeUpdate();
            session.getTransaction().commit();
            log.info("- RHQ_MEASUREMENT_DATA_NUM_6H - Cleaned -");
        }

        if (config.isRun1DAggregateDataMigration()) {
            session.getTransaction().begin();
            nativeQuery = session.createSQLQuery(MigrationQuery.DELETE_1D_DATA.toString());
            nativeQuery.executeUpdate();
            session.getTransaction().commit();
            log.info("- RHQ_MEASUREMENT_DATA_NUM_1D - Cleaned -");
        }

        if (config.isRunRawDataMigration()) {
            for (String table : getRawDataTables()) {
                session.getTransaction().begin();
                String deleteAllData = String.format(MigrationQuery.DELETE_RAW_ALL_DATA.toString(), table);
                nativeQuery = session.createSQLQuery(deleteAllData);
                nativeQuery.executeUpdate();
                session.getTransaction().commit();
                log.info("- " + table + " - Cleaned -");
            }
        }

        if (config.isRunTraitMigration()) {
            session.getTransaction().begin();
            nativeQuery = session.createSQLQuery(MigrationQuery.DELETE_TRAIT_DATA.getQuery());
            nativeQuery.executeUpdate();
            session.getTransaction().commit();
            log.info("- trait data - Cleaned -");
        }

        if (config.isRunCallTimeMigration()) {
            session.getTransaction().begin();
            nativeQuery = session.createSQLQuery(MigrationQuery.DELETE_CALL_TIME_DATA_VALUE.getQuery());
            nativeQuery = session.createSQLQuery(MigrationQuery.DELETE_CALL_TIME_DATA_KEY.getQuery());
            nativeQuery.executeUpdate();
            session.getTransaction().commit();
            log.info("- call time - Cleaned -");
        }

        closeSQLSession(session);
    }

    @Override
    public long estimate() throws Exception {
        return 300000; // return return 5 minutes for now without any database side checks.
    }
}