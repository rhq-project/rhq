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
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorValue("group")
@Entity
@NamedQueries( { @NamedQuery(name = GroupOperationScheduleEntity.QUERY_FIND_BY_GROUP_ID, query = "SELECT s "
    + "  FROM GroupOperationScheduleEntity s " + " WHERE s.group.id = :groupId ") })
public class GroupOperationScheduleEntity extends OperationScheduleEntity {
    public static final String QUERY_FIND_BY_GROUP_ID = "GroupOperationScheduleEntity.findByGroupId";

    private static final long serialVersionUID = 1L;

    @JoinColumn(name = "GROUP_ID", referencedColumnName = "ID")
    @ManyToOne
    private ResourceGroup group;

    protected GroupOperationScheduleEntity() {
    }

    public GroupOperationScheduleEntity(String jobName, String jobGroup, Date nextFireTime, ResourceGroup group) {
        super(jobName, jobGroup, nextFireTime);
        this.group = group;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("GroupOperationScheduleEntity: ");
        str.append("group=[" + this.group);
        str.append("], " + super.toString());
        str.append("]");
        return str.toString();
    }
}