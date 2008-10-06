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

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;

@DiscriminatorValue("plugin")
@Entity
@NamedQueries( {
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS, query = "SELECT cu "
        + "  FROM PluginConfigurationUpdate cu "),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG, query = "SELECT cu "
        + "  FROM PluginConfigurationUpdate cu " + " WHERE cu.resource.id = :resourceId "
        + "   AND cu.status <> 'INPROGRESS' " + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) "
        + "  FROM PluginConfigurationUpdate cu2 " + " WHERE cu2.resource.id = :resourceId "
        + "   AND cu2.status <> 'INPROGRESS') "),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID, query = "SELECT cu "
        + "  FROM PluginConfigurationUpdate cu " + " WHERE cu.resource.id = :resourceId "
        + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " + "  FROM PluginConfigurationUpdate cu2 "
        + " WHERE cu2.resource.id = :resourceId) "),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, query = "" //
        + "SELECT new org.rhq.core.domain.configuration.composite.PluginConfigurationUpdateResourceComposite( cu, cu.resource.id, cu.resource.name ) "
        + "  FROM PluginConfigurationUpdate cu "
        + " WHERE cu.aggregateConfigurationUpdate.id = :aggregateConfigurationUpdateId"),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, query = "SELECT cu.id "
        + "  FROM PluginConfigurationUpdate cu "
        + " WHERE cu.aggregateConfigurationUpdate.id = :aggregateConfigurationUpdateId"),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID, query = "SELECT cu.status "
        + "  FROM PluginConfigurationUpdate cu "
        + " WHERE cu.aggregateConfigurationUpdate.id = :aggregateConfigurationUpdateId GROUP BY cu.status"),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM PluginConfigurationUpdate pcu WHERE pcu.resource IN (:resources))"),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE_BY_GROUP, query = "UPDATE PluginConfigurationUpdate pcu SET pcu.aggregateConfigurationUpdate = null WHERE pcu.aggregateConfigurationUpdate IN ( select apcu FROM AggregatePluginConfigurationUpdate apcu WHERE apcu.group.id = :groupId )"),
    @NamedQuery(name = PluginConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE, query = "UPDATE PluginConfigurationUpdate pcu SET pcu.aggregateConfigurationUpdate = null WHERE pcu.aggregateConfigurationUpdate IN ( select apcu FROM AggregatePluginConfigurationUpdate apcu WHERE apcu.id = :apcuId )") })
public class PluginConfigurationUpdate extends AbstractResourceConfigurationUpdate {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_IN_STATUS = "PluginConfigurationUpdate.findAllInStatus";
    public static final String QUERY_FIND_CURRENTLY_ACTIVE_CONFIG = "PluginConfigurationUpdate.findCurrentlyActiveConfig";
    public static final String QUERY_FIND_LATEST_BY_RESOURCE_ID = "PluginConfigurationUpdate.findByResource";
    public static final String QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID = "PluginConfigurationUpdate.findCompositeByParentUpdateId";
    public static final String QUERY_FIND_BY_PARENT_UPDATE_ID = "PluginConfigurationUpdate.findByParentUpdateId";
    public static final String QUERY_FIND_STATUS_BY_PARENT_UPDATE_ID = "PluginConfigurationUpdate.findStatusByParentUpdateId";
    public static final String QUERY_DELETE_BY_RESOURCES = "PluginConfigurationUpdate.deleteByResources";
    public static final String QUERY_DELETE_UPDATE_AGGREGATE_BY_GROUP = "pluginConfigurationUpdate.deleteUpdateAggregateByGroup";
    public static final String QUERY_DELETE_UPDATE_AGGREGATE = "pluginConfigurationUpdate.deleteUpdateAggregate";

    @JoinColumn(name = "PLUGIN_CONFIG_RES_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private Resource resource;

    @JoinColumn(name = "AGG_PLUGIN_UPDATE_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne
    private AggregatePluginConfigurationUpdate aggregateConfigurationUpdate;

    protected PluginConfigurationUpdate() {
    } // JPA

    public PluginConfigurationUpdate(Resource resource, Configuration config, String subjectName) {
        super(config, subjectName);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public AggregatePluginConfigurationUpdate getAggregateConfigurationUpdate() {
        return aggregateConfigurationUpdate;
    }

    public void setAggregateConfigurationUpdate(AggregatePluginConfigurationUpdate aggregateConfigurationUpdate) {
        this.aggregateConfigurationUpdate = aggregateConfigurationUpdate;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", resource=").append(this.resource);

        if (aggregateConfigurationUpdate != null) {
            // circular toString if you try to print the entire aggregateConfigurationUpdate object
            str.append(", aggregatePluginConfigurationUpdate=").append(aggregateConfigurationUpdate.getId());
        }
    }
}