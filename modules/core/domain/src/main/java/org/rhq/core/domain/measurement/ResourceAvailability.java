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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.resource.Resource;

/**
 * This entity represents the latest known availability data for a resource.
 *
 * @author Joseph Marques
 */
@Entity
@Table(name = ResourceAvailability.TABLE_NAME)
@NamedQueries( //
{ @NamedQuery(name = ResourceAvailability.QUERY_FIND_BY_RESOURCE_ID, query = "" //
    + "  SELECT ra FROM ResourceAvailability ra WHERE ra.resourceId = :resourceId "),
    @NamedQuery(name = ResourceAvailability.QUERY_FIND_BY_RESOURCE_IDS, query = "" //
        + "  SELECT ra FROM ResourceAvailability ra WHERE ra.resourceId IN ( :resourceIds ) "),
    @NamedQuery(name = ResourceAvailability.UPDATE_PLATFORM_BY_AGENT_ID, query = "" //
        + "  UPDATE ResourceAvailability " //
        + "     SET availabilityType = :availabilityType " //
        + "   WHERE resourceId IN ( SELECT res.id " //
        + "                           FROM Resource res " //
        + "                          WHERE res.agent.id = :agentId " //
        + "                            AND res.parentResource IS NULL )"),
    @NamedQuery(name = ResourceAvailability.UPDATE_CHILD_BY_AGENT_ID, query = "" //
        + "  UPDATE ResourceAvailability ra" //
        + "     SET availabilityType = :availabilityType " //
        + "   WHERE resourceId IN ( SELECT res.id " //
        + "                           FROM Resource res " //
        + "                          WHERE res.agent.id = :agentId " //
        + "                            AND res.parentResource IS NOT NULL ) " //
        + "     AND ra.availabilityType <> :disabled "),
    /*
     * Platform plugins always return up for availability.  Platforms are
     * only down if the check-suspect-agent's backfiller sets them down.
     * Thus this agent has been backfilled if it's platform is not up.
     *
     * Returns 0 if the agent has NOT been backfilled, non-zero if it is.
     */
    @NamedQuery(name = ResourceAvailability.QUERY_IS_AGENT_BACKFILLED, query = "" //
        + "SELECT COUNT(avail.id) " // return count of
        + "  FROM Resource res " //
        + "  JOIN res.currentAvailability avail " // we only want the current availability
        + " WHERE res.agent.id = :agentId " // use id not name to prevent an unnecessary join to agent table
        + "   AND res.parentResource IS NULL " // we only want platforms
        + "   AND avail.availabilityType <> 1") // get all NOT UP
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_RESOURCE_AVAIL_ID_SEQ", sequenceName = "RHQ_RESOURCE_AVAIL_ID_SEQ")
public class ResourceAvailability implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_RESOURCE_AVAIL";

    public static final String QUERY_FIND_BY_RESOURCE_ID = "ResourceAvailability.findByResourceId";
    public static final String QUERY_FIND_BY_RESOURCE_IDS = "ResourceAvailability.findByResourceIds";
    public static final String UPDATE_CHILD_BY_AGENT_ID = "ResourceAvailability.updateChildByAgentId";
    public static final String UPDATE_PLATFORM_BY_AGENT_ID = "ResourceAvailability.updatePlatformByAgentId";
    public static final String QUERY_IS_AGENT_BACKFILLED = "ResourceAvailability.isAgentBackfilled";

    @SuppressWarnings("unused")
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_RESOURCE_AVAIL_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Resource resource;

    @Column(name = "RESOURCE_ID", insertable = false, updatable = false)
    private int resourceId;

    /**
     * Availability state for this time period
     */
    @Column(name = "AVAILABILITY_TYPE", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private AvailabilityType availabilityType;

    protected ResourceAvailability() {
        // for JPA use only
    }

    /**
     * Constructor for {@link ResourceAvailability}.
     *
     * @param resource
     * @param type
     */
    public ResourceAvailability(Resource resource, AvailabilityType type) {
        if (resource == null) {
            throw new IllegalArgumentException("resource==null");
        }

        this.resource = resource;
        this.resourceId = resource.getId();
        this.availabilityType = type;
    }

    public Resource getResource() {
        return resource;
    }

    /**
     * Lightweight way to get only the resource ID, as getResource goes through a lazy proxy
     */
    public int getResourceId() {
        return resourceId;
    }

    /**
     * @return availability type
     */
    public AvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(AvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    @Override
    public String toString() {
        return "Availability[resourceId=" + resourceId + ", avail=" + this.availabilityType + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((availabilityType == null) ? 0 : availabilityType.hashCode());
        result = (prime * result) + resourceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof ResourceAvailability)) {
            return false;
        }

        final ResourceAvailability other = (ResourceAvailability) obj;

        if (resourceId != other.resourceId) {
            return false;
        }

        if (availabilityType == null) {
            if (other.availabilityType != null) {
                return false;
            }
        } else if (!availabilityType.equals(other.availabilityType)) {
            return false;
        }

        return true;
    }
}