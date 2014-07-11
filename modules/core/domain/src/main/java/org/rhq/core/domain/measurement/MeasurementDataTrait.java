/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@NamedQueries({
    @NamedQuery(name = MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE_AND_DISPLAY_TYPE, query = "SELECT trait, d.displayName "
        + "FROM MeasurementDataTrait trait JOIN trait.schedule s join s.definition d JOIN s.resource r"
        + " WHERE r.id = :resourceId "
        + "  AND trait.id.timestamp = "
        + "       (SELECT max(mdt.id.timestamp) "
        + "       FROM MeasurementDataTrait mdt "
        + "        WHERE s.id = mdt.schedule.id "
        + "        )  "
        + "  AND d.displayType = :displayType "),
    @NamedQuery(name = MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE, query = "SELECT trait, d.displayName "
        + "FROM MeasurementDataTrait trait JOIN trait.schedule s join s.definition d JOIN s.resource r"
        + " WHERE r.id = :resourceId " + "  AND trait.id.timestamp = " + "       (SELECT max(mdt.id.timestamp) "
        + "       FROM MeasurementDataTrait mdt " + "        WHERE s.id = mdt.schedule.id)  "),
    @NamedQuery(name = MeasurementDataTrait.FIND_CURRENT_FOR_SCHEDULES, query = "SELECT mdt, d.displayName FROM MeasurementDataTrait mdt JOIN mdt.schedule s JOIN s.definition d "
        + "WHERE mdt.id.scheduleId IN ( :scheduleIds ) "
        + " AND mdt.id.timestamp = "
        + " ( SELECT MAX(x.id.timestamp) "
        + "   FROM MeasurementDataTrait x "
        + "   WHERE x.id.scheduleId = mdt.id.scheduleId" + " ) "),
    @NamedQuery(name = MeasurementDataTrait.FIND_ALL_FOR_RESOURCE_AND_DEFINITION, query = "SELECT trait, d.displayName "
        + "FROM MeasurementDataTrait trait JOIN trait.schedule s JOIN s.definition d JOIN s.resource r "
        + " WHERE r.id = :resourceId " + "  AND d.id = :definitionId " + "ORDER BY trait.id.timestamp DESC "),
    @NamedQuery(name = MeasurementDataTrait.QUERY_DELETE_BY_RESOURCES, query = "DELETE MeasurementDataTrait t WHERE t.schedule IN ( SELECT ms FROM MeasurementSchedule ms WHERE ms.resource.id IN ( :resourceIds ) )") })
@Table(name = "RHQ_MEASUREMENT_DATA_TRAIT")
public class MeasurementDataTrait extends MeasurementData {

    /**
     * Find current traits for a Resource in :resourceId that have a certain displayType in :displayType
     */
    public static final String FIND_CURRENT_FOR_RESOURCE_AND_DISPLAY_TYPE = "MeasurementDataTrait.FindCurrentForResourceAndDislayType";

    /**
     * Find all current traits for a Resource in :resourceId
     */
    public static final String FIND_CURRENT_FOR_RESOURCE = "MeasurementDataTrait.FindCurrentForResource";

    /**
     * Find the current traits for the schedule ids passed in :scheduleIds
     */
    public static final String FIND_CURRENT_FOR_SCHEDULES = "MeasurementDataTrait.FIND_CURRENT_FOR_SCHEDULES";

    /**
     * Find all trait data for the provided resource id and definition id resource id is first parameter definition id
     * is second parameter
     */
    public static final String FIND_ALL_FOR_RESOURCE_AND_DEFINITION = "MeasurementDataTrait.FIND_ALL_FOR_RESOURCE_AND_DEFINITION";

    public static final String QUERY_DELETE_BY_RESOURCES = "MeasurementDataTrait.deleteByResources";

    /**
     * @deprecated as of RHQ 4.13, no longer used
     */
    @Deprecated
    public static final String NATIVE_QUERY_PURGE = "" //
        + "DELETE FROM rhq_measurement_data_trait " // SQL Server doesn't like aliases, use full table name
        + "WHERE EXISTS " // rewritten as exists because H2 doesn't support multi-column conditions
        + "  (SELECT t2.schedule_id, t2.time_stamp " //
        + "   FROM rhq_measurement_data_trait t2, " //
        + "     (SELECT max(t4.time_stamp) as mx, t4.schedule_id as schedule_id " //
        + "      FROM rhq_measurement_data_trait t4 " //
        + "      WHERE t4.time_stamp < ? " //
        + "      GROUP BY t4.schedule_id) t3 " //
        + "   WHERE t2.schedule_id = t3.schedule_id " //
        + "   AND t2.time_stamp < t3.mx " //
        + "   AND rhq_measurement_data_trait.time_stamp = t2.time_stamp " // rewrote multi-column conditions as additional
        + "   AND rhq_measurement_data_trait.schedule_id = t2.schedule_id) "; // correlated restrictions to the delete table

    private static final long serialVersionUID = 1L;

    private String value;

    /**
     * Create a new trait object with the current system time for the timestamp.
     *
     * @param request basically the {@link MeasurementSchedule} for this trait
     * @param value   the metric value of this trait
     */
    public MeasurementDataTrait(MeasurementScheduleRequest request, String value) {
        super(request);
        this.value = value;
    }

    /**
     * Create a new Trait object.
     *
     * @param timestamp time when the measurement was taken
     * @param request   basically the {@link MeasurementSchedule} for this trait
     * @param value     the metric value of this trait
     */
    public MeasurementDataTrait(long timestamp, MeasurementScheduleRequest request, String value) {
        super(timestamp, request);
        this.value = value;
    }

    /**
     * Create a new Trait object. MeasurementSchedule and timestamp are given in the passed pk.
     *
     * @param pk    primary key
     * @param value the measurement value for this trait
     */
    public MeasurementDataTrait(MeasurementDataPK pk, String value) {
        super(pk);
        this.value = value;
    }

    protected MeasurementDataTrait() {
        /* JPA use only */
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MeasurementDataTrait[name=" + getName() + ", value=\"" + this.value + "\", scheduleId="
                + this.id.scheduleId + ", timestamp=" + this.id.timestamp + "]";
    }

}
