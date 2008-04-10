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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@NamedQueries( {
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
    @NamedQuery(name = MeasurementDataTrait.QUERY_DELETE_BY_RESOURCES, query = "DELETE MeasurementDataTrait t WHERE t.schedule IN ( SELECT ms FROM MeasurementSchedule ms WHERE ms.resource IN ( :resources ) )") })
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

    /*
     * Each time this is called, it will delete the oldest datum for each group of
     * data identified by schedule_id that has more than one element in it; so this
     * query should be called in the purge job until it returns zero.
     *
     * NOTE: Avoid using the AS keyword in the FROM clauses in this query, because Oracle barfs on it
     *       (see http://download.oracle.com/docs/cd/B19306_01/server.102/b14200/ap_standard_sql003.htm, subfeature id
     *       E051-08).
     */
    public static final String NATIVE_QUERY_PURGE = "" + // 
        "DELETE FROM rhq_measurement_data_trait t " + //
        "WHERE t.time_stamp = " + //
        "(" + //
        "        SELECT MIN(it.time_stamp) " + //
        "        FROM rhq_measurement_data_trait it " + //
        "        WHERE t.time_stamp < ? " + //
        "        AND it.schedule_id = it.schedule_id " + //
        "        GROUP BY it.schedule_id " + //
        "        HAVING COUNT(it.schedule_id) > 1 " + //
        ")";

    private static final long serialVersionUID = 1L;

    @Column(length = 255)
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
        return "MeasurementDataTrait[" + "value=[" + value + "], " + super.toString() + "]";
    }
}