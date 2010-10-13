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
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorValue("plugin")
@Entity
@NamedQueries( {
    @NamedQuery(name = GroupPluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, query = "SELECT apcu "
        + "FROM GroupPluginConfigurationUpdate AS apcu " + "WHERE apcu.group.id = :groupId"),
    @NamedQuery(name = GroupPluginConfigurationUpdate.QUERY_FIND_LATEST_BY_GROUP_ID, query = "" //
        + "SELECT cgu " //
        + "  FROM GroupPluginConfigurationUpdate cgu " //
        + " WHERE cgu.group.id = :groupId " //
        + "   AND cgu.modifiedTime = ( SELECT MAX(cgu2.modifiedTime) " //
        + "                             FROM GroupPluginConfigurationUpdate cgu2 " //
        + "                            WHERE cgu2.group.id = :groupId ) "),
    @NamedQuery(name = GroupPluginConfigurationUpdate.QUERY_DELETE_BY_ID, query = "DELETE "
        + "FROM GroupPluginConfigurationUpdate AS apcu " + "WHERE apcu.id IN ( :ids ) ") })
public class GroupPluginConfigurationUpdate extends AbstractGroupConfigurationUpdate {
    public static final String QUERY_FIND_BY_GROUP_ID = "GroupPluginConfigurationUpdate.findByGroupId";
    public static final String QUERY_FIND_LATEST_BY_GROUP_ID = "GroupPluginConfigurationUpdatee.findLatestByGroupId";
    public static final String QUERY_DELETE_BY_ID = "GroupPluginConfigurationUpdate.deleteById";

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "groupConfigurationUpdate", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private List<PluginConfigurationUpdate> configurationUpdates = new ArrayList<PluginConfigurationUpdate>();

    protected GroupPluginConfigurationUpdate() {
    } // JPA

    public GroupPluginConfigurationUpdate(ResourceGroup group, String subjectName) {
        super(group, subjectName);
        /*
         * this.configuration = groupConfiguration.deepCopy(false);
         * 
         * (ips, 02/13/09): This is a temporary workaround - we don't really need to store a Configuration at all,
         *      since it can be recalculated from the member configs when needed, but the AbstractConfigurationUpdate
         *      base class requires the configuration field to be non-null.
         */
        this.configuration = new Configuration();
    }

    public void setConfigurationUpdates(List<PluginConfigurationUpdate> configurationUpdates) {
        this.configurationUpdates = configurationUpdates;
    }

    public List<PluginConfigurationUpdate> getConfigurationUpdates() {
        return this.configurationUpdates;
    }

    public PluginConfigurationUpdate getPluginConfigurationUpdate(Resource updateTarget) {
        Configuration oldResourcePluginConfiguration = updateTarget.getPluginConfiguration();
        Configuration newResourcePluginConfiguration = getMergedConfiguration(oldResourcePluginConfiguration,
            this.configuration);
        PluginConfigurationUpdate update = new PluginConfigurationUpdate(updateTarget, newResourcePluginConfiguration,
            getSubjectName());
        return update;
    }

    private Configuration getMergedConfiguration(Configuration base, Configuration changes) {
        Configuration results = base.deepCopy(false);

        merge(results, changes);

        return results;
    }

    private void merge(AbstractPropertyMap base, AbstractPropertyMap changes) {
        for (Map.Entry<String, Property> changesEntry : changes.getMap().entrySet()) {
            String changesPropertyName = changesEntry.getKey();
            Property changesProperty = changesEntry.getValue();

            if (changesProperty instanceof PropertySimple) {
                PropertySimple changesPropertySimple = (PropertySimple) changesProperty;

                if ((changesPropertySimple.getOverride() == null) || (!changesPropertySimple.getOverride())) {
                    continue;
                }

                PropertySimple basePropertySimple = (PropertySimple) base.get(changesPropertyName);
                if (basePropertySimple == null) {
                    basePropertySimple = new PropertySimple(changesPropertyName, changesPropertySimple.getStringValue());
                    base.put(basePropertySimple);
                } else {
                    basePropertySimple.setStringValue(changesPropertySimple.getStringValue());
                }
            } else if (changesProperty instanceof PropertyMap) {
                PropertyMap changesPropertyMap = (PropertyMap) changesProperty;
                PropertyMap basePropertyMap = (PropertyMap) base.get(changesPropertyName);
                if (basePropertyMap == null) {
                    basePropertyMap = new PropertyMap(changesPropertyName);
                    base.put(basePropertyMap);
                }
                // Recurse...
                merge(basePropertyMap, changesPropertyMap);
            }
            /*
            else if (changesProperty instanceof PropertyList) {
               throw new UnsupportedOperationException(
                   "PropertyList type not supported for group plugin configuration updates");
            } else {
               throw new UnsupportedOperationException("Property of type '"
                   + changesProperty.this.getClass().getName().substring(this.getClass().getName().lastIndexOf(".")+1) + "' "
                   + "not supported for group plugin configuration updates");
            }
            */
        }
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        //str.append(", pluginConfigurationUpdates=").append(getConfigurationUpdates());
    }
}