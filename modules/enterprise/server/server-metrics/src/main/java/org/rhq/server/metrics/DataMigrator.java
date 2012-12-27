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

package org.rhq.server.metrics;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.rhq.core.domain.measurement.MeasurementDataNumeric1D;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementDataNumeric6H;
import org.rhq.core.domain.measurement.MeasurementDataNumericAggregateInterface;

/**
 * @author Stefan Negrea
 *
 */
public class DataMigrator {

    private static final int MAX_RECORDS_TO_MIGRATE = 1000;

    private final EntityManager entityManager;
    private final Session session;

    private boolean deleteDataImmediatelyAfterMigration;
    private boolean deleteAllDataAtTheEndOfMigration;
    private boolean runRawDataMigration;
    private boolean run1HAggregateDataMigration;
    private boolean run6HAggregateDataMigration;
    private boolean run1DAggregateDataMigration;

    public DataMigrator(EntityManager entityManager, Session session) {
        this.entityManager = entityManager;
        this.session = session;

        this.deleteDataImmediatelyAfterMigration = true;
        this.deleteAllDataAtTheEndOfMigration = false;
        this.runRawDataMigration = true;
        this.run1HAggregateDataMigration = true;
        this.run6HAggregateDataMigration = true;
        this.run1DAggregateDataMigration = true;
    }

    public void run1HAggregateDataMigration(boolean value) {
        this.run1HAggregateDataMigration = value;
    }

    public void run6HAggregateDataMigration(boolean value) {
        this.run6HAggregateDataMigration = value;
    }

    public void run1DAggregateDataMigration(boolean value) {
        this.run1DAggregateDataMigration = value;
    }

    public void deleteDataImmediatelyAfterMigration(boolean value) {
        this.deleteDataImmediatelyAfterMigration = value;
        this.deleteAllDataAtTheEndOfMigration = !value;
    }

    public void deleteAllDataAtTheEndOfMigration(boolean value) {
        this.deleteAllDataAtTheEndOfMigration = value;
        this.deleteDataImmediatelyAfterMigration = !value;
    }

    public void migrateData() {
        if (runRawDataMigration) {
            migrateRawData();
        }

        if (run1HAggregateDataMigration) {
            migrateAggregatedMetricsData(MeasurementDataNumeric1H.QUERY_FIND_ALL, MetricsTable.ONE_HOUR);
        }

        if (run6HAggregateDataMigration) {
            migrateAggregatedMetricsData(MeasurementDataNumeric6H.QUERY_FIND_ALL, MetricsTable.SIX_HOUR);
        }

        if (run1DAggregateDataMigration) {
            migrateAggregatedMetricsData(MeasurementDataNumeric1D.QUERY_FIND_ALL, MetricsTable.TWENTY_FOUR_HOUR);
        }

        if (deleteAllDataAtTheEndOfMigration) {
            this.clearAllData();
        }
    }

    private void migrateRawData() {
        //possibly need to add raw SQL code here because data is split among several tables
    }

    @SuppressWarnings("unchecked")
    private void migrateAggregatedMetricsData(String query, MetricsTable metricsTable) {
        List<MeasurementDataNumericAggregateInterface> existingData = null;

        while (true) {
            Query q = this.entityManager.createNamedQuery(query);
            q.setMaxResults(MAX_RECORDS_TO_MIGRATE);
            existingData = (List<MeasurementDataNumericAggregateInterface>) q.getResultList();

            if (existingData.size() == 0) {
                break;
            }

            try {
                String cql = "INSERT INTO " + metricsTable
                    + " (schedule_id, time, type, value) VALUES (?, ?, ?, ?) USING TTL " + metricsTable.getTTL();
                PreparedStatement statement = session.prepare(cql);

                for (MeasurementDataNumericAggregateInterface measurement : existingData) {

                    BoundStatement boundStatement = statement.bind(measurement.getScheduleId(),
                        new Date(measurement.getTimestamp()), AggregateType.MIN.ordinal(), measurement.getMin());
                    session.execute(boundStatement);

                    boundStatement = statement.bind(measurement.getScheduleId(), new Date(measurement.getTimestamp()),
                        AggregateType.MAX.ordinal(),  measurement.getMax());
                    session.execute(boundStatement);

                    boundStatement = statement.bind(measurement.getScheduleId(), new Date(measurement.getTimestamp()),
                        AggregateType.AVG.ordinal(), Double.parseDouble(measurement.getValue().toString()));
                    session.execute(boundStatement);
                }
            } catch (NoHostAvailableException e) {
                throw new CQLException(e);
            }

            if (this.deleteDataImmediatelyAfterMigration) {
                for (Object entity : existingData) {
                    this.entityManager.remove(entity);
                }
                this.entityManager.flush();
            }
        }
    }

    private void clearAllData() {
        Query q = this.entityManager.createNamedQuery(MeasurementDataNumeric1H.QUERY_DELETE_ALL);
        q.executeUpdate();

        q = this.entityManager.createNamedQuery(MeasurementDataNumeric6H.QUERY_DELETE_ALL);
        q.executeUpdate();

        q = this.entityManager.createNamedQuery(MeasurementDataNumeric1D.QUERY_DELETE_ALL);
        q.executeUpdate();
    }
}
