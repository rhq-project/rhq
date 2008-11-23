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

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class MeasurementMonitor implements MeasurementMonitorMBean, MBeanRegistration {
    private AtomicLong measurementInsertTime = new AtomicLong();

    private AtomicLong measurementsInserted = new AtomicLong();

    private AtomicLong availabilityInsertTime = new AtomicLong();

    private AtomicLong availabilitiesInserted = new AtomicLong();

    private AtomicLong compressionTime = new AtomicLong();

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

    public int getScheduledMeasurementsPerMinute() {
        return LookupUtil.getMeasurementScheduleManager().getScheduledMeasurementsPerMinute();
    }

    public long getMeasurementCompressionTime() {
        return compressionTime.get();
    }

    public void incrementMeasurementCompressionTime(long delta) {
        this.compressionTime.addAndGet(delta);
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

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        objectName = name;
        mbeanServer = server;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
        mbeanServer = null;
    }
}