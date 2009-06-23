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
package org.rhq.core.domain.measurement;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@NamedQueries( {
    // Unfortunately, hibernate is broken after 3.2.0.rc4 - we can't process these insert/delete HQL queries.
    // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2833
    // Instead, take these HQLs, plug into Eclipse Hibernate Tool and look at the generated native SQL.
    // Take those native queries (for each supported database) and create native queries here.
    // Can't use NamedNativeQuery either, hibernate doesn't support that.

    // update: RHQ-716 proved that this still isn't working, even with a version of Hibernate containing a fix
    // for HHH-2833.  Basically, CURRENT_TIMESTAMP returns a timestamp data type, but computeTime field expects
    // a long.  this incompatibility forces the query analysis to bomb at compile time.  this issue could have been
    // by-passed if Hibernate allows binding parameters in the select list, but that is disallowed as well.
    /*
    @NamedQuery(name = MeasurementBaseline.QUERY_CALC_FIRST_AUTOBASELINE, query = "" //
        + " INSERT INTO MeasurementBaseline (baselineMin,baselineMax,baselineMean,computeTime,scheduleId) "
        + "    SELECT min(d.min) AS baselineMin, "
        + "           max(d.max) AS baselineMax, "
        + "           avg(d.value) AS baselineMean, "
        + "           CURRENT_TIMESTAMP AS currentTime, "
        + "           d.id.scheduleId AS scheduleId "
        + "      FROM MeasurementDataNumeric1H d "
        + "           JOIN d.schedule s "
        + "           LEFT JOIN s.baseline b "
        + "     WHERE s.definition.numericType = 0 "
        + "       AND b.id IS NULL "
        + "       AND d.id.timestamp BETWEEN :startTime AND :endTime "
        + "  GROUP BY d.id.scheduleId "
        + "    HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId "
        + "                                 FROM MeasurementDataNumeric1H d1 "
        + "                                WHERE d1.id.timestamp <= :startTime) "),
    @NamedQuery(name = MeasurementBaseline.QUERY_DELETE_EXISTING_AUTOBASELINES, query = "" //
        + " DELETE MeasurementBaseline AS doomed WHERE doomed.scheduleId IN "
        + "( "
        + "   SELECT d.id.scheduleId AS scheduleId "
        + "     FROM MeasurementDataNumeric1H d "
        + "          JOIN d.schedule s "
        + "          LEFT JOIN s.baseline b "
        + "    WHERE b.id IS NOT NULL "
        + "      AND d.id.timestamp BETWEEN :startTime AND :endTime "
        + "      AND b.userEntered = FALSE "
        + " GROUP BY d.id.scheduleId "
        + "   HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId "
        + "                                FROM MeasurementDataNumeric1H d1 "
        + "                               WHERE d1.id.timestamp <= :startTime) " + ") "),
        */
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_BY_RESOURCE, query = "SELECT mb FROM MeasurementBaseline mb WHERE mb.schedule.resource.id = :resourceId"),
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_BY_RESOURCE_IDS_AND_DEF_IDS, query = "SELECT mb FROM MeasurementBaseline mb "
        + "WHERE mb.schedule.resource.id IN (:resourceIds) AND  mb.schedule.definition.id IN (:definitionIds)"),

    // this query will potentially load in alot - keep composite object small
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_BY_COMPUTE_TIME, query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite( "
        + "       mb.id, mb.baselineMin, mb.baselineMax, mb.baselineMean, sched.id "
        + "       ) "
        + "FROM MeasurementBaseline mb "
        + "     LEFT JOIN mb.schedule sched "
        + "     LEFT JOIN sched.definition def "
        + "WHERE mb.computeTime = :computeTime " + "  AND def.numericType = :numericType "),
    @NamedQuery(name = MeasurementBaseline.QUERY_DELETE_BY_COMPUTE_TIME, query = "" //
        + "DELETE MeasurementBaseline bl " //
        + " WHERE bl.computeTime < :timestamp "),
    @NamedQuery(name = MeasurementBaseline.QUERY_DELETE_BY_RESOURCES, query = "DELETE MeasurementBaseline bl WHERE bl.schedule IN ( SELECT ms FROM MeasurementSchedule ms WHERE ms.resource.id IN ( :resourceIds ) )") })
@SequenceGenerator(name = "MEAS_BL_GEN", sequenceName = "RHQ_MEASUREMENT_BLINE_ID_SEQ")
@SuppressWarnings("unused")
@Table(name = "RHQ_MEASUREMENT_BLINE")
public class MeasurementBaseline implements Serializable {
    public static final String QUERY_FIND_BY_RESOURCE = "MeasurementBaseline.findBaselinesForResource";
    public static final String QUERY_FIND_BY_RESOURCE_IDS_AND_DEF_IDS = "MeasurementBaseline.findBaselineForResourceIdsAndDefinitionIds";
    public static final String QUERY_FIND_BY_COMPUTE_TIME = "MeasurementBaseline.findByComputeTime";
    public static final String QUERY_DELETE_BY_COMPUTE_TIME = "MeasurementBaseline.deleteByComputeTime";
    public static final String QUERY_DELETE_BY_RESOURCES = "MeasurementBaseline.deleteByResources";
    public static final String QUERY_CALC_FIRST_AUTOBASELINE = "MeasurementBaseline.calcFirstAutoBaseline";
    public static final String QUERY_DELETE_EXISTING_AUTOBASELINES = "MeasurementBaseline.deleteExistingAutoBaseline";
    public static final String NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_POSTGRES;
    public static final String NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_ORACLE;
    public static final String NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_SQLSERVER;

    static {
        /*
         * we only want to compute baselines for measurements that are DYNAMIC
         */
        NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_POSTGRES = "" //
            + "    INSERT INTO RHQ_MEASUREMENT_BLINE ( id, BL_MIN, BL_MAX, BL_MEAN, BL_COMPUTE_TIME, SCHEDULE_ID ) " //
            + "         SELECT nextval('RHQ_MEASUREMENT_BLINE_ID_SEQ'), " //
            + "                MIN(data1h.minvalue) AS bline_min, " //
            + "                MAX(data1h.maxvalue) AS bline_max, " //
            + "                AVG(data1h.value) AS bline_mean, " //
            + "                ? AS bline_ts, " // ?1=computeTime
            + "                data1h.SCHEDULE_ID AS bline_sched_id " //
            + "           FROM RHQ_MEASUREMENT_DATA_NUM_1H data1h  " // baselines are 1H data statistics
            + "     INNER JOIN RHQ_MEASUREMENT_SCHED sched  " // baselines are aggregates of schedules
            + "             ON data1h.SCHEDULE_ID = sched.id  " //
            + "     INNER JOIN RHQ_MEASUREMENT_DEF def " // only compute off of dynamic types 
            + "             ON sched.definition = def.id " //
            + "LEFT OUTER JOIN RHQ_MEASUREMENT_BLINE bline " // we want null entries on purpose
            + "             ON sched.id = bline.SCHEDULE_ID  " //
            + "          WHERE ( def.numeric_type = 0 ) " // only dynamics (NumericType.DYNAMIC)
            + "            AND ( bline.id IS NULL ) " // no baseline means it was deleted or never calculated
            + "            AND ( data1h.TIME_STAMP BETWEEN ? AND ? ) " // ?2=startTime, ?3=endTime
            + "       GROUP BY data1h.SCHEDULE_ID " // baselines are aggregates per schedule
            // but only calculate baselines for schedules where we have data that fills (startTime, endTime)
            + "         HAVING data1h.SCHEDULE_ID in ( SELECT mdata.SCHEDULE_ID "
            + "                                          FROM RHQ_MEASUREMENT_DATA_NUM_1H mdata  " //
            + "                                         WHERE mdata.TIME_STAMP <= ? ) " // ?4=startTime
            + "          LIMIT 100000 "; // batch at most 100K inserts at a time to shrink the xtn size

        NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_ORACLE = "" //
            + "    INSERT INTO RHQ_MEASUREMENT_BLINE ( id, BL_MIN, BL_MAX, BL_MEAN, BL_COMPUTE_TIME, SCHEDULE_ID ) "
            + "         SELECT RHQ_MEASUREMENT_BLINE_ID_SEQ.nextval, " //
            + "                blMin, blMax, blAvg, coTime, schId "
            + "           FROM ( SELECT MIN(data1h.minvalue) AS blMin, " //
            + "                         MAX(data1h.maxvalue) AS blMax, " //
            + "                         AVG(data1h.value) AS blAvg, " //
            + "                         ? as coTime, " // ?1=computeTime
            + "                         data1h.SCHEDULE_ID as schId  " //
            + "                    FROM RHQ_MEASUREMENT_DATA_NUM_1H data1h " // baselines are 1H data statistics
            + "              INNER JOIN RHQ_MEASUREMENT_SCHED sched " // baselines are aggregates of schedules
            + "                      ON data1h.SCHEDULE_ID = sched.id " //
            + "              INNER JOIN RHQ_MEASUREMENT_DEF def " // only compute off of dynamic types
            + "                      ON sched.definition = def.id " //
            + "         LEFT OUTER JOIN RHQ_MEASUREMENT_BLINE bline " // we want null entries on purpose
            + "                      ON sched.id = bline.SCHEDULE_ID  " //
            + "                   WHERE ( def.numeric_type = 0 ) " // only dynamics (NumericType.DYNAMIC)
            + "                     AND ( bline.id IS NULL ) " // no baseline means it was deleted or never calculated
            + "                     AND ( data1h.TIME_STAMP BETWEEN ? AND ? ) " // ?2=startTime, ?3=endTime
            + "                     AND ROWNUM < 100001 " // batch at most 100K inserts at a time to shrink the xtn size
            + "                GROUP BY data1h.SCHEDULE_ID  " // baselines are aggregates per schedule
            // but only calculate baselines for schedules where we have data that fills (startTime, endTime)
            + "                  HAVING data1h.SCHEDULE_ID in ( SELECT mdata.SCHEDULE_ID " //
            + "                                                   FROM RHQ_MEASUREMENT_DATA_NUM_1H mdata "
            + "                                                  WHERE mdata.TIME_STAMP <= ? ) ) "; // ?4=startTime

        NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_SQLSERVER = "" //
            + "    INSERT INTO RHQ_MEASUREMENT_BLINE ( BL_MIN, BL_MAX, BL_MEAN, BL_COMPUTE_TIME, SCHEDULE_ID ) "
            + "         SELECT MIN(data1h.minvalue) AS blMin, " //
            + "                MAX(data1h.maxvalue) AS blMax, " //
            + "                AVG(data1h.value) AS blAvg, " //
            + "                ? as coTime, " // ?1=computeTime
            + "                data1h.SCHEDULE_ID as schId  " //
            + "           FROM RHQ_MEASUREMENT_DATA_NUM_1H data1h " // baselines are 1H data statistics
            + "     INNER JOIN RHQ_MEASUREMENT_SCHED sched " // baselines are aggregates of schedules
            + "             ON data1h.SCHEDULE_ID = sched.id " //
            + "     INNER JOIN RHQ_MEASUREMENT_DEF def " // only compute off of dynamic types
            + "             ON sched.definition = def.id " //
            + "LEFT OUTER JOIN RHQ_MEASUREMENT_BLINE bline " // we want null entries on purpose
            + "             ON sched.id = bline.SCHEDULE_ID  " //
            + "          WHERE ( def.numeric_type = 0 ) " // only dynamics (NumericType.DYNAMIC)
            + "            AND ( bline.id IS NULL ) " // no baseline means it was deleted or never calculated
            + "            AND ( data1h.TIME_STAMP BETWEEN ? AND ? ) " // ?2=startTime, ?3=endTime
            + "       GROUP BY data1h.SCHEDULE_ID  " // baselines are aggregates per schedule
            // but only calculate baselines for schedules where we have data that fills (startTime, endTime)
            + "         HAVING data1h.SCHEDULE_ID in ( SELECT mdata.SCHEDULE_ID " //
            + "                                          FROM RHQ_MEASUREMENT_DATA_NUM_1H mdata "
            + "                                         WHERE mdata.TIME_STAMP <= ? ) "; // ?4=startTime
    }
    private static final long serialVersionUID = 1L;
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "MEAS_BL_GEN")
    @Id
    private int id;

    @Column(name = "BL_USER_ENTERED", nullable = false)
    private boolean userEntered;

    @Column(name = "BL_MIN", nullable = false)
    private double baselineMin;

    @Column(name = "BL_MAX", nullable = false)
    private double baselineMax;

    @Column(name = "BL_MEAN", nullable = false)
    private double baselineMean;

    @Column(name = "BL_COMPUTE_TIME", nullable = false)
    private long computeTime;

    @JoinColumn(name = "SCHEDULE_ID", nullable = false)
    @OneToOne(fetch = FetchType.LAZY)
    private MeasurementSchedule schedule;

    // we need this to support autobaseline insertion queries
    @Column(name = "SCHEDULE_ID", nullable = false, insertable = false, updatable = false)
    private int scheduleId;

    public MeasurementBaseline() {
        computeTime = System.currentTimeMillis();
        userEntered = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MeasurementSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(MeasurementSchedule schedule) {
        this.schedule = schedule;
        this.schedule.setBaseline(this);
        this.scheduleId = schedule.getId();
    }

    /**
     * The time when the baseline was inserted into the database (which is usually when the baseline was calculated).
     *
     * @return the time when the baseline was persisted
     */
    public Date getComputeTime() {
        return new Date(computeTime);
    }

    /**
     * Sets the compute time to the current time. This approximation is good enough since it uses JPA annotations for
     * update/persist, we don't need to update the field ourselves.
     */
    @PrePersist
    @PreUpdate
    private void setComputeTime() {
        computeTime = System.currentTimeMillis();
    }

    /**
     * The highest value the measurement ever reached during the baseline time period.
     *
     * @return maximum value of the measurement
     */
    public Double getMax() {
        return baselineMax;
    }

    public void setMax(Double max) {
        this.baselineMax = max;
    }

    /**
     * The average measurement value as computed over the baseline time period.
     *
     * @return the average measurement value
     */
    public Double getMean() {
        return baselineMean;
    }

    public void setMean(Double mean) {
        this.baselineMean = mean;
    }

    /**
     * The lowest value the measurement ever reached during the baseline time period.
     *
     * @return minimum value of the measurement
     */
    public Double getMin() {
        return baselineMin;
    }

    public void setMin(Double min) {
        this.baselineMin = min;
    }

    /**
     * If <code>true</code>, it means a user manually entered the baseline values, as opposed to having them
     * automatically be calculated by examining past measurement data.
     *
     * @return indicates if the user entered the baselines or if the values were automatically calculated
     */
    public boolean isUserEntered() {
        return userEntered;
    }

    public void setUserEntered(boolean userEntered) {
        this.userEntered = userEntered;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MeasurementBaseline");
        sb.append("{id=").append(id);
        sb.append(", userEntered=").append(userEntered);
        sb.append(", baselineMin=").append(baselineMin);
        sb.append(", baselineMax=").append(baselineMax);
        sb.append(", baselineMean=").append(baselineMean);
        sb.append(", computeTime=").append(computeTime);
        sb.append(", scheduleId=").append(scheduleId);
        sb.append('}');
        return sb.toString();
    }
}