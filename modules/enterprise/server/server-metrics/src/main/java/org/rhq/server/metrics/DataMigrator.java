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

/**
 * @author Stefan Negrea
 *
 */
public class DataMigrator {

    private final EntityManager entityManager;

    private Session session;

    public DataMigrator(EntityManager entityManager, Session session) {
        this.entityManager = entityManager;
        this.session = session;
    }

    public void migrateData() {

        migrateRawData();
        migrateOneHourData();
        migrateSixHourData();
        migrateTwentyFourHourData();

        clearAllData();
    }

    private void migrateRawData() {

    }

    @SuppressWarnings("unchecked")
    private void migrateOneHourData() {
        Query q = this.entityManager.createNamedQuery(MeasurementDataNumeric1H.QUERY_FIND_ALL);
        List<MeasurementDataNumeric1H> existingData = q.getResultList();

        try {
            PreparedStatement statement = createPreparedStatement(MetricsTable.ONE_HOUR);

            for (MeasurementDataNumeric1H measurement : existingData) {
                insertData(statement, measurement.getScheduleId(), measurement.getMin(), measurement.getMax(),
                    Double.parseDouble(measurement.getValue().toString()), measurement.getTimestamp());
            }
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void migrateSixHourData() {
        Query q = this.entityManager.createNamedQuery(MeasurementDataNumeric6H.QUERY_FIND_ALL);
        List<MeasurementDataNumeric6H> existingData = q.getResultList();

        try {
            PreparedStatement statement = createPreparedStatement(MetricsTable.SIX_HOUR);

            for (MeasurementDataNumeric6H measurement : existingData) {
                insertData(statement, measurement.getScheduleId(), measurement.getMin(), measurement.getMax(),
                    Double.parseDouble(measurement.getValue().toString()), measurement.getTimestamp());
            }
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
        }
    }

    private void migrateTwentyFourHourData() {
        Query q = this.entityManager.createNamedQuery(MeasurementDataNumeric1D.QUERY_FIND_ALL);
        List<MeasurementDataNumeric1D> existingData = q.getResultList();

        try {
            PreparedStatement statement = createPreparedStatement(MetricsTable.TWENTY_FOUR_HOUR);

            for (MeasurementDataNumeric1D measurement : existingData) {
                insertData(statement, measurement.getScheduleId(), measurement.getMin(), measurement.getMax(),
                    Double.parseDouble(measurement.getValue().toString()), measurement.getTimestamp());
            }
        } catch (NoHostAvailableException e) {
            throw new CQLException(e);
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

    private PreparedStatement createPreparedStatement(MetricsTable metricsTable) throws NoHostAvailableException {
        String cql = "INSERT INTO " + metricsTable + " (schedule_id, time, type, value) VALUES (?, ?, ?, ?) USING TTL "
            + metricsTable.getTTL();
        PreparedStatement statement = session.prepare(cql);
        return statement;
    }

    private void insertData(PreparedStatement statement, int scheduleId, double min, double max, double average,
        long timestamp)
        throws NoHostAvailableException {
        BoundStatement boundStatement = statement.bind(scheduleId, new Date(timestamp), AggregateType.MIN.ordinal(),
            min);
        session.execute(boundStatement);

        boundStatement = statement.bind(scheduleId, new Date(timestamp), AggregateType.MAX.ordinal(), max);
        session.execute(boundStatement);

        boundStatement = statement.bind(scheduleId, new Date(timestamp), AggregateType.AVG.ordinal(), average);
        session.execute(boundStatement);
    }
}
