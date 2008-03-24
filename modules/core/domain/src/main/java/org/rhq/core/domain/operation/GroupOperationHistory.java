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
package org.rhq.core.domain.operation;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorValue("group")
@Entity
@NamedQueries( {
    @NamedQuery(name = GroupOperationHistory.QUERY_FIND_ABANDONED_IN_PROGRESS, query = "select h "
        + "from GroupOperationHistory h where h.id IN "
        + "( select distinct ih.id from GroupOperationHistory ih join ih.resourceOperationHistories iroh "
        + "where iroh.status <> :status and ih.status = :status )"),
    @NamedQuery(name = GroupOperationHistory.QUERY_FIND_BY_GROUP_ID_AND_STATUS, query = "select h "
        + "from GroupOperationHistory h " + "where h.group.id = :groupId " + "and h.status = :status"),
    @NamedQuery(name = GroupOperationHistory.QUERY_FIND_BY_GROUP_ID_AND_NOT_STATUS, query = "select h "
        + "from GroupOperationHistory h " + "where h.group.id = :groupId " + "and h.status <> :status") })
public class GroupOperationHistory extends OperationHistory {
    public static final String QUERY_FIND_ABANDONED_IN_PROGRESS = "GroupOperationHistory.findAbandonedInProgress";
    public static final String QUERY_FIND_BY_GROUP_ID_AND_STATUS = "GroupOperationHistory.findByGroupIdAndStatus";
    public static final String QUERY_FIND_BY_GROUP_ID_AND_NOT_STATUS = "GroupOperationHistory.findByGroupIdAndNotStatus";

    private static final long serialVersionUID = 1L;

    @JoinColumn(name = "GROUP_ID", referencedColumnName = "ID")
    @ManyToOne
    private ResourceGroup group;

    @OneToMany(mappedBy = "groupOperationHistory", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<ResourceOperationHistory> resourceOperationHistories = new ArrayList<ResourceOperationHistory>();

    protected GroupOperationHistory() {
    }

    public GroupOperationHistory(String jobName, String jobGroup, String subjectName,
        OperationDefinition operationDefinition, Configuration parameters, ResourceGroup group) {
        super(jobName, jobGroup, subjectName, operationDefinition, parameters);
        this.group = group;
        setStartedTime(); // group operation histories are started implicitly
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    public List<ResourceOperationHistory> getResourceOperationHistories() {
        return resourceOperationHistories;
    }

    public void setResourceOperationHistories(List<ResourceOperationHistory> resourceOperationHistories) {
        this.resourceOperationHistories = resourceOperationHistories;
    }

    public void addResourceOperationHistory(ResourceOperationHistory history) {
        this.resourceOperationHistories.add(history);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("GroupOperationHistory: ");
        str.append("group=[" + this.group);
        str.append("], " + super.toString());
        str.append("]");
        return str.toString();
    }
}