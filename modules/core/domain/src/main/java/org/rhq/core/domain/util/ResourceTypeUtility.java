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

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A set of utility methods for working with {@link ResourceType}s.
 *
 * @author Ian Springer
 */
public abstract class ResourceTypeUtility {

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

    private ResourceTypeUtility() {
    }

}
