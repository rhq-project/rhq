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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.group.ResourceGroup;

@DiscriminatorColumn(name = "DTYPE")
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_GROUP_UPDATE_ID_SEQ")
@Table(name = "RHQ_CONFIG_GROUP_UPDATE")
public abstract class AbstractAggregateConfigurationUpdate extends AbstractConfigurationUpdate implements Serializable {
    @GeneratedValue(generator = "SEQ", strategy = GenerationType.SEQUENCE)
    @Id
    private int id;

    @JoinColumn(name = "GROUP_ID", referencedColumnName = "ID")
    @ManyToOne
    private ResourceGroup group;

    public static final String MIXED_VALUES_MARKER = "~ Mixed Values ~";

    protected AbstractAggregateConfigurationUpdate() {
    }

    public static Configuration getAggregateConfiguration(List<Configuration> elements) {
        Configuration results = null;

        for (Configuration next : elements) {
            if (next == null) {
                throw new IllegalArgumentException(
                    "All elements must be non-null when creating an aggregate configuration");
            }

            if (results == null) {
                results = next.deepCopy(false);
                continue;
            }

            mergeIntoAggregate(results.getMap(), next.getMap());
        }

        return results;
    }

    private static void mergeIntoAggregate(Map<String, Property> aggregate, Map<String, Property> next) {
        for (Map.Entry<String, Property> changesEntry : next.entrySet()) {
            String nextPropertyName = changesEntry.getKey();
            Property nextProperty = changesEntry.getValue();

            if (nextProperty instanceof PropertySimple) {
                PropertySimple changesPropertySimple = (PropertySimple) nextProperty;
                PropertySimple aggregatePropertySimple = (PropertySimple) aggregate.get(nextPropertyName);

                // use MIXED_VALUES_MARKER on the left-hand side because getStringValue() can return null
                if (MIXED_VALUES_MARKER.equals(aggregatePropertySimple.getStringValue())) {
                    continue; // it's already been nulled out, either naturally or via this algo, so continue
                }

                if (aggregatePropertySimple.equals(changesPropertySimple)) {
                    continue; // do nothing, leave equal values set
                }

                aggregatePropertySimple.setValue(MIXED_VALUES_MARKER);
            } else if (nextProperty instanceof PropertyMap) {
                PropertyMap changesPropertyMap = (PropertyMap) nextProperty;
                PropertyMap aggregatePropertyMap = (PropertyMap) aggregate.get(nextPropertyName);

                mergeIntoAggregate(aggregatePropertyMap.getMap(), changesPropertyMap.getMap());
            } else if (nextProperty instanceof PropertyList) {
                throw new UnsupportedOperationException("PropertyList type not supported for aggregate updates");
            } else {
                throw new UnsupportedOperationException("Property of type '" + nextProperty.getClass().getSimpleName()
                    + "' " + "not supported for aggregate updates");
            }
        }
    }

    public AbstractAggregateConfigurationUpdate(ResourceGroup group, Configuration configuration, String subjectName) {
        this.group = group;
        this.subjectName = subjectName;
        this.configuration = configuration.deepCopy(false);

        setStatus(ConfigurationUpdateStatus.INPROGRESS);
        setErrorMessage(null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    @Override
    protected void appendToStringInternals(StringBuilder str) {
        super.appendToStringInternals(str);
        str.append(", id=").append(this.id);
        str.append(", resourceGroup=").append(this.group);
    }
}