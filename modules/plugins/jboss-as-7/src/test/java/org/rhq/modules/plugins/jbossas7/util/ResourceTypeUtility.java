package org.rhq.modules.plugins.jbossas7.util;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * TODO: Move these method to a utils module.
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
