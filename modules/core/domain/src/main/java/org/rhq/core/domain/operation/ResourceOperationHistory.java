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

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;

/**
 * Information on a specific operation execution on a particular resource. This individual operation execution may or
 * may not have been part of a {@link #getGroupOperationHistory() group execution}.
 *
 * @author John Mazzitelli
 */
@DiscriminatorValue("resource")
@Entity
@NamedQueries( {
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_ALL_IN_STATUS, query = "SELECT h FROM ResourceOperationHistory h WHERE h.status = :status"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_BY_GROUP_OPERATION_HISTORY_ID, query = "select h "
        + "from ResourceOperationHistory h " + "where h.groupOperationHistory.id = :groupHistoryId "),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_BY_RESOURCE_ID_AND_STATUS, query = "select h "
        + "from ResourceOperationHistory h " + "where h.resource.id = :resourceId " + "and h.status = :status"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_BY_RESOURCE_ID_AND_NOT_STATUS, query = "" //
        + "SELECT h " //
        + "  FROM ResourceOperationHistory h " //
        + " WHERE h.resource.id = :resourceId " //
        + "   AND h.status <> :status " //
        + "   AND (h.startedTime > :beginTime OR :beginTime IS NULL) " //
        + "   AND (h.modifiedTime < :endTime OR :endTime IS NULL) "),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_LATEST_COMPLETED_OPERATION, query = "select h "
        + "from ResourceOperationHistory h " + "where h.resource.id = :resourceId " + "  and h.status <> 'INPROGRESS' "
        + "  and h.modifiedTime = (select max(h2.modifiedTime) " + "from ResourceOperationHistory h2 "
        + "where h2.resource.id = :resourceId " + "  and h2.status <> 'INPROGRESS')"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_OLDEST_INPROGRESS_OPERATION, query = "select h "
        + "from ResourceOperationHistory h " + "where h.resource.id = :resourceId " + "  and h.status = 'INPROGRESS' "
        + "  and h.modifiedTime = (select min(h2.modifiedTime) " + "from ResourceOperationHistory h2 "
        + "where h2.resource.id = :resourceId " + "  and h2.status = 'INPROGRESS')"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM ResourceOperationHistory roh "
        + " WHERE roh.resource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_ALL_ADMIN, query = "" //
        + "   SELECT new org.rhq.core.domain.operation.composite.ResourceOperationHistoryComposite" // 
        + "        ( roh, parent.id, parent.name ) " //
        + "     FROM ResourceOperationHistory roh " //
        + "     JOIN roh.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (roh.startedTime > :startTime OR :startTime IS NULL) " //
        + "      AND (roh.modifiedTime < :endTime OR :endTime IS NULL) " //
        + "      AND (roh.status LIKE :status OR :status IS NULL) "), //
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_ALL, query = "" //
        + "   SELECT new org.rhq.core.domain.operation.composite.ResourceOperationHistoryComposite" // 
        + "        ( roh, parent.id, parent.name ) " //
        + "     FROM ResourceOperationHistory roh " //
        + "     JOIN roh.resource res " //
        + "LEFT JOIN res.parentResource parent " //
        + "    WHERE res.id IN ( SELECT rr.id FROM Resource rr " //
        + "                        JOIN rr.implicitGroups g JOIN g.roles r JOIN r.subjects s " //
        + "                       WHERE s.id = :subjectId ) " //
        + "      AND (UPPER(res.name) LIKE :resourceFilter ESCAPE :escapeChar OR :resourceFilter IS NULL) " //
        + "      AND (UPPER(parent.name) LIKE :parentFilter ESCAPE :escapeChar OR :parentFilter IS NULL) " //
        + "      AND (roh.startedTime > :startTime OR :startTime IS NULL) " //
        + "      AND (roh.modifiedTime < :endTime OR :endTime IS NULL) " //
        + "      AND (roh.status LIKE :status OR :status IS NULL) "),
    @NamedQuery(name = ResourceOperationHistory.QUERY_DETACH_FROM_GROUP_HISTORY, query = ""
        + "UPDATE ResourceOperationHistory h set  h.groupOperationHistory = NULL WHERE h.groupOperationHistory.id = :historyId") })
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceOperationHistory extends OperationHistory {
    public static final String QUERY_FIND_ALL_IN_STATUS = "ResourceOperationHistory.findAllInStatus";
    public static final String QUERY_FIND_BY_GROUP_OPERATION_HISTORY_ID = "ResourceOperationHistory.findByGroupOperationHistoryId";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_STATUS = "ResourceOperationHistory.findByResourceIdAndStatus";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_NOT_STATUS = "ResourceOperationHistory.findByResourceIdAndNotStatus";
    public static final String QUERY_FIND_LATEST_COMPLETED_OPERATION = "ResourceOperationHistory.findLatestCompletedOperation";
    public static final String QUERY_FIND_OLDEST_INPROGRESS_OPERATION = "ResourceOperationHistory.findOldestInProgressOperation";
    public static final String QUERY_DELETE_BY_RESOURCES = "ResourceOperationHistory.deleteByResources";
    public static final String QUERY_DETACH_FROM_GROUP_HISTORY = "ResourceOperationHistory.detachFromGroupHistory";

    // for subsystem views
    public static final String QUERY_FIND_ALL = "OperationHistory.findAll";
    public static final String QUERY_FIND_ALL_ADMIN = "OperationHistory.findAll_admin";

    private static final long serialVersionUID = 1L;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID")
    @ManyToOne
    @XmlTransient
    private Resource resource;

    @JoinColumn(name = "RESULTS_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, optional = true)
    private Configuration results;

    @JoinColumn(name = "GROUP_HISTORY_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(optional = true)
    private GroupOperationHistory groupOperationHistory;

    protected ResourceOperationHistory() {
    }

    public ResourceOperationHistory(String jobName, String jobGroup, String subjectName,
        OperationDefinition operationDefinition, Configuration parameters, Resource resource,
        GroupOperationHistory groupHistory) {
        super(jobName, jobGroup, subjectName, operationDefinition, parameters);
        this.resource = resource;
        setGroupOperationHistory(groupHistory);
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Configuration getResults() {
        return results;
    }

    public void setResults(Configuration results) {
        this.results = results;
    }

    /**
     * If this individual resource operation execution history was part of a group operation execution, this will return
     * the non-<code>null</code> history for that group execution. If this was not part of a group execution, this will
     * return <code>null</code>.
     *
     * @return the group history entity
     */
    public GroupOperationHistory getGroupOperationHistory() {
        return groupOperationHistory;
    }

    public void setGroupOperationHistory(GroupOperationHistory groupOperationHistory) {
        this.groupOperationHistory = groupOperationHistory;

        if (this.groupOperationHistory != null) {
            groupOperationHistory.addResourceOperationHistory(this);
        }
    }

    /* 
     * there may be some operations whose results are sensitive values, such as passwords - do not show them
     * as part of the toString.  they can still be gotten by explicitly calling getResults()
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceOperationHistory: ");
        str.append("resource=[" + this.resource);
        str.append("], group-history=[" + this.groupOperationHistory);
        str.append("], " + super.toString());
        str.append("]");
        return str.toString();
    }
}