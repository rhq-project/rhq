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
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_BY_RESOURCE_ID_AND_NOT_STATUS, query = "select h "
        + "from ResourceOperationHistory h " + "where h.resource.id = :resourceId " + "and h.status <> :status"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_LATEST_COMPLETED_OPERATION, query = "select h "
        + "from ResourceOperationHistory h " + "where h.resource.id = :resourceId " + "  and h.status <> 'INPROGRESS' "
        + "  and h.modifiedTime = (select max(h2.modifiedTime) " + "from ResourceOperationHistory h2 "
        + "where h2.resource.id = :resourceId " + "  and h2.status <> 'INPROGRESS')"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_FIND_OLDEST_INPROGRESS_OPERATION, query = "select h "
        + "from ResourceOperationHistory h " + "where h.resource.id = :resourceId " + "  and h.status = 'INPROGRESS' "
        + "  and h.modifiedTime = (select min(h2.modifiedTime) " + "from ResourceOperationHistory h2 "
        + "where h2.resource.id = :resourceId " + "  and h2.status = 'INPROGRESS')"),
    @NamedQuery(name = ResourceOperationHistory.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM ResourceOperationHistory roh "
        + " WHERE roh.resource IN (:resources))") })
public class ResourceOperationHistory extends OperationHistory {
    public static final String QUERY_FIND_ALL_IN_STATUS = "ResourceOperationHistory.findAllInStatus";
    public static final String QUERY_FIND_BY_GROUP_OPERATION_HISTORY_ID = "ResourceOperationHistory.findByGroupOperationHistoryId";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_STATUS = "ResourceOperationHistory.findByResourceIdAndStatus";
    public static final String QUERY_FIND_BY_RESOURCE_ID_AND_NOT_STATUS = "ResourceOperationHistory.findByResourceIdAndNotStatus";
    public static final String QUERY_FIND_LATEST_COMPLETED_OPERATION = "ResourceOperationHistory.findLatestCompletedOperation";
    public static final String QUERY_FIND_OLDEST_INPROGRESS_OPERATION = "ResourceOperationHistory.findOldestInProgressOperation";
    public static final String QUERY_DELETE_BY_RESOURCES = "ResourceOperationHistory.deleteByResources";

    private static final long serialVersionUID = 1L;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID")
    @ManyToOne
    private Resource resource;

    @JoinColumn(name = "RESULTS_CONFIG_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
    private Configuration results;

    @JoinColumn(name = "GROUP_HISTORY_ID", referencedColumnName = "ID")
    @ManyToOne
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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ResourceOperationHistory: ");
        str.append("resource=[" + this.resource);

        // there may be some operations whose results are sensitive values, like passwords
        // do not show them by default - but allow testers to see them via a system property
        if (Boolean.getBoolean("rhq.test.operation.show-values")) {
            str.append("], results=[" + this.results);
        }

        str.append("], group-history=[" + this.groupOperationHistory);
        str.append("], " + super.toString());
        str.append("]");
        return str.toString();
    }
}