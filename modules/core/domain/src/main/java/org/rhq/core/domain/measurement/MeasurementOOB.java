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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Store for OOB related data
 *
 * @author Heiko W. Rupp
 */
@NamedQueries( {
    @NamedQuery(name = MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE, query = "" //
        + "   SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite" //
        + "        ( res.name, res.id, res.ancestry, res.resourceType.id, def.displayName, sched.id, o.timestamp, def.id, o.oobFactor, " //
        + "          bal.baselineMin, bal.baselineMax, def.units, parent.name, parent.id) " //
        + "     FROM MeasurementOOB o, MeasurementSchedule sched " //
        + "LEFT JOIN sched.definition def " //
        + "LEFT JOIN sched.resource res " //
        + "LEFT JOIN sched.baseline bal " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE o.id = sched.id " //
        + "      AND res.inventoryStatus = 'COMMITTED' " //
        + "      AND sched.definition = def " //
        + "      AND sched.resource = res " //
        + "      AND bal.schedule = sched " //
        + "      AND res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " //
        + "                       WHERE s.id = :subjectId ) " //
        + "      AND (UPPER(def.displayName ) LIKE :metricName ESCAPE :escapeChar OR :metricName is null ) " //
        + "      AND (UPPER(res.name) LIKE :resourceName ESCAPE :escapeChar OR :resourceName is null ) " //
        + "      AND (UPPER(parent.name) LIKE :parentName ESCAPE :escapeChar OR :parentName is null ) "), //
    @NamedQuery(name = MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite" //
        + "        ( res.name, res.id, res.ancestry, res.resourceType.id, def.displayName, sched.id, o.timestamp, def.id, o.oobFactor, " //
        + "          bal.baselineMin, bal.baselineMax, def.units, parent.name, parent.id) " //
        + "     FROM MeasurementOOB o, MeasurementSchedule sched " //
        + "LEFT JOIN sched.definition def " //
        + "LEFT JOIN sched.resource res " //
        + "LEFT JOIN sched.baseline bal " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE o.id = sched.id " //
        + "      AND res.inventoryStatus = 'COMMITTED' " //
        + "      AND sched.definition = def " //
        + "      AND sched.resource = res " //
        + "      AND bal.schedule = sched " //
        + "      AND (UPPER(def.displayName ) LIKE :metricName ESCAPE :escapeChar OR :metricName is null ) " //
        + "      AND (UPPER(res.name) LIKE :resourceName ESCAPE :escapeChar OR :resourceName is null ) " //
        + "      AND (UPPER(parent.name) LIKE :parentName ESCAPE :escapeChar OR :parentName is null ) "), //
    @NamedQuery(name = MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT, query = "" //
        + "   SELECT COUNT(sched.id) " //
        + "     FROM MeasurementOOB o, MeasurementSchedule sched " //
        + "LEFT JOIN sched.definition def " //
        + "LEFT JOIN sched.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE o.id = sched.id " //
        + "      AND res.inventoryStatus = 'COMMITTED' " //
        + "      AND sched.definition = def " //
        + "      AND sched.resource = res " //
        + "      AND (UPPER(def.displayName ) LIKE :metricName OR :metricName is null ) " //
        + "      AND (UPPER(res.name) LIKE :resourceName OR :resourceName is null ) " //
        + "      AND (UPPER(parent.name) LIKE :parentName OR :parentName is null ) "), //
    @NamedQuery(name = MeasurementOOB.DELETE_OUTDATED, query = "" //
        + "DELETE FROM MeasurementOOB o " //
        + "      WHERE o.id IN ( SELECT b.schedule.id " //
        + "                        FROM MeasurementBaseline b " //
        + "                       WHERE b.computeTime > :cutOff )"), //
    @NamedQuery(name = MeasurementOOB.COUNT_FOR_DATE, query = "" //
        + "SELECT COUNT(o.timestamp) " //
        + "  FROM MeasurementOOB o " //
        + " WHERE o.timestamp = :timestamp"), //
    @NamedQuery(name = MeasurementOOB.DELETE_FOR_SCHEDULE, query = "" //
        + "DELETE FROM MeasurementOOB o " //
        + "      WHERE o.id = :id"), //
    @NamedQuery(name = MeasurementOOB.DELETE_FOR_GROUP_AND_DEFINITION, query = "" //
        + "DELETE FROM MeasurementOOB o " //
        + "      WHERE o.id IN ( SELECT ms.id " //
        + "                        FROM MeasurementSchedule ms " //
        + "                        JOIN ms.resource res " //
        + "                        JOIN res.implicitGroups rg " //
        + "                       WHERE rg.id = :groupId " //
        + "                         AND ms.definition.id = :definitionId )"), //
    @NamedQuery(name = MeasurementOOB.DELETE_FOR_RESOURCES, query = "" //
        + "DELETE FROM MeasurementOOB o " //
        + "      WHERE o.id IN ( SELECT ms.id " //
        + "                        FROM MeasurementSchedule ms " //
        + "                       WHERE ms.resource.id IN ( :resourceIds ) )"), //
    @NamedQuery(name = MeasurementOOB.GET_HIGHEST_FACTORS_FOR_RESOURCE, query = "" //
        + "   SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite" //
        + "        ( res.name, res.id, res.ancestry, res.resourceType.id, def.displayName, sched.id, o.timestamp, def.id, o.oobFactor, " //
        + "          bal.baselineMin , bal.baselineMax, def.units ) " //
        + "     FROM MeasurementOOB o, MeasurementSchedule sched " //
        + "LEFT JOIN sched.definition def " //
        + "LEFT JOIN sched.resource res " //
        + "LEFT JOIN sched.baseline bal " //
        + "    WHERE o.id = sched.id " //
        + "      AND sched.definition = def " //
        + "      AND sched.resource = res " //
        + "      AND bal.schedule = sched " //
        + "      AND :resourceId = res.id "), //
    @NamedQuery(name = MeasurementOOB.GET_HIGHEST_FACTORS_FOR_GROUP, query = "" //
        + "   SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite" //
        + "        ( res.name, res.id, res.ancestry, res.resourceType.id, def.displayName, sched.id, o.timestamp, def.id, o.oobFactor, " //
        + "          bal.baselineMin , bal.baselineMax, def.units ) " //
        + "     FROM MeasurementOOB o, MeasurementSchedule sched " //
        + "LEFT JOIN sched.definition def " //
        + "LEFT JOIN sched.resource res " //
        + "LEFT JOIN sched.baseline bal " //
        + "LEFT JOIN sched.resource.explicitGroups ig " //
        + "    WHERE o.id = sched.id " //
        + "      AND sched.definition = def " //
        + "      AND sched.resource = res " //
        + "      AND bal.schedule = sched " //
        + "      AND :groupId = ig.id ") })
@Entity
@Table(name = "RHQ_MEASUREMENT_OOB")
public class MeasurementOOB {

    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE = "GetSchedulesWithOObAggregate";
    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE_ADMIN = "GetSchedulesWithOObAggregate_admin";
    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT = "GetSchedulesWithOObAggregateCount";
    public static final String DELETE_OUTDATED = "DeleteOutdatedOOBs";
    public static final String COUNT_FOR_DATE = "CountOOBForDate";
    public static final String GET_HIGHEST_FACTORS_FOR_RESOURCE = "GetHighestOOBFactorForResource";
    public static final String GET_HIGHEST_FACTORS_FOR_GROUP = "GetHighestOOBFactorForGroup";
    public static final String DELETE_FOR_GROUP_AND_DEFINITION = "DeleteOOBForGroupAndDefinition";
    public static final String DELETE_FOR_SCHEDULE = "DeleteOOBForSchedule";

    public static final String DELETE_FOR_RESOURCES = "DeleteOOBForResurces";

    /*
     * (Jan 16, 2009) - The new OOB system uses a threshold to determine whether a metric is a "problem" or not.
     *                  The current threshold is a static value - 0.1 - and does not change based on any characteristic
     *                  of the metric data collected.  Dynamic metrics, however, can have either naturally narrow or
     *                  naturally wide baseline ranges; this makes the static threshold of 0.1 either too sensitive
     *                  or not sensitive enough, respectively.  The next improvement to the baseline system probably
     *                  needs to use a sliding scale for this threshold, to more accurately characterize "problem"
     *                  metrics across varying magnitudes of metric baseline deltas.
     */
    public static final String INSERT_QUERY = "" //
        + "INSERT INTO rhq_measurement_oob_tmp (oob_factor, schedule_id, time_stamp ) \n" //
        + "     ( SELECT max(mx*100) as mxdiff, id, ? \n" //  ?1 = begin
        + "         FROM ( SELECT max(((d.maxvalue - b.bl_max) / (b.bl_max - b.bl_min))) AS mx, d.schedule_id as id \n" //
        + "                  FROM rhq_measurement_bline b, rhq_measurement_data_num_1h d, rhq_measurement_sched sc, rhq_measurement_def def \n" //
        + "                 WHERE b.schedule_id = d.schedule_id \n" //
        + "                   AND sc.id = b.schedule_id \n" //
        + "                   AND d.value > b.bl_max \n" //
        + "                   AND d.time_stamp = ? \n" // ?2 = begin
        + "                   AND (b.bl_max - b.bl_min) > 0.1 \n" //
        + "                   AND (d.maxvalue - b.bl_max) >0 \n " //
        + "                   AND sc.enabled = %TRUE% \n" //
        + "                   AND sc.definition = def.id \n" //
        + "                   AND def.numeric_type = 0 \n" // Only dynamic metrics
        + "              GROUP BY d.schedule_id \n" //
        + "UNION ALL \n" //
        + "      SELECT max(((b.bl_min - d.minvalue) / (b.bl_max - b.bl_min))) AS mx, d.schedule_id as id \n" //
        + "        FROM rhq_measurement_bline b, rhq_measurement_data_num_1h d, rhq_measurement_sched sc, rhq_measurement_def def \n" //
        + "       WHERE b.schedule_id = d.schedule_id \n" //
        + "         AND sc.id = b.schedule_id \n" //
        + "         AND d.value < b.bl_max  \n" //
        + "         AND d.time_stamp = ? \n" // ?3 = begin
        + "         AND (b.bl_max - b.bl_min) > 0.1 \n" //
        + "         AND (b.bl_min - d.minvalue) >0 \n" //
        + "         AND sc.enabled = %TRUE% \n" //
        + "         AND sc.definition = def.id \n" //
        + "         AND def.numeric_type = 0 \n" //
        + "    GROUP BY d.schedule_id \n" //
        + "   ) data \n" //
        + "GROUP BY id, mx \n" //
        + "  HAVING mx > 0.05 )";

    public static final String UPDATE_MASTER_POSTGRES = "" //
        + "UPDATE rhq_measurement_oob \n"
        + "   SET oob_factor = rhq_measurement_oob_tmp.oob_factor,time_stamp=rhq_measurement_oob_tmp.time_stamp \n"
        + "  FROM rhq_measurement_oob_tmp \n"
        + " WHERE rhq_measurement_oob_tmp.oob_factor > rhq_measurement_oob.oob_factor \n"
        + "   AND rhq_measurement_oob_tmp.schedule_id = rhq_measurement_oob.schedule_id ";

    public static final String MERGE_TABLES_ORACLE = "" //
        + "MERGE INTO rhq_measurement_oob oob_ \n" //
        + "     USING rhq_measurement_oob_tmp tmp_ \n" //
        + "        ON ( tmp_.schedule_id = oob_.schedule_id ) \n"
        + "      WHEN MATCHED THEN UPDATE SET oob_factor = tmp_.oob_factor, time_stamp = tmp_.time_stamp \n"
        + "      WHEN NOT MATCHED THEN INSERT ( oob_.schedule_id, oob_.time_stamp, oob_.oob_factor ) \n"
        + "                            VALUES ( tmp_.schedule_id, tmp_.time_stamp, tmp_.oob_factor )";

    /*
     * H2 syntax doesn't support the more complex SET...FROM...WHERE like Postgres, and although it does support
     * MERGE it doesn't support the WHEN [NOT] MATCHED syntax; so we'll just delete any and all OOBs that have become
     * obsolete, and follow that up with a call to INSERT_NEW_ONES, which should give us the same effect as row updates
     */
    public static final String UPDATE_MASTER_GENERIC = "" //
        + "DELETE FROM rhq_measurement_oob \n" //
        + "      WHERE EXISTS ( SELECT oob_tmp.schedule_id \n" //
        + "                       FROM rhq_measurement_oob_tmp oob_tmp \n" //
        + "                      WHERE oob_tmp.oob_factor > rhq_measurement_oob.oob_factor \n" //
        + "                        AND oob_tmp.schedule_id = rhq_measurement_oob.schedule_id ) ";

    public static final String INSERT_NEW_ONES = "" //
        + "INSERT INTO rhq_measurement_oob (oob_factor, schedule_id, time_stamp) \n"
        + "     ( SELECT oob_factor, schedule_id,  time_stamp \n"
        + "         FROM rhq_measurement_oob_tmp \n"
        + "        WHERE NOT EXISTS ( SELECT rhq_measurement_oob.schedule_id \n "
        + "                             FROM rhq_measurement_oob \n "
        + "                            WHERE rhq_measurement_oob.schedule_id = rhq_measurement_oob_tmp.schedule_id ) )";

    public static final String TRUNCATE_TMP_TABLE = "TRUNCATE TABLE rhq_measurement_oob_tmp";

    public static final String SECURITY_ADDITION = "" //
        + " AND ( res.id IN ( SELECT rr.id  FROM Resource rr " //
        + "                     JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " //
        + "                    WHERE s.id = :subjectId ) ) ";

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SCHEDULE_ID")
    int id;

    @Column(name = "TIME_STAMP")
    long timestamp;
    /**
     * The 'severity' of the violation. Original data is double, but we
     * don't need that precision here, so use an int to conserve space
     */
    @Column(name = "OOB_FACTOR")
    private int oobFactor;

    public MeasurementOOB() {
    }

    public int getScheduleId() {
        return id;
    }

    public void setScheduleId(int scheduleId) {
        id = scheduleId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getOobFactor() {
        return oobFactor;
    }

    public void setOobFactor(int oobFactor) {
        this.oobFactor = oobFactor;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MeasurementOOB");
        sb.append("{id=").append(id);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", oobFactor=").append(oobFactor);
        sb.append('}');
        return sb.toString();
    }
}
