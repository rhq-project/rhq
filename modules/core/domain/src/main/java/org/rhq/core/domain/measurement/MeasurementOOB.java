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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
                        "           sched.id,max(o.id.timestamp),def.id,max(o.oobFactor),avg(o.oobFactor),bal.baselineMin , bal.baselineMax, def.units) " +
                        "FROM MeasurementOOB o "+
                        "LEFT JOIN o.schedule sched " +
                        "LEFT JOIN sched.definition def " +
                        "LEFT JOIN sched.resource res " +
                        "LEFT JOIN sched.baseline bal " +
                        "WHERE (o.id.timestamp >= :begin AND o.id.timestamp <= :end )" +
                        "  AND o.id.scheduleId = sched.id " +
                        "  AND sched.definition = def " +
                        "  AND sched.resource = res " +
                        "  AND bal.schedule = sched " +
                        "  AND (:resourceId = res.id OR :resourceId is null )" +
                        "GROUP BY res.name, res.id, def.displayName, sched.id, def.id, bal.baselineMin , bal.baselineMax, def.units "
                            ),
        @NamedQuery(name=MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT,
                query = "  SELECT sched.id " +
                        "  FROM MeasurementOOB o "+
                        "  LEFT JOIN o.schedule sched " +
                        "  LEFT JOIN sched.definition def " +
                        "  LEFT JOIN sched.resource res " +
                        "  WHERE (o.id.timestamp >= :begin AND o.id.timestamp <= :end )" +
                        "    AND o.id.scheduleId = sched.id " +
                        "    AND sched.definition = def " +
                        "    AND sched.resource = res " +
                        "  GROUP BY sched.id "
                        ),
        @NamedQuery(name=MeasurementOOB.GET_FACTOR_FOR_SCHEDULES,
                query= "SELECT o.id.scheduleId,max(o.oobFactor),avg(o.oobFactor)" +
                        "FROM MeasurementOOB o "+
                        "WHERE (o.id.timestamp >= :begin AND o.id.timestamp <= :end )" +
                        "  AND o.id.scheduleId IN (:schedules)  " +
                        "GROUP BY o.id.scheduleId"
        ),
        @NamedQuery(name=MeasurementOOB.DELETE_OUTDATED,
                query = "DELETE FROM MeasurementOOB o " +
                        "WHERE o.id.scheduleId IN (" +
                        "  SELECT b.schedule.id " +
                        "  FROM MeasurementBaseline b " +
                        "  WHERE b.computeTime > :cutOff" +
                        ")"
        ),
        @NamedQuery(name=MeasurementOOB.COUNT_FOR_DATE,
                query = "SELECT COUNT(o) FROM MeasurementOOB o " +
                        "WHERE o.id.timestamp = :timestamp"
        ),
        @NamedQuery(name=MeasurementOOB.GET_HIGHEST_FACTORS_FOR_RESOURCE,
                query = "SELECT new org.rhq.core.domain.measurement.composite.MeasurementOOBComposite(res.name,res.id,def.displayName," +
                        "           sched.id,o.id.timestamp,def.id,o.oobFactor,bal.baselineMin , bal.baselineMax, def.units ) " +
                        "FROM MeasurementOOB o "+
                        "LEFT JOIN o.schedule sched " +
                        "LEFT JOIN sched.definition def " +
                        "LEFT JOIN sched.resource res " +
                        "LEFT JOIN sched.baseline bal " +
                        "WHERE (o.id.timestamp >= :begin AND o.id.timestamp <= :end )" +
                        "  AND o.id.scheduleId = sched.id " +
                        "  AND sched.definition = def " +
                        "  AND sched.resource = res " +
                        "  AND bal.schedule = sched " +
                        "  AND :resourceId = res.id "// +
        )
})
@Entity
@Table(name="RHQ_MEASUREMENT_OOB")
public class MeasurementOOB {

    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE = "GetSchedulesWithOObAggregate";

    public static final String GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT = "GetSchedulesWithOObAggregateCount";

    public static final String GET_FACTOR_FOR_SCHEDULES = "GetFactorForSchedules";

    public static final String DELETE_OUTDATED = "DeleteOutdatedOOBs";

    public static final String COUNT_FOR_DATE = "CountForDate";

    public static final String GET_HIGHEST_FACTORS_FOR_RESOURCE = "GetHighestFactorForResource";

    public static final String INSERT_QUERY_POSTGRES =
            "insert into rhq_measurement_oob (oob_factor, schedule_id,  time_stamp )  \n" +
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
                    "         AND sc.enabled = true\n" +
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
                    "         AND sc.enabled = true\n" +
                    "         AND sc.definition = def.id\n" +
                    "         AND def.numeric_type = 0\n" +
                    "    group by d.schedule_id \n" +
                    " ) data\n" +
                    " group by id, mx\n" +
                    " having mx > 0.01 " +
                    ")";

    public static final String INSERT_QUERY_ORACLE = "insert into rhq_measurement_oob (oob_factor, schedule_id,  time_stamp )  \n" +
            "(\n" +
            "SELECT max(mx*100) as mxdiff, id, ? \n" +
            "FROM  (\n" +
            "    SELECT max(((d.maxvalue - b.bl_max) / (b.bl_max - b.bl_min))) AS mx, d.schedule_id as id\n" +
            "    FROM rhq_measurement_bline b, rhq_measurement_data_num_1h d, rhq_measurement_sched sc, rhq_measurement_def def\n" +
            "    WHERE b.schedule_id = d.schedule_id\n" +
            "         AND sc.id = b.schedule_id\n" +
            "         AND d.value > b.bl_max\n" +
            "         AND d.time_stamp = ?\n" +
            "         AND ((b.bl_max - b.bl_min) > 0.1 )\n" +  // TODO delta depending on max value ?
            "         AND (d.maxvalue - b.bl_max) >0 \n " +
            "         AND sc.enabled = 1\n" +
            "         AND sc.definition = def.id\n" +
            "         AND def.numeric_type = 0\n" +
            "   GROUP BY d.schedule_id\n" +
            " UNION ALL \n" +
            "        SELECT   max(((b.bl_min - d.minvalue) / (b.bl_max - b.bl_min))) AS mx, d.schedule_id as id\n" +
            "       FROM rhq_measurement_bline b, rhq_measurement_data_num_1h d, rhq_measurement_sched sc, rhq_measurement_def def\n" +
            "       WHERE b.schedule_id = d.schedule_id\n" +
            "           AND sc.id = b.schedule_id\n" +
            "           AND d.value < b.bl_max  \n" +
            "           AND d.time_stamp = ?\n" +
            "           AND ((b.bl_max - b.bl_min) > 0.1)\n" +   // TODO delta depending on max value ?
            "           AND (b.bl_min - d.minvalue) >0 \n" +
            "           AND sc.enabled = 1\n" +
            "           AND sc.definition = def.id\n" +
            "           AND def.numeric_type = 0\n" +
            "       GROUP BY d.schedule_id \n" +
            ")\n" +
            "GROUP BY id, mx\n" +
            "HAVING mx > 0.01" +
            ") ";
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    MeasurementDataPK id; // Same PK, so reuse of that class
    @JoinColumn(name = "SCHEDULE_ID", insertable = false, updatable = false, nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    MeasurementSchedule schedule;
    /**
     * The 'severity' of the violation. Original data is double, but we
     * don't need that precision here, so use an int to conserve space
     */
    @Column(name="OOB_FACTOR")
    private int oobFactor;

    protected MeasurementOOB() {

    }

    public int getScheduleId() {
        return id.scheduleId;
    }

    public long getTimestamp() {
        return id.timestamp;
    }

    public int getOobFactor() {
        return oobFactor;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MeasurementOOB");
        sb.append("{id=").append(id);
        sb.append(", oobFactor=").append(oobFactor);
        sb.append('}');
        return sb.toString();
    }
}
