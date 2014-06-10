/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.measurement.instrumentation;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.rhq.core.util.ObjectNameFactory;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.util.JMXUtil;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class MeasurementMonitor implements MeasurementMonitorMBean {
    private static final ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=MeasurementMonitor");

    @EJB
    private StorageClientManager storageClientManager;

    private AtomicLong measurementInsertTime = new AtomicLong();

    private AtomicLong measurementsInserted = new AtomicLong();

    private AtomicLong callTimeInsertTime = new AtomicLong();

    private AtomicLong calltimeValuesInserted = new AtomicLong();

    private AtomicLong availabilityInsertTime = new AtomicLong();

    private AtomicLong availabilitiesInserted = new AtomicLong();

    private AtomicLong changesOnlyAvailabilityReports = new AtomicLong();

    private AtomicLong fullAvailabilityReports = new AtomicLong();

    private AtomicLong purgeTime = new AtomicLong();

    private AtomicLong baselineCalculationTime = new AtomicLong();

    // all of the purgedXYZ attributes will contain the number of purged items during the LAST purge
    // they are not an aggregation over multiple purges - it only tracks the LAST purge that was run

    private AtomicLong purgedAlerts = new AtomicLong();

    private AtomicLong purgedAlertConditions = new AtomicLong();

    private AtomicLong purgedAlertNotifications = new AtomicLong();

    private AtomicLong purgedAvailabilities = new AtomicLong();

    private AtomicLong purgedCallTimeData = new AtomicLong();

    private AtomicLong purgedEvents = new AtomicLong();

    private AtomicLong purgedMeasurementTraits = new AtomicLong();

    private static MBeanServer mbeanServer;
    private static ObjectName objectName;

    private static MeasurementMonitorMBean proxy;

    public static MeasurementMonitorMBean getMBean() {
        if (proxy == null) {
            if (objectName != null) {
                proxy = (MeasurementMonitorMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
                    objectName, MeasurementMonitorMBean.class, false);
            } else {
                // create a local object
                proxy = new MeasurementMonitor();
            }
        }

        return proxy;
    }

    public long getMeasurementInsertTime() {
        return measurementInsertTime.get();
    }

    public void incrementMeasurementInsertTime(long delta) {
        this.measurementInsertTime.addAndGet(delta);
    }

    public long getMeasurementsInserted() {
        return measurementsInserted.get();
    }

    public void incrementMeasurementsInserted(long delta) {
        this.measurementsInserted.addAndGet(delta);
    }

    public long getCalltimeValuesInserted() {
        return calltimeValuesInserted.get();
    }

    public void incrementCalltimeValuesInserted(long delta) {
        this.calltimeValuesInserted.addAndGet(delta);
    }

    public long getCallTimeInsertTime() {
        return callTimeInsertTime.get();
    }

    public void incrementCallTimeInsertTime(long delta) {
        this.callTimeInsertTime.addAndGet(delta);
    }

    public long getAvailabilityInsertTime() {
        return availabilityInsertTime.get();
    }

    public void incrementAvailabilityInsertTime(long delta) {
        this.availabilityInsertTime.addAndGet(delta);
    }

    public long getAvailabilitiesInserted() {
        return availabilitiesInserted.get();
    }

    public void incrementAvailabilitiesInserted(long delta) {
        this.availabilitiesInserted.addAndGet(delta);
    }

    public long getChangesOnlyAvailabilityReports() {
        return changesOnlyAvailabilityReports.get();
    }

    public long getFullAvailabilityReports() {
        return fullAvailabilityReports.get();
    }

    public long getTotalAvailabilityReports() {
        return getChangesOnlyAvailabilityReports() + getFullAvailabilityReports();
    }

    public void incrementAvailabilityReports(boolean changesOnlyReport) {
        if (changesOnlyReport) {
            this.changesOnlyAvailabilityReports.incrementAndGet();
        } else {
            this.fullAvailabilityReports.incrementAndGet();
        }
    }

    public int getScheduledMeasurementsPerMinute() {
        return LookupUtil.getMeasurementScheduleManager().getScheduledMeasurementsPerMinute();
    }

    public long getMeasurementCompressionTime() {
        return storageClientManager.getMetricsServer().getTotalAggregationTime();
    }

    public long getPurgeTime() {
        return purgeTime.get();
    }

    public void incrementPurgeTime(long delta) {
        this.purgeTime.addAndGet(delta);
    }

    public long getBaselineCalculationTime() {
        return this.baselineCalculationTime.get();
    }

    public void incrementBaselineCalculationTime(long delta) {
        this.baselineCalculationTime.addAndGet(delta);
    }

    public long getPurgedAlerts() {
        return this.purgedAlerts.get();
    }

    public void setPurgedAlerts(long delta) {
        this.purgedAlerts.set(delta);
    }

    public long getPurgedAlertConditions() {
        return this.purgedAlertConditions.get();
    }

    public void setPurgedAlertConditions(long delta) {
        this.purgedAlertConditions.set(delta);
    }

    public long getPurgedAlertNotifications() {
        return this.purgedAlertNotifications.get();
    }

    public void setPurgedAlertNotifications(long delta) {
        this.purgedAlertNotifications.set(delta);
    }

    public long getPurgedAvailabilities() {
        return this.purgedAvailabilities.get();
    }

    public void setPurgedAvailabilities(long delta) {
        this.purgedAvailabilities.set(delta);
    }

    public long getPurgedCallTimeData() {
        return this.purgedCallTimeData.get();
    }

    public void setPurgedCallTimeData(long delta) {
        this.purgedCallTimeData.set(delta);
    }

    public long getPurgedEvents() {
        return this.purgedEvents.get();
    }

    public void setPurgedEvents(long delta) {
        this.purgedEvents.set(delta);
    }

    public long getPurgedMeasurementTraits() {
        return this.purgedMeasurementTraits.get();
    }

    public void setPurgedMeasurementTraits(long delta) {
        this.purgedMeasurementTraits.set(delta);
    }

    @Override
    public int getAggregationBatchSize() {
        return storageClientManager.getMetricsServer().getAggregationBatchSize();
    }

    @Override
    public void setAggregationBatchSize(int size) {
        storageClientManager.setAggregationBatchSize(size);
    }

    @Override
    public int getAggregationParallelism() {
        return storageClientManager.getAggregationParallelism();
    }

    @Override
    public void setAggregationParallelism(int parallelism) {
        storageClientManager.setAggregationParallelism(parallelism);
    }

    @Override
    public int getAggregationWorkers() {
        return storageClientManager.getAggregationWorkers();
    }

    @Override
    public void setAggregationWorkers(int numWorkers) {
        storageClientManager.setAggregationWorkers(numWorkers);
    }

    @Override
    public int getRawDataAgeLimit() {
        return storageClientManager.getRawDataAgeLimit();
    }

    @Override
    public void setRawDataAgeLimit(int ageLimit) {
        storageClientManager.setRawDataAgeLimit(ageLimit);
    }

    @PostConstruct
    private void init() {
        JMXUtil.registerMBean(this, OBJECT_NAME);
        mbeanServer = JMXUtil.getPlatformMBeanServer();
        objectName = OBJECT_NAME;
    }

    @PreDestroy
    private void destroy() {
        mbeanServer = null;
        objectName = null;
        JMXUtil.unregisterMBeanQuietly(OBJECT_NAME);
    }
}
