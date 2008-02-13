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
package org.rhq.core.domain.configuration.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorValue("plugin")
@Entity
@NamedQueries( {
    @NamedQuery(name = AggregatePluginConfigurationUpdate.QUERY_FIND_BY_GROUP_ID, query = "SELECT apcu "
        + "FROM AggregatePluginConfigurationUpdate AS apcu " + "WHERE apcu.group.id = :groupId"),
    @NamedQuery(name = AggregatePluginConfigurationUpdate.QUERY_DELETE_BY_ID, query = "DELETE "
        + "FROM AggregatePluginConfigurationUpdate AS apcu " + "WHERE apcu.id IN ( :ids ) ") })
public class AggregatePluginConfigurationUpdate extends AbstractAggregateConfigurationUpdate {
    public static final String QUERY_FIND_BY_GROUP_ID = "AggregatePluginConfigurationUpdate.findByGroupId";
    public static final String QUERY_DELETE_BY_ID = "AggregatePluginConfigurationUpdate.deleteById";

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "aggregateConfigurationUpdate", cascade = { CascadeType.ALL })
    private List<PluginConfigurationUpdate> configurationUpdates = new ArrayList<PluginConfigurationUpdate>();

    protected AggregatePluginConfigurationUpdate() {
    } // JPA

    public AggregatePluginConfigurationUpdate(ResourceGroup group, Configuration aggregateConfiguration,
        String subjectName) {
        super(group, aggregateConfiguration, subjectName);
    }

    public void setConfigurationUpdates(List<PluginConfigurationUpdate> configurationUpdates) {
        this.configurationUpdates = configurationUpdates;
    }

    public List<PluginConfigurationUpdate> getConfigurationUpdates() {
        return this.configurationUpdates;
    }

    public void addConfigurationUpdate(Resource updateTarget) {
        Configuration oldResourcePluginConfiguration = updateTarget.getPluginConfiguration();
        Configuration newResourcePluginConfiguration = getMergedConfiguration(oldResourcePluginConfiguration,
            this.configuration);
        PluginConfigurationUpdate update = new PluginConfigurationUpdate(updateTarget, newResourcePluginConfiguration,
            getSubjectName());
        this.configurationUpdates.add(update);
        update.setAggregateConfigurationUpdate(this);
    }

    private Configuration getMergedConfiguration(Configuration base, Configuration changes) {
        Configuration results = base.deepCopy(false);

        merge(results.getMap(), changes.getMap());

        return results;
    }

    private void merge(Map<String, Property> base, Map<String, Property> changes) {
        for (Map.Entry<String, Property> changesEntry : changes.entrySet()) {
            String changesPropertyName = changesEntry.getKey();
            Property changesProperty = changesEntry.getValue();

            if (changesProperty instanceof PropertySimple) {
                PropertySimple changesPropertySimple = (PropertySimple) changesProperty;

                if ((changesPropertySimple.getOverride() == null) || (changesPropertySimple.getOverride() == false)) {
                    continue;
                }

                PropertySimple basePropertySimple = (PropertySimple) base.get(changesPropertyName);
                basePropertySimple.setStringValue(changesPropertySimple.getStringValue());
            } else if (changesProperty instanceof PropertyMap) {
                PropertyMap changesPropertyMap = (PropertyMap) changesProperty;
                PropertyMap basePropertyMap = (PropertyMap) base.get(changesPropertyName);

                merge(basePropertyMap.getMap(), changesPropertyMap.getMap());
            } else if (changesProperty instanceof PropertyList) {
                throw new UnsupportedOperationException(
                    "PropertyList type not supported for aggregate plugin configuration updates");
            } else {
                throw new UnsupportedOperationException("Property of type '"
                    + changesProperty.getClass().getSimpleName() + "' "
                    + "not supported for aggregate plugin configuration updates");
            }
        }
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", pluginConfigurationUpdates=").append(getConfigurationUpdates());
    }
}