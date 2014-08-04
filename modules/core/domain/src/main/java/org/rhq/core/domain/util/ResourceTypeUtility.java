/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A set of utility methods for working with {@link ResourceType}s.
 *
 * @since 4.4
 * @author Ian Springer
 */
public abstract class ResourceTypeUtility {

    /**
     * Returns the set of MeasurementDefinitions defined by the given Resource type, which are accepted by the specified
     * filter. If the filter is null, all of the type's MeasurementDefinitions will be returned.
     *
     * @param type the Resource type
     * @param filter the filter; may be null
     *
     * @return the set of MeasurementDefinition defined by the given Resource type, which are accepted by the specified
     *         filter, or, if the filter is null, all of the type's MeasurementDefinitions
     */
    public static Set<MeasurementDefinition> getMeasurementDefinitions(ResourceType type,
                                                                       MeasurementDefinitionFilter filter) {        
        if (filter == null) {
            return type.getMetricDefinitions();
        }
        Set<MeasurementDefinition> acceptedMetricDefs = new LinkedHashSet<MeasurementDefinition>();
        for (MeasurementDefinition metricDef : type.getMetricDefinitions()) {
            if (filter.accept(metricDef)) {
                acceptedMetricDefs.add(metricDef);
            }
        }
        return acceptedMetricDefs;
    }
    
    public static MeasurementDefinition getMeasurementDefinition(ResourceType type, String metricName) {
        for (MeasurementDefinition metricDefinition : type.getMetricDefinitions()) {
            if (metricDefinition.getName().equals(metricName)) {
                return metricDefinition;
            }
        }
        return null;
    }

    public static OperationDefinition getOperationDefinition(ResourceType type, String operationName) {
        for (OperationDefinition operationDefinition : type.getOperationDefinitions()) {
            if (operationDefinition.getName().equals(operationName)) {
                return operationDefinition;
            }
        }
        return null;
    }

    public static String displayName(ResourceType resourceType) {
        if (resourceType == null)
            return null;
        return resourceType.getDisplayName() == null ? resourceType.getName() : resourceType.getDisplayName();
    }

    private ResourceTypeUtility() {
    }

}
