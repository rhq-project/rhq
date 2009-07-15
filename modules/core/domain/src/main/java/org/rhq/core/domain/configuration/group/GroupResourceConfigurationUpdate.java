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
package org.rhq.core.domain.configuration.group;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorValue("resource")
@Entity
@NamedQueries( {
    @NamedQuery(name = GroupResourceConfigurationUpdate.QUERY_FIND_ALL_IN_STATUS, query = "" //
        + "SELECT arcu " //
        + "  FROM GroupResourceConfigurationUpdate arcu " //
        + " WHERE arcu.status = :status"),
    @NamedQuery(name = GroupResourceConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, query = "SELECT arcu "
        + "  FROM GroupResourceConfigurationUpdate AS arcu " // 
        + " WHERE arcu.group.id = :groupId"),
    @NamedQuery(name = GroupResourceConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID, query = "" //
        + "SELECT cgu " //
        + "  FROM GroupResourceConfigurationUpdate cgu " //
        + " WHERE cgu.group.id = :groupId " //
        + "   AND cgu.modifiedTime = ( SELECT MAX(cgu2.modifiedTime) " //
        + "                             FROM GroupResourceConfigurationUpdate cgu2 " //
        + "                            WHERE cgu2.group.id = :groupId ) ") })
public class GroupResourceConfigurationUpdate extends AbstractGroupConfigurationUpdate {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL_IN_STATUS = "GroupResourceConfigurationUpdate.findAllInStatus";
    public static final String QUERY_FIND_BY_GROUP_ID = "GroupResourceConfigurationUpdate.findByGroupId";
    public static final String QUERY_FIND_LATEST_BY_GROUP_ID = "GroupResourceConfigurationUpdate.findLatestByGroupId";

    @OneToMany(mappedBy = "groupConfigurationUpdate", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<ResourceConfigurationUpdate> configurationUpdates = new ArrayList<ResourceConfigurationUpdate>();

    protected GroupResourceConfigurationUpdate() {
    } // JPA

    public GroupResourceConfigurationUpdate(ResourceGroup group, String subjectName) {
        super(group, subjectName);
        // TODO (ips, 02/13/09): This is a temporary workaround - we don't really need to store a Configuration at all,
        //      since it can be recalculated from the member configs when needed, but the AbstractConfigurationUpdate
        //      base class requires the configuration field to be non-null.
        this.configuration = new Configuration();
    }

    public void setConfigurationUpdates(List<ResourceConfigurationUpdate> configurationUpdates) {
        this.configurationUpdates = configurationUpdates;
    }

    public List<ResourceConfigurationUpdate> getConfigurationUpdates() {
        return this.configurationUpdates;
    }

    public void addConfigurationUpdate(ResourceConfigurationUpdate groupMember) {
        this.configurationUpdates.add(groupMember);
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", resourceConfigurationUpdates=").append(getConfigurationUpdates());
    }
}