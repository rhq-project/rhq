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
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.status = :status"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_BY_RESOURCE_ID, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE ( cu.resource.id = :resourceId OR :resourceId IS NULL ) " //
        + "   AND ( cu.createdTime > :startTime OR :startTime IS NULL ) " //
        + "   AND ( cu.modifiedTime < :endTime OR :endTime IS NULL ) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_CURRENTLY_ACTIVE_CONFIG, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.resource.id = :resourceId " //
        + "   AND cu.status = 'SUCCESS' " //
        + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " //
        + "                             FROM ResourceConfigurationUpdate cu2 " //
        + "                            WHERE cu2.resource.id = :resourceId " //
        + "                              AND cu2.status = 'SUCCESS' ) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_RESOURCE_ID, query = "" //
        + "SELECT cu " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.resource.id = :resourceId " //
        + "   AND cu.modifiedTime = ( SELECT MAX(cu2.modifiedTime) " // 
        + "                             FROM ResourceConfigurationUpdate cu2 " //
        + "                            WHERE cu2.resource.id = :resourceId ) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID, query = "" //
        + "SELECT new org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite" //
        + "       ( cu.id, cu.status, cu.errorMessage, cu.subjectName, cu.createdTime, cu.modifiedTime, " // update w/o config
        + "         res.id, res.name ) " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + "  JOIN cu.resource res " //
        + " WHERE cu.aggregateConfigurationUpdate.id = :aggregateConfigurationUpdateId"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_BY_PARENT_UPDATE_ID, query = "" //
        + "SELECT cu.id " //
        + "  FROM ResourceConfigurationUpdate cu " //
        + " WHERE cu.aggregateConfigurationUpdate.id = :aggregateConfigurationUpdateId"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_COMPOSITES_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite" //
        + "        ( cu.id, cu.status, cu.errorMessage, cu.subjectName, cu.createdTime, cu.modifiedTime, " // update w/o config
        + "          res.id, res.name, parent.id, parent.name ) " //
        + "     FROM ResourceConfigurationUpdate cu " //
        + "     JOIN cu.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE (cu.modifiedTime <> (SELECT MIN(icu.modifiedTime) " // 
        + "                                 FROM ResourceConfigurationUpdate icu " //
        + "                                WHERE icu.resource.id = res.id))" //
        + "      AND (UPPER(res.name) LIKE :resourceFilter OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter OR :parentFilter IS NULL) " //
        + "      AND (cu.createdTime > :startTime OR :startTime IS NULL) " //
        + "      AND (cu.modifiedTime < :endTime OR :endTime IS NULL) " //
        + "      AND (cu.status LIKE :status OR :status IS NULL) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_FIND_ALL_COMPOSITES, query = "" //
        + "   SELECT new org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite" //
        + "        ( cu.id, cu.status, cu.errorMessage, cu.subjectName, cu.createdTime, cu.modifiedTime, " // update w/o config
        + "          res.id, res.name, parent.id, parent.name ) " //
        + "     FROM ResourceConfigurationUpdate cu " //
        + "     JOIN cu.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " //
        + "                       WHERE s.id = :subjectId ) " //
        + "      AND (cu.modifiedTime <> (SELECT MIN(icu.modifiedTime) " // 
        + "                                 FROM ResourceConfigurationUpdate icu " //
        + "                                WHERE icu.resource.id = res.id))" //
        + "      AND (UPPER(res.name) LIKE :resourceFilter OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter OR :parentFilter IS NULL) " //
        + "      AND (cu.createdTime > :startTime OR :startTime IS NULL) " //
        + "      AND (cu.modifiedTime < :endTime OR :endTime IS NULL) " //
        + "      AND (cu.status LIKE :status OR :status IS NULL) "),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_1, query = "" //
        + "DELETE FROM Configuration c " //
        + " WHERE c IN ( SELECT rcu.configuration " //
        + "                FROM ResourceConfigurationUpdate rcu " //
        + "               WHERE rcu.resource IN ( :resources ) " //
        + "                AND NOT rcu.configuration = rcu.resource.resourceConfiguration )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_BY_RESOURCES_2, query = ""
        + "DELETE FROM ResourceConfigurationUpdate rcu " //
        + " WHERE rcu.resource IN ( :resources )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE, query = "" //
        + "UPDATE ResourceConfigurationUpdate rcu " //
        + "   SET rcu.aggregateConfigurationUpdate = NULL " //
        + " WHERE rcu.aggregateConfigurationUpdate IN ( SELECT arcu " //
        + "                                               FROM AggregateResourceConfigurationUpdate arcu " //
        + "                                              WHERE arcu.id = :arcuId )"),
    @NamedQuery(name = ResourceConfigurationUpdate.QUERY_DELETE_UPDATE_AGGREGATE_BY_GROUP, query = "" //
        + "UPDATE ResourceConfigurationUpdate rcu " //
        + "   SET rcu.aggregateConfigurationUpdate = NULL " //
        + " WHERE rcu.aggregateConfigurationUpdate IN ( SELECT arcu " //
        + "                                               FROM AggregateResourceConfigurationUpdate arcu " //
        + "                                              WHERE arcu.group.id = :groupId )") })
public class ResourceConfigurationUpdate extends AbstractResourceConfigurationUpdate {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_IN_STATUS = "ResourceConfigurationUpdate.findAllInStatus";
    public static final String QUERY_FIND_ALL_BY_RESOURCE_ID = "ResourceConfigurationUpdate.findAllByResourceId";
    public static final String QUERY_FIND_CURRENTLY_ACTIVE_CONFIG = "ResourceConfigurationUpdate.findCurrentlyActiveConfig";
    public static final String QUERY_FIND_LATEST_BY_RESOURCE_ID = "ResourceConfigurationUpdate.findByResource";
    public static final String QUERY_FIND_COMPOSITE_BY_PARENT_UPDATE_ID = "ResourceConfigurationUpdate.findCompositeByParentUpdateId";
    public static final String QUERY_FIND_BY_PARENT_UPDATE_ID = "ResourceConfigurationUpdate.findByParentUpdateId";

    // for subsystem views
    public static final String QUERY_FIND_ALL_COMPOSITES = "ResourceConfigurationUpdate.findAllComposites";
    public static final String QUERY_FIND_ALL_COMPOSITES_ADMIN = "ResourceConfigurationUpdate.findAllComposites_admin";

    // for efficient object cleanup/purge
    public static final String QUERY_DELETE_BY_RESOURCES_1 = "ResourceConfigurationUpdate.deleteByResources1";
    public static final String QUERY_DELETE_BY_RESOURCES_2 = "ResourceConfigurationUpdate.deleteByResources2";
    public static final String QUERY_DELETE_UPDATE_AGGREGATE = "ResourceConfigurationUpdate.deleteUpdateAggregate";
    public static final String QUERY_DELETE_UPDATE_AGGREGATE_BY_GROUP = "ResourceConfigurationUpdate.deleteUpdateAggregateByGroup";

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