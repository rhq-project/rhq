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
package org.rhq.core.domain.configuration;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.rhq.core.domain.configuration.group.AggregateResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;

@DiscriminatorValue("resource")
@Entity
@NamedQueries( {
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS, query = "SELECT cu "
        + "  FROM ResourceConfigurationUpdate cu " + " WHERE cu.status = :status"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID, query = "SELECT cu "
        + "  FROM ResourceConfigurationUpdate cu " + " WHERE cu.resource.id = :resourceId"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG, query = "SELECT cu "
        + "  FROM ResourceConfigurationUpdate cu " + " WHERE cu.resource.id = :resourceId "
        + "   AND cu.status = 'SUCCESS' " + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) "
        + "   FROM ResourceConfigurationUpdate cu2 " + "  WHERE cu2.resource.id = :resourceId "
        + "    AND cu2.status = 'SUCCESS') "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID, query = "SELECT cu "
        + "  FROM ResourceConfigurationUpdate cu " + " WHERE cu.resource.id = :resourceId "
        + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " + "   FROM ResourceConfigurationUpdate cu2 "
        + "  WHERE cu2.resource.id = :resourceId) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM ResourceConfigurationUpdate rcu WHERE rcu.resource IN (:resources))"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE, query = "UPDATE ResourceConfigurationUpdate rcu SET rcu.aggregateConfigurationUpdate = null WHERE rcu.aggregateConfigurationUpdate IN ( select arcu FROM AggregateResourceConfigurationUpdate arcu WHERE arcu.group.id = :groupId )") })
public class ResourceConfigurationUpdate extends AbstractResourceConfigurationUpdate {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_IN_STATUS = "ResourceConfigurationUpdate.findAllInStatus";
    public static final String QUERY_FIND_ALL_BY_RESOURCE_ID = "ResourceConfigurationUpdate.findAllByResourceId";
    public static final String QUERY_FIND_CURRENTLY_ACTIVE_CONFIG = "ResourceConfigurationUpdate.findCurrentlyActiveConfig";
    public static final String QUERY_FIND_LATEST_BY_RESOURCE_ID = "ResourceConfigurationUpdate.findByResource";
    public static final String QUERY_DELETE_BY_RESOURCES = "ResourceConfigurationUpdate.deleteByResources";
    public static final String QUERY_DELETE_UPDATE_AGGREGATE = "ResourceConfigurationUpdate.deleteUpdateAggregate";

    @JoinColumn(name = "CONFIG_RES_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private Resource resource;

    @JoinColumn(name = "AGG_RES_UPDATE_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private AggregateResourceConfigurationUpdate aggregateConfigurationUpdate;

    protected ResourceConfigurationUpdate() {
    } // JPA

    public ResourceConfigurationUpdate(Resource resource, Configuration config, String subjectName) {
        super(config, subjectName);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public AggregateResourceConfigurationUpdate getAggregateConfigurationUpdate() {
        return aggregateConfigurationUpdate;
    }

    public void setAggregateConfigurationUpdate(AggregateResourceConfigurationUpdate aggregateConfigurationUpdate) {
        this.aggregateConfigurationUpdate = aggregateConfigurationUpdate;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", resource=").append(this.resource);

        if (aggregateConfigurationUpdate != null) {
            // circular toString if you try to print the entire aggregateConfigurationUpdate object
            str.append(", aggregateResourceConfigurationUpdate=").append(aggregateConfigurationUpdate.getId());
        }
    }
}