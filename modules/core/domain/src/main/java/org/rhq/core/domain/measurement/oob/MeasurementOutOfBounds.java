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
package org.rhq.core.domain.measurement.oob;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.measurement.MeasurementSchedule;

/**
 * An indicator that a measurement has gone out of bounds (OOB) and by how much.
 *
 * @author John Mazzitelli
 */

@Entity
@NamedQueries( {
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_BY_ID_AND_LOAD, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob LEFT JOIN FETCH oob.schedule LEFT JOIN FETCH oob.schedule.resource "
        + "   WHERE oob.id = :id "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_ALL_ADMIN, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob " + "   WHERE oob.occurred >= :oldest "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_ALL, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob JOIN oob.schedule.resource.implicitGroups g JOIN g.roles r JOIN r.subjects s "
        + "   WHERE oob.occurred >= :oldest AND s = :subject "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE_ADMIN, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob "
        + "   WHERE oob.occurred >= :oldest AND oob.schedule.resource.id = :resourceId "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_FOR_RESOURCE, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob JOIN oob.schedule.resource.implicitGroups g JOIN g.roles r JOIN r.subjects s "
        + "   WHERE oob.occurred >= :oldest AND oob.schedule.resource.id = :resourceId AND s = :subject "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE_ADMIN, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob " + "   WHERE oob.occurred BETWEEN :begin AND :end "
        + "     AND oob.schedule.id = :scheduleId "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_COUNT_FOR_SCHEDULE_IDS_ADMIN, query = "  SELECT oob.schedule.id,count(*) "
        + "    FROM MeasurementOutOfBounds oob "
        + "   WHERE oob.occurred BETWEEN :begin AND :end "
        + "     AND oob.schedule.id IN ( :scheduleIds ) " + "GROUP BY oob.schedule.id"),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_FOR_SCHEDULE, query = "  SELECT oob "
        + "    FROM MeasurementOutOfBounds oob JOIN oob.schedule.resource.implicitGroups g JOIN g.roles r JOIN r.subjects s "
        + "   WHERE oob.occurred BETWEEN :begin AND :end " + "   AND oob.schedule.id = :scheduleId AND s = :subject "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_FIND_FOR_DEFINITION_AND_RESOURCEIDS_ADMIN, query = " SELECT oob "
        + "   FROM MeasurementOutOfBounds oob " + "  WHERE oob.occurred BETWEEN :begin AND :end "
        + "    AND oob.schedule.definition.id = :definitionId " + "    AND oob.schedule.resource IN (:resources) "),
    @NamedQuery(name = MeasurementOutOfBounds.QUERY_DELETE_BY_RESOURCES, query = "DELETE MeasurementOutOfBounds oob WHERE oob.schedule IN ( SELECT ms FROM MeasurementSchedule ms WHERE ms.resource IN ( :resources ) )") })
@SequenceGenerator(name = "RHQ_MEASUREMENT_OOB_ID_SEQ", sequenceName = "RHQ_MEASUREMENT_OOB_ID_SEQ")
@Table(name = "RHQ_MEASUREMENT_OOB")
public class MeasurementOutOfBounds implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_ID_AND_LOAD = "MeasurementOutOfBounds.findByIdAndLoad";
    public static final String QUERY_FIND_ALL_ADMIN = "MeasurementOutOfBounds.findAll_admin";
    public static final String QUERY_FIND_ALL = "MeasurementOutOfBounds.findAll";
    public static final String QUERY_FIND_FOR_RESOURCE_ADMIN = "MeasurementOutOfBounds.findForResource_admin";
    public static final String QUERY_FIND_FOR_RESOURCE = "MeasurementOutOfBounds.findForResource";
    public static final String QUERY_FIND_FOR_SCHEDULE_ADMIN = "MeasurementOutOfBounds.findForSchedule_admin";
    public static final String QUERY_FIND_FOR_SCHEDULE = "MeasurementOutOfBounds.findForSchedule";
    public static final String QUERY_FIND_FOR_DEFINITION_AND_RESOURCEIDS_ADMIN = "MeasurementOutOfBounds.findForDefinitionAndResourceIdsAdmin";
    public static final String QUERY_COUNT_FOR_SCHEDULE_IDS_ADMIN = "MeasurementOutOfBounds.QUERY_COUNT_FOR_SCHEDULE_ADMIN";
    public static final String QUERY_DELETE_BY_RESOURCES = "MeasurementOutOfBounds.deleteByResources";

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_MEASUREMENT_OOB_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "SCHEDULE_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private MeasurementSchedule schedule;

    @Column(name = "OCCURRED", nullable = false)
    private long occurred;

    @Column(name = "DIFF", nullable = false)
    private double diff;

    protected MeasurementOutOfBounds() {
    }

    public MeasurementOutOfBounds(MeasurementSchedule schedule, long occurred, double diff) {
        setSchedule(schedule);
        setOccurred(occurred);
        setDiff(diff);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public MeasurementSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(MeasurementSchedule schedule) {
        if (schedule == null) {
            throw new NullPointerException("schedule==null");
        }

        this.schedule = schedule;
        schedule.addOutOfBounds(this);
    }

    /**
     * Returns the time (in epoch milliseconds) when a measurement was detected to have gone out-of-bounds from its
     * baseline.
     *
     * @return epoch milliseconds of when the out-of-bounds occurred
     */
    public long getOccurred() {
        return occurred;
    }

    public void setOccurred(long occurred) {
        this.occurred = occurred;
    }

    /**
     * Returns the difference between the baseline and the actual measured value. If this returned value is greater than
     * 0, the measured value exceeded the maximum baseline; if less than 0, the measured value dipped below the minimum
     * baseline.
     *
     * @return the difference between the measured value and the baseline.
     */
    public double getDiff() {
        return this.diff;
    }

    public void setDiff(double diff) {
        this.diff = diff;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((schedule == null) ? 0 : schedule.hashCode());
        result = (31 * result) + (int) (occurred ^ (occurred >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof MeasurementOutOfBounds)) {
            return false;
        }

        final MeasurementOutOfBounds other = (MeasurementOutOfBounds) obj;
        if (occurred != other.occurred) {
            return false;
        }

        if (schedule == null) {
            if (other.schedule != null) {
                return false;
            }
        } else if (!schedule.equals(other.schedule)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("OOB: ");
        str.append("id=[" + id);
        str.append("]; schedule=[" + schedule);
        str.append("]; occurred=[" + new Date(occurred));
        str.append("]; diff=[" + diff);
        str.append("]");
        return str.toString();
    }
}