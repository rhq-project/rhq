/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
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
package org.rhq.enterprise.server.measurement.instrumentation;

/**
 * @author Greg Hinkle
 */
public interface MeasurementMonitorMBean {

    long getMeasurementInsertTime();

    void incrementMeasurementInsertTime(long delta);

    long getMeasurementsInserted();

    void incrementMeasurementsInserted(long delta);

    long getCallTimeInsertTime();

    void incrementCallTimeInsertTime(long delta);

    long getCalltimeValuesInserted();

    void incrementCalltimeValuesInserted(long delta);

    int getScheduledMeasurementsPerMinute();

    long getPurgeTime();

    void incrementPurgeTime(long delta);

    long getMeasurementCompressionTime();

    long getBaselineCalculationTime();

    void incrementBaselineCalculationTime(long delta);

    long getAvailabilityInsertTime();

    void incrementAvailabilityInsertTime(long delta);

    long getAvailabilitiesInserted();

    void incrementAvailabilitiesInserted(long delta);

    long getChangesOnlyAvailabilityReports();

    long getFullAvailabilityReports();

    long getTotalAvailabilityReports();

    void incrementAvailabilityReports(boolean changesOnlyReport);

    long getPurgedAlerts();

    void setPurgedAlerts(long delta);

    long getPurgedAlertConditions();

    void setPurgedAlertConditions(long delta);

    long getPurgedAlertNotifications();

    void setPurgedAlertNotifications(long delta);

    long getPurgedEvents();

    void setPurgedEvents(long delta);

    long getPurgedAvailabilities();

    void setPurgedAvailabilities(long delta);

    long getPurgedCallTimeData();

    void setPurgedCallTimeData(long delta);

    long getPurgedMeasurementTraits();

    void setPurgedMeasurementTraits(long delta);

    int getAggregationBatchSize();

    void setAggregationBatchSize(int size);

    int getAggregationParallelism();

    void setAggregationParallelism(int parallelism);

    int getAggregationWorkers();

    void setAggregationWorkers(int numWorkers);

    int getRawDataAgeLimit();

    void setRawDataAgeLimit(int ageLimit);
}