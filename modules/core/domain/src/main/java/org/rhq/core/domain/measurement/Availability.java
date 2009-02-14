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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import org.rhq.core.domain.resource.Resource;

/**
 * This Entity Bean stores information about resource availability. The actual data is run-length encoded. This means
 * that the {@link #getAvailabilityType() availability state} (e.g. UP or DOWN) changed at the
 * {@link #getStartTime() start time}.
 *
 * This provides historical data regarding when a resource was up or down.  To get the current availability
 * of a resource, it is best to use the {@link ResourceAvailability} entity instead.
 *  
 * @see ResourceAvailability
 * 
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Availability.FIND_CURRENT_BY_RESOURCE, query = "SELECT av FROM Availability av "
        + " WHERE av.resource.id = :resourceId " + "   AND av.endTime IS NULL " + "ORDER BY av.startTime ASC "), // this order by is on purpose - for handling NonUniqueResultException problems
    @NamedQuery(name = Availability.FIND_BY_RESOURCE, query = "  SELECT av FROM Availability av "
        + "   WHERE av.resource.id = :resourceId " + "ORDER BY av.startTime ASC"),
    @NamedQuery(name = Availability.FIND_BY_RESOURCE_NO_SORT, query = "  SELECT av FROM Availability av "
        + "   WHERE av.resource.id = :resourceId "),

    // get all current resource availabilities for those that do not match a given availability type
    @NamedQuery(name = Availability.FIND_NONMATCHING_WITH_RESOURCE_ID_BY_AGENT_AND_TYPE, query = "SELECT new org.rhq.core.domain.resource.composite.ResourceIdWithAvailabilityComposite(av.resource.id, av) "
        + "  FROM Availability av "
        + " WHERE av.resource.agent.id = :agentId "
        + "   AND ((av.availabilityType <> :availabilityType AND :availabilityType IS NOT NULL) "
        + "        OR (av.availabilityType IS NOT NULL AND :availabilityType IS NULL) "
        + "        OR (av.availabilityType IS NULL AND :availabilityType IS NOT NULL))" + "   AND av.endTime IS NULL"),
    @NamedQuery(name = Availability.FIND_FOR_RESOURCE_WITHIN_INTERVAL, query = "SELECT av FROM Availability av "
        + " WHERE av.resource.id = :resourceId "
        + "   AND ((av.startTime <= :start AND (av.endTime >= :start OR av.endTime IS NULL) ) " /* availability straddles :start */
        + "       OR (av.startTime BETWEEN :start AND :end)) " /* interval straddles availability.startTime */
        + "ORDER BY av.startTime ASC"),
    @NamedQuery(name = Availability.FIND_FOR_RESOURCE_GROUP_WITHIN_INTERVAL, query = "SELECT av FROM Availability av "
        + " WHERE av.resource.id IN (SELECT ires.id FROM ResourceGroup rg JOIN rg.implicitResources ires WHERE rg.id = :groupId) "
        + "   AND ((av.startTime <= :start AND (av.endTime >= :start OR av.endTime IS NULL) ) " /* availability straddles :start */
        + "       OR (av.startTime BETWEEN :start AND :end)) " /* interval straddles availability.startTime */
        + "ORDER BY av.startTime ASC"),
    @NamedQuery(name = Availability.FIND_FOR_AUTO_GROUP_WITHIN_INTERVAL, query = "SELECT av FROM Availability av "
        + " JOIN av.resource res JOIN res.parentResource parent JOIN res.resourceType type " //
        + " WHERE parent.id = :parentId AND type.id = :typeId "
        + "   AND ((av.startTime <= :start AND (av.endTime >= :start OR av.endTime IS NULL) ) " /* availability straddles :start */
        + "       OR (av.startTime BETWEEN :start AND :end)) " /* interval straddles availability.startTime */
        + "ORDER BY av.startTime ASC"),
    @NamedQuery(name = Availability.FIND_BY_RESOURCE_AND_DATE, query = "SELECT av FROM Availability av "
        + " WHERE av.resource.id = :resourceId " + "   AND av.startTime <= :aTime "
        + "   AND (av.endTime >= :aTime OR av.endTime IS NULL) "),

    // Returns 0 if the agent is backfilled as DOWN, returns 1 if not.
    // It does this very simply - if the platform is down, the only way it could have
    // gone down is by the check-suspect-agent's backfiller - avail reports never show
    // a downed platform because the platform plugins all return UP for platform availability.
    @NamedQuery(name = Availability.QUERY_IS_AGENT_BACKFILLED, query = "SELECT COUNT(DISTINCT av.availabilityType) FROM Availability av WHERE "
        + "   av.resource.agent.name = :agentName AND "
        + "   av.resource.parentResource IS NULL AND "
        + "   av.endTime IS NULL AND " + "   NOT av.availabilityType = 0"),
    @NamedQuery(name = Availability.QUERY_DELETE_BY_RESOURCES, query = "DELETE Availability a WHERE a.resource IN ( :resources )") })
@SequenceGenerator(name = "Generator", sequenceName = "RHQ_AVAILABILITY_ID_SEQ")
@Table(name = "RHQ_AVAILABILITY")
public class Availability implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String FIND_CURRENT_BY_RESOURCE = "Availability.findCurrentByResource";
    public static final String FIND_BY_RESOURCE = "Availability.findByResource";
    public static final String FIND_BY_RESOURCE_NO_SORT = "Availability.findByResourceNoSort";
    public static final String FIND_NONMATCHING_WITH_RESOURCE_ID_BY_AGENT_AND_TYPE = "Availability.findNonmatchingWithResourceIdByAgentAndType";
    public static final String FIND_FOR_RESOURCE_WITHIN_INTERVAL = "Availability.findForResourceWithinInterval";
    public static final String FIND_FOR_RESOURCE_GROUP_WITHIN_INTERVAL = "Availability.findForResourceGroupWithinInterval";
    public static final String FIND_FOR_AUTO_GROUP_WITHIN_INTERVAL = "Availability.findForAutoGroupWithinInterval";
    public static final String FIND_BY_RESOURCE_AND_DATE = "Availability.findByResourceAndDate";
    public static final String QUERY_IS_AGENT_BACKFILLED = "Availability.isAgentBackfilled";
    public static final String QUERY_DELETE_BY_RESOURCES = "Availability.deleteByResources";

    public static final String NATIVE_QUERY_PURGE = "DELETE FROM RHQ_AVAILABILITY WHERE END_TIME < ?";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Generator")
    @Id
    private int id;

    /**
     * Start time of this availability state
     */
    @Column(name = "START_TIME", nullable = false)
    private long startTime;

    /**
     * End time of this availability state (which is the start of the next availability time period)
     */
    @Column(name = "END_TIME", nullable = true)
    private Long endTime;

    /**
     * Availability state for this time period
     */
    @Column(name = "AVAILABILITY_TYPE", nullable = true)
    @Enumerated(EnumType.ORDINAL)
    private AvailabilityType availabilityType;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource resource;

    protected Availability() {
        // for JPA use only
    }

    /**
     * Constructor for {@link Availability}. If <code>type</code> is <code>null</code>, it will be considered unknown.
     *
     * @param resource
     * @param startTime
     * @param type
     */
    public Availability(Resource resource, Date startTime, AvailabilityType type) {
        if (resource == null) {
            throw new IllegalArgumentException("resource==null");
        }

        this.resource = resource;
        this.availabilityType = type;
        this.startTime = (startTime != null) ? startTime.getTime() : new Date().getTime();
        this.endTime = null;
    }

    public int getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public Date getStartTime() {
        return new Date(startTime);
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime.getTime();
    }

    /**
     * The end time of this availability period. This will be <code>null</code> if this represents the last known
     * availability.
     *
     * @return end of the availability period
     */
    public Date getEndTime() {
        return (endTime != null) ? new Date(endTime.longValue()) : null;
    }

    public void setEndTime(Date endTime) {
        this.endTime = (endTime != null) ? endTime.getTime() : null;
    }

    /**
     * Indicates the availability status as either UP or DOWN; if <code>null</code> is returned, the status is unknown.
     *
     * @return availability status
     */
    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    /**
     * Sets the availability status. This can be <code>null</code> to indicate an "unknown" availability status.
     *
     * @param availabilityType
     */
    public void setAvailabilityType(AvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    @Override
    public String toString() {
        return "Availability[id=" + id + ",type=" + this.availabilityType + ",start-time=" + getStartTime()
            + ",end-time=" + getEndTime() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((availabilityType == null) ? 0 : availabilityType.hashCode());
        result = (prime * result) + ((endTime == null) ? 0 : endTime.hashCode());
        result = (prime * result) + ((resource == null) ? 0 : resource.hashCode());
        result = (prime * result) + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Availability)) {
            return false;
        }

        final Availability other = (Availability) obj;
        if (availabilityType == null) {
            if (other.availabilityType != null) {
                return false;
            }
        } else if (!availabilityType.equals(other.availabilityType)) {
            return false;
        }

        if (endTime == null) {
            if (other.endTime != null) {
                return false;
            }
        } else if (!endTime.equals(other.endTime)) {
            return false;
        }

        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        if (startTime != other.startTime) {
            return false;
        }

        return true;
    }
}