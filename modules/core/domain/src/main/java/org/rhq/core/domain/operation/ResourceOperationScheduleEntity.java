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
package org.rhq.core.domain.operation;

import java.util.Date;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.rhq.core.domain.resource.Resource;

/**
 * Information on a specific operation schedule on a particular resource.
 *
 * @author John Mazzitelli
 */

@DiscriminatorValue("resource")
@Entity
@NamedQueries( {
    @NamedQuery(name = ResourceOperationScheduleEntity.QUERY_FIND_BY_RESOURCE_ID, query = "SELECT s "
        + "  FROM ResourceOperationScheduleEntity s " + " WHERE s.resource.id = :resourceId "),
    @NamedQuery(name = ResourceOperationScheduleEntity.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM ResourceOperationScheduleEntity s "
        + " WHERE s.resource.id IN ( :resourceIds ) )") })
public class ResourceOperationScheduleEntity extends OperationScheduleEntity {
    public static final String QUERY_DELETE_BY_RESOURCES = "ResourceOperationScheduleEntity.QUERY_DELETE_BY_RESOURCES";
    public static final String QUERY_FIND_BY_RESOURCE_ID = "ResourceOperationScheduleEntity.findByResourceId";

    private static final long serialVersionUID = 1L;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID")
    @ManyToOne
    private Resource resource;

    protected ResourceOperationScheduleEntity() {
    }

    public ResourceOperationScheduleEntity(String jobName, String jobGroup, Date nextFireTime, Resource resource) {
        super(jobName, jobGroup, nextFireTime);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceOperationScheduleEntity: ");
        str.append("resource=[" + this.resource);
        str.append("], " + super.toString());
        str.append("]");
        return str.toString();
    }
}