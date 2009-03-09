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
@NamedQueries({
        @NamedQuery(name=MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE,
                query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite(res.name,res.id,def.displayName," +
                        "           sched.id, o.timestamp,def.id,o.oobFactor,bal.baselineMin , bal.baselineMax," +
                        "           def.units, parent.name, parent.id) " +
                        "FROM MeasurementOOB o, MeasurementSchedule sched "+
                        "LEFT JOIN sched.definition def " +
                        "LEFT JOIN sched.resource res " +
                        "LEFT JOIN sched.baseline bal " +
                        "LEFT JOIN res.parentResource parent " +
                        "WHERE o.id = sched.id " +
                        "  AND sched.definition = def " +
                        "  AND sched.resource = res " +
                        "  AND bal.schedule = sched " +
                        "  AND (UPPER(def.displayName ) LIKE :metricName OR :metricName is null ) " +
                        "  AND (UPPER(res.name) LIKE :resourceName OR :resourceName is null ) " +
                        "  AND (UPPER(parent.name) LIKE :parentName OR :parentName is null ) "
                            ),
        @NamedQuery(name=MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT,
                query = "SELECT sched.id " +
                        "FROM MeasurementOOB o , MeasurementSchedule sched "+
                        "LEFT JOIN sched.definition def " +
                        "LEFT JOIN sched.resource res " +
                        "LEFT JOIN res.parentResource parent " +
                        "WHERE o.id = sched.id " +
                        "  AND sched.definition = def " +
                        "  AND sched.resource = res " +
                        "  AND (UPPER(def.displayName ) LIKE :metricName OR :metricName is null ) " +
                        "  AND (UPPER(res.name) LIKE :resourceName OR :resourceName is null ) " +
                        "  AND (UPPER(parent.name) LIKE :parentName OR :parentName is null ) "
                        ),
        @NamedQuery(name=MeasurementOOB.DELETE_OUTDATED,
                query = "DELETE FROM MeasurementOOB o " +
                        "WHERE o.id IN (" +
                        "  SELECT b.schedule.id " +
                        "  FROM MeasurementBaseline b " +
                        "  WHERE b.computeTime > :cutOff" +
                        ")"
        ),
        @NamedQuery(name=MeasurementOOB.COUNT_FOR_DATE,
                query = "SELECT COUNT(o.timestamp) FROM MeasurementOOB o " +
                        "WHERE o.timestamp = :timestamp"
        ),
        @NamedQuery(name=MeasurementOOB.DELETE_FOR_SCHEDULE,
                query = "DELETE FROM MeasurementOOB o WHERE o.id = :id"
        ),
        @NamedQuery(name=MeasurementOOB.DELETE_FOR_RESOURCES,
                query = "DELETE FROM MeasurementOOB o " +
                        "WHERE o.id IN " +
                        "      ( SELECT ms.id FROM MeasurementSchedule ms WHERE ms.resource IN ( :resources ) )"
        ),
        @NamedQuery(name=MeasurementOOB.GET_HIGHEST_FACTORS_FOR_RESOURCE,
                query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite(res.name,res.id,def.displayName," +
                        "           sched.id,o.timestamp,def.id,o.oobFactor,bal.baselineMin , bal.baselineMax, def.units ) " +
                        "FROM MeasurementOOB o, MeasurementSchedule sched "+
                        "LEFT JOIN sched.definition def " +
                        "LEFT JOIN sched.resource res " +
                        "LEFT JOIN sched.baseline bal " +
                        "WHERE o.id = sched.id " +
                        "  AND sched.definition = def " +
                        "  AND sched.resource = res " +
                        "  AND bal.schedule = sched " +
                        "  AND :resourceId = res.id " +
                        "ORDER BY sched.id, o.oobFactor DESC "
        )
})
@Entity
@Table(name="RHQ_MEASUREMENT_OOB")
public class MeasurementOOB {

    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE = "GetSchedulesWithOObAggregate";
    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT = "GetSchedulesWithOObAggregateCount";
    public static final String DELETE_OUTDATED = "DeleteOutdatedOOBs";
    public static final String COUNT_FOR_DATE = "CountOOBForDate";
    public static final String GET_HIGHEST_FACTORS_FOR_RESOURCE = "GetHighestOOBFactorForResource";
    public static final String DELETE_FOR_SCHEDULE = "DeleteOOBForSchedule";
    public static final String DELETE_FOR_RESOURCES = "DeleteOOBForResurces";

    public static final String INSERT_QUERY =
            "insert into rhq_measurement_oob_tmp (oob_factor, schedule_id,  time_stamp )  \n" +
                    "(SELECT max(mx*100) as mxdiff, id, ?\n" + //  ?1 = begin
                    " FROM\n" +
                    " (\n" +
                    "    SELECT max(((d.maxvalue - b.bl_max) / (b.bl_max - b.bl_min))) AS mx, d.schedule_id as id\n" +
                    "    FROM rhq_measurement_bline b, rhq_measurement_data_num_1h d, rhq_measurement_sched sc, rhq_measurement_def def\n" +
                    "    WHERE b.schedule_id = d.schedule_id\n" +
                    "         AND sc.id = b.schedule_id\n" +
                    "         AND d.value > b.bl_max\n" +
                    "         AND d.time_stamp = ?\n" +   // ?2 = begin
                    "         AND (b.bl_max - b.bl_min) > 0.1 \n" + // TODO delta depending on max value ?
                    "         AND (d.maxvalue - b.bl_max) >0 \n " +
                    "         AND sc.enabled = %TRUE%\n" +
                    "         AND sc.definition = def.id\n" +
                    "         AND def.numeric_type = 0\n" + // Only dynamic metrics
                    "    group by d.schedule_id\n" +
                    " UNION ALL\n" +
                    "   SELECT   max(((b.bl_min - d.minvalue) / (b.bl_max - b.bl_min))) AS mx, d.schedule_id as id\n" +
                    "   FROM rhq_measurement_bline b, rhq_measurement_data_num_1h d, rhq_measurement_sched sc, rhq_measurement_def def\n" +
                    "   WHERE b.schedule_id = d.schedule_id\n" +
                    "         AND sc.id = b.schedule_id\n" +
                    "         AND d.value < b.bl_max  \n" +
                    "         AND d.time_stamp = ?\n" + // ?3 = begin
                    "         AND (b.bl_max - b.bl_min) > 0.1\n" + // TODO delta depending on max value ?
                    "         AND (b.bl_min - d.minvalue) >0 \n" +
                    "         AND sc.enabled = %TRUE%\n" +
                    "         AND sc.definition = def.id\n" +
                    "         AND def.numeric_type = 0\n" +
                    "    group by d.schedule_id \n" +
                    " ) data\n" +
                    " group by id, mx\n" +
                    " having mx > 0.05 " +
                    ")";

    public static final String UPDATE_MASTER_POSTGRES =
                    "update rhq_measurement_oob\n" +
                    "set oob_factor = rhq_measurement_oob_tmp.oob_factor,  time_stamp=rhq_measurement_oob_tmp.time_stamp " +
                    "from rhq_measurement_oob_tmp\n" +
                    "where rhq_measurement_oob_tmp.oob_factor > rhq_measurement_oob.oob_factor\n" +
                    "   and rhq_measurement_oob_tmp.schedule_id = rhq_measurement_oob.schedule_id ";

    public static final String MERGE_TABLES_ORACLE =
                   "merge into rhq_measurement_oob oob_\n" +
                           "using rhq_measurement_oob_tmp tmp_\n" +
                           "ON ( tmp_.schedule_id = oob_.schedule_id )                \n" +
                           "WHEN MATCHED THEN UPDATE SET oob_factor = tmp_.oob_factor, time_stamp = tmp_.time_stamp    \n" +
                           "WHEN NOT MATCHED THEN INSERT (oob_.schedule_id, oob_.time_stamp, oob_.oob_factor) \n" +
                           "    VALUES (tmp_.schedule_id, tmp_.time_stamp, tmp_.oob_factor)";

    public static final String INSERT_NEW_ONES =
                    "insert into rhq_measurement_oob (oob_factor, schedule_id,  time_stamp)  (\n" +
                            "select oob_factor, schedule_id,  time_stamp \n" +
                            "from  rhq_measurement_oob_tmp \n" +
                            "where not exists ( \n" +
                            "\n" +
                            "    select rhq_measurement_oob.schedule_id " +
                            "    from rhq_measurement_oob " +
                            "    where rhq_measurement_oob.schedule_id = rhq_measurement_oob_tmp.schedule_id\n" +
                            ")" +
                            ")";

    public static final String TRUNCATE_TMP_TABLE =
                    "TRUNCATE TABLE rhq_measurement_oob_tmp";

    public static final String SECURITY_ADDITION =
                "  AND (res.id IN  ( SELECT rr.id FROM Resource rr " +
                "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " +
                "                        WHERE s.id = :subjectId )) ";


    private static final long serialVersionUID = 1L;

    @Id
    @Column(name="SCHEDULE_ID")
    int id;

    @Column(name="TIME_STAMP")
    long timestamp;
    /**
     * The 'severity' of the violation. Original data is double, but we
     * don't need that precision here, so use an int to conserve space
     */
    @Column(name="OOB_FACTOR")
    private int oobFactor;

    protected MeasurementOOB() {

    }

    public int getScheduleId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getOobFactor() {
        return oobFactor;
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
