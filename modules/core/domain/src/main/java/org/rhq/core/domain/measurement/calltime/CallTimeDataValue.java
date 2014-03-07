/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.measurement.calltime;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.Nullable;

/**
 * Measurement data for a set of calls to a particular destination over a certain time span.
 *
 * @author Ian Springer
 */
@Entity
@NamedQueries( {
    // NOTE: This query only includes data chunks that are fully within the specified time interval, because it would
    //       not be possible to extrapolate the stats only for the overlapping portion of partially overlapping chunks.
    @NamedQuery(name = CallTimeDataValue.QUERY_FIND_COMPOSITES_FOR_RESOURCE, query = "SELECT new org.rhq.core.domain.measurement.calltime.CallTimeDataComposite("
        + "key.callDestination, "
        + "MIN(value.minimum), "
        + "MAX(value.maximum), "
        + "SUM(value.total), "
        + "SUM(value.count), "
        + "SUM(value.total) / SUM(value.count)) "
        + "FROM CallTimeDataValue value "
        + "JOIN value.key key "
        + "WHERE key.schedule.id = :scheduleId "
        + "AND value.count != 0 "
        + "AND value.minimum != -1 "
        + "AND value.beginTime >= :beginTime "
        + "AND value.endTime <= :endTime "
        + "GROUP BY key.callDestination "),
    @NamedQuery(name = CallTimeDataValue.QUERY_FIND_RAW_FOR_RESOURCE, query = "SELECT new org.rhq.core.domain.measurement.calltime.CallTimeDataComposite("
        + "key.callDestination, "
        + "value.minimum, "
        + "value.maximum, "
        + "value.total, "
        + "value.count, "
        + "value.total / value.count) "
        + "FROM CallTimeDataValue value "
        + "WHERE key.schedule.id = :scheduleId "
        + "AND value.count != 0 "
        + "AND value.minimum != -1 "
        + "AND value.beginTime >= :beginTime "
        + "AND value.endTime <= :endTime "),
    @NamedQuery(name = CallTimeDataValue.QUERY_DELETE_BY_RESOURCES, query = "DELETE CallTimeDataValue ctdv WHERE ctdv.key IN ( SELECT ctdk.id FROM CallTimeDataKey ctdk WHERE ctdk.schedule.resource.id IN ( :resourceIds ) )") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_CALLTIME_DATA_VALUE_ID_SEQ", sequenceName = "RHQ_CALLTIME_DATA_VALUE_ID_SEQ")
@Table(name = "RHQ_CALLTIME_DATA_VALUE")
public class CallTimeDataValue implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_COMPOSITES_FOR_RESOURCE = "CallTimeDataValue.findCompositesForResource";
    public static final String QUERY_DELETE_BY_RESOURCES = "CallTimeDataValue.deleteByResources";
    public static final String QUERY_FIND_RAW_FOR_RESOURCE = "CallTimeDataValue.findRawForResource";

    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_CALLTIME_DATA_VALUE_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "KEY_ID", nullable = false)
    @ManyToOne
    private CallTimeDataKey key;

    @Column(name = "BEGIN_TIME", nullable = false)
    private long beginTime;

    @Column(name = "END_TIME", nullable = false)
    private long endTime;

    @Column(name = "MINIMUM", nullable = false)
    private double minimum = Double.NaN;

    @Column(name = "MAXIMUM", nullable = false)
    private double maximum;

    @Column(name = "TOTAL", nullable = false)
    private double total;

    @Column(name = "COUNT", nullable = false)
    private long count;

    /**
     * Create a new <code>CallTimeDataValue</code>.
     *
     * @param beginTime the begin time of the time range for which the call-time data was collected
     * @param endTime   the end time of the time range for which the call-time data was collected
     */
    public CallTimeDataValue(Date beginTime, Date endTime) {
        this.beginTime = beginTime.getTime();
        this.endTime = endTime.getTime();
    }

    public CallTimeDataValue() {
        /* for JPA and deserialization use only */
    }

    public int getId() {
        return id;
    }

    @Nullable
    public CallTimeDataKey getKey() {
        return key;
    }

    public void setKey(@Nullable CallTimeDataKey key) {
        this.key = key;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void mergeCallTime(double callTime) {
        if (callTime < 0) {
            throw new IllegalArgumentException("Call time is a duration and so must be >= 0.");
        }

        this.count++;
        this.total += callTime;
        if ((callTime < this.minimum) || Double.isNaN(this.minimum)) {
            this.minimum = callTime;
        }

        if (callTime > this.maximum) {
            this.maximum = callTime;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".") + 1) + "[" + "key="
            + this.key + ", " + "beginTime=" + new Date(this.beginTime) + ", " + "endTime=" + new Date(this.endTime)
            + ", " + "minimum=" + this.minimum + ", " + "maximum=" + this.maximum + ", " + "total=" + this.total + ", "
            + "count=" + this.count + "]";
    }
}