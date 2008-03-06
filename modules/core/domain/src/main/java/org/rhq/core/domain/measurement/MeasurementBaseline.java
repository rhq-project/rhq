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
    //   @NamedQuery(name=MeasurementBaseline.QUERY_CALC_FIRST_AUTOBASELINE,
    //      query= "INSERT INTO MeasurementBaseline (baselineMin,baselineMax,baselineMean,computeTime,scheduleId) "
    //           + "    SELECT min(d.min) AS baselineMin, "
    //           + "           max(d.max) AS baselineMax, "
    //           + "           avg(d.value) AS baselineMean, "
    //           + "           CURRENT_TIMESTAMP AS computeTime, "
    //           + "           d.id.scheduleId AS scheduleId "
    //           + "      FROM MeasurementDataNumeric1H d "
    //           + "           JOIN d.schedule s "
    //           + "           LEFT JOIN s.baseline b "
    //           + "     WHERE s.definition.numericType = 0 "
    //           + "       AND b.id IS NULL "
    //           + "       AND d.id.timestamp BETWEEN :startTime AND :endTime "
    //           + "  GROUP BY d.id.scheduleId "
    //           + "    HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId "
    //           + "                                 FROM MeasurementDataNumeric1H d1 "
    //           + "                                WHERE d1.id.timestamp <= :startTime) "),
    //   @NamedQuery(name=MeasurementBaseline.QUERY_DELETE_EXISTING_AUTOBASELINES,
    //       query="DELETE MeasurementBaseline AS doomed WHERE doomed.scheduleId IN "
    //           + "( "
    //           + "   SELECT d.id.scheduleId AS scheduleId "
    //           + "     FROM MeasurementDataNumeric1H d "
    //           + "          JOIN d.schedule s "
    //           + "          LEFT JOIN s.baseline b "
    //           + "    WHERE b.id IS NOT NULL "
    //           + "      AND d.id.timestamp BETWEEN :startTime AND :endTime "
    //           + "      AND b.userEntered = FALSE "
    //           + " GROUP BY d.id.scheduleId "
    //           + "   HAVING d.id.scheduleId IN (SELECT d1.id.scheduleId "
    //           + "                                FROM MeasurementDataNumeric1H d1 "
    //           + "                               WHERE d1.id.timestamp <= :startTime) "
    //           + ") "),
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_BY_RESOURCE, query = "SELECT mb FROM MeasurementBaseline mb WHERE mb.schedule.resource.id = :resourceId"),
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_BY_RESOURCE_IDS_AND_DEF_IDS, query = "SELECT mb FROM MeasurementBaseline mb "
        + "WHERE mb.schedule.resource.id IN (:resourceIds) AND  mb.schedule.definition.id IN (:definitionIds)"),

    // this query will potentially load in alot - keep composite object small (all baselines essentially)
    // this is needed for the alert cache - which itself will require alot of memory for large inventories
    // with large amounts of measurements getting collected
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_ALL_DYNAMIC_MEASUREMENT_BASELINES, query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite( "
        + "       mb.id, mb.baselineMin, mb.baselineMax, mb.baselineMean, sched.id "
        + "       ) "
        + "FROM MeasurementBaseline mb "
        + "     LEFT JOIN mb.schedule sched "
        + "     LEFT JOIN sched.definition def "
        + "WHERE def.numericType = :numericType "),

    // this query will potentially load in alot - keep composite object small
    @NamedQuery(name = MeasurementBaseline.QUERY_FIND_BY_COMPUTE_TIME, query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementBaselineComposite( "
        + "       mb.id, mb.baselineMin, mb.baselineMax, mb.baselineMean, sched.id "
        + "       ) "
        + "FROM MeasurementBaseline mb "
        + "     LEFT JOIN mb.schedule sched "
        + "     LEFT JOIN sched.definition def "
        + "WHERE mb.computeTime = :computeTime " + "  AND def.numericType = :numericType ") })
@SequenceGenerator(name = "MEAS_BL_GEN", sequenceName = "RHQ_MEASUREMENT_BLINE_ID_SEQ")
@SuppressWarnings("unused")
@Table(name = "RHQ_MEASUREMENT_BLINE")
public class MeasurementBaseline implements Serializable {
    public static final String QUERY_FIND_ALL_DYNAMIC_MEASUREMENT_BASELINES = "MeasurementBaseline.findAllDynamicMeasurementBaselines";
    public static final String QUERY_FIND_BY_RESOURCE = "MeasurementBaseline.findBaselinesForResource";
    public static final String QUERY_FIND_BY_RESOURCE_IDS_AND_DEF_IDS = "MeasurementBaseline.findBaselineForResourceIdsAndDefinitionIds";
    public static final String QUERY_FIND_BY_COMPUTE_TIME = "MeasurementBaseline.findByComputeTime";
    public static final String QUERY_DELETE_BY_RESOURCE_ID = "DELETE FROM rhq_measurement_bline bl WHERE bl.schedule_id IN ( SELECT ms.id FROM rhq_measurement_sched ms WHERE ms.resource_id = :resourceId )";
    public static final String NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_POSTGRES;
    public static final String NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_ORACLE;
    public static final String NATIVE_QUERY_DELETE_EXISTING_AUTOBASELINES_POSTGRES;
    public static final String NATIVE_QUERY_DELETE_EXISTING_AUTOBASELINES_ORACLE;

    static {
        /*
         * we only want to compute baselines for measurements that are DYNAMIC
         */
        NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_POSTGRES = "insert into "
            + " RHQ_MEASUREMENT_BLINE "
            + " ( id, BL_MIN, BL_MAX, BL_MEAN, BL_COMPUTE_TIME, SCHEDULE_ID ) select "
            + "  nextval ('RHQ_MEASUREMENT_BLINE_ID_SEQ'), "
            + "  min(measuremen0_.minvalue) as col_0_0_, "
            + "  max(measuremen0_.maxvalue) as col_1_0_, "
            + "  avg(measuremen0_.value) as col_2_0_, "
            + "  ? as col_3_0_, " // ?1=computeTime
            + "  measuremen0_.SCHEDULE_ID as col_4_0_  " + " from " + "  RHQ_MEASUREMENT_DATA_NUM_1H measuremen0_  "
            + " inner join " + "  RHQ_MEASUREMENT_SCHED measuremen1_  "
            + "   on measuremen0_.SCHEDULE_ID=measuremen1_.id  " + " inner join "
            + "  RHQ_MEASUREMENT_DEF measurement_def" + "   on measuremen1_.definition=measurement_def.id "
            + " left outer join "
            + "  RHQ_MEASUREMENT_BLINE measuremen2_  "
            + "   on measuremen1_.id=measuremen2_.SCHEDULE_ID  "
            + " where "
            + "  ( "
            + "   measurement_def.numeric_type = 0 " // NumericType.DYNAMIC is first enum value -> 0
            + "  ) " + "  and ( " + "   measuremen2_.id is null "
            + "  )  "
            + "  and ( "
            + "   measuremen0_.TIME_STAMP between ? and ? " // ?2=startTime, ?3=endTime
            + "  )  " + " group by " + "  measuremen0_.SCHEDULE_ID  " + " having " + "  measuremen0_.SCHEDULE_ID in ( "
            + "   select " + "    measuremen3_.SCHEDULE_ID  " + "   from "
            + "    RHQ_MEASUREMENT_DATA_NUM_1H measuremen3_  " + "   where " + "    measuremen3_.TIME_STAMP<=? " // ?4=startTime
            + "  ) ";

        NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_ORACLE = "INSERT INTO RHQ_MEASUREMENT_BLINE "
            + " ( id, BL_MIN, BL_MAX, BL_MEAN, BL_COMPUTE_TIME, SCHEDULE_ID ) "
            + " SELECT RHQ_MEASUREMENT_BLINE_ID_SEQ.nextval, blMin, blMax, blAvg , coTime, schId  "
            + " FROM ( "
            + "  SELECT  "
            + "    min(data1h.minvalue) as blMin,   max(data1h.maxvalue) as blMax, "
            + "    avg(data1h.value) as blAvg,   ? as coTime, " // ?1=computeTime
            + "    data1h.SCHEDULE_ID as schId  " + "  FROM RHQ_MEASUREMENT_DATA_NUM_1H data1h  " + "  inner join "
            + "    RHQ_MEASUREMENT_SCHED measuremen1_   on data1h.SCHEDULE_ID=measuremen1_.id  "
            + "  inner join "
            + "    RHQ_MEASUREMENT_DEF measurement_def  on measuremen1_.definition=measurement_def.id "
            + "  left outer join "
            + "    RHQ_MEASUREMENT_BLINE measuremen2_   on measuremen1_.id=measuremen2_.SCHEDULE_ID  "
            + "  WHERE "
            + "   (  measurement_def.numeric_type = 0 ) " // NumericType.DYNAMIC is first enum value -> 0
            + "    AND (   measuremen2_.id is null  )  "
            + "    AND (   data1h.TIME_STAMP between ? and ? ) " // ?2=startTime, ?3=endTime
            + "  GROUP BY data1h.SCHEDULE_ID  " + "  HAVING  data1h.SCHEDULE_ID in ( " + "   SELECT "
            + "    measuremen3_.SCHEDULE_ID  " + "   FROM " + "    RHQ_MEASUREMENT_DATA_NUM_1H measuremen3_  "
            + "   WHERE " + "    measuremen3_.TIME_STAMP<=? " // ?4=startTime
            + "  ) " + " ) ";

        NATIVE_QUERY_DELETE_EXISTING_AUTOBASELINES_POSTGRES = "DELETE FROM RHQ_MEASUREMENT_BLINE "
            + " WHERE "
            + "  SCHEDULE_ID in "
            + "  ( "
            + "   select measuremen1_.SCHEDULE_ID "
            + "     from RHQ_MEASUREMENT_DATA_NUM_1H measuremen1_ "
            + "          inner join RHQ_MEASUREMENT_SCHED measuremen2_ "
            + "                     on measuremen1_.SCHEDULE_ID=measuremen2_.id "
            + "          left outer join RHQ_MEASUREMENT_BLINE measuremen3_ "
            + "                          on measuremen2_.id=measuremen3_.SCHEDULE_ID "
            + "    where ( measuremen3_.id is not null ) "
            + "      and ( measuremen1_.TIME_STAMP between ? and ? ) " // ?1=startTime, ?2=endTime
            + "      and measuremen3_.BL_USER_ENTERED=false " + "    group by measuremen1_.SCHEDULE_ID "
            + "    having measuremen1_.SCHEDULE_ID in " + "           ( "
            + "            select measuremen4_.SCHEDULE_ID "
            + "              from RHQ_MEASUREMENT_DATA_NUM_1H measuremen4_ "
            + "             where measuremen4_.TIME_STAMP<=? " // ?3=startTime
            + "           ) " + "   ) ";

        NATIVE_QUERY_DELETE_EXISTING_AUTOBASELINES_ORACLE = "DELETE FROM RHQ_MEASUREMENT_BLINE measuremen0_ "
            + " WHERE " + "  measuremen0_.SCHEDULE_ID in " + "  ( "
            + "   select measuremen1_.SCHEDULE_ID "
            + "     from RHQ_MEASUREMENT_DATA_NUM_1H measuremen1_, "
            + "          RHQ_MEASUREMENT_SCHED measuremen2_, "
            + "          RHQ_MEASUREMENT_BLINE measuremen3_ "
            + "    where measuremen1_.SCHEDULE_ID=measuremen2_.id "
            + "      and measuremen2_.id=measuremen3_.SCHEDULE_ID(+) "
            + "      and ( measuremen3_.id is not null ) "
            + "      and ( measuremen1_.TIME_STAMP between ? and ? ) " // ?1=startTime, ?2=endTime
            + "      and measuremen3_.BL_USER_ENTERED=0 " + "    group by measuremen1_.SCHEDULE_ID "
            + "    having measuremen1_.SCHEDULE_ID in " + "           ( "
            + "            select measuremen4_.SCHEDULE_ID "
            + "              from RHQ_MEASUREMENT_DATA_NUM_1H measuremen4_ "
            + "             where measuremen4_.TIME_STAMP<=? " // ?3=startTime
            + "           ) " + "  ) ";
    }

    private static final long serialVersionUID = 1L;
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MEAS_BL_GEN")
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
        this.scheduleId = (schedule == null) ? 0 : schedule.getId();
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
}