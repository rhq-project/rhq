package org.rhq.enterprise.server.plugins.alertOperations;

import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.util.LookupUtil;

public class OperationInfo {
    public enum Constants {
        RESOURCE("resourceId"), //
        OPERATION("operationName"), //
        USABLE("usable"), //
        PARAMETERS_CONFIG("parametersConfig");

        public final String propertyName;

        private Constants(String propertyName) {
            this.propertyName = propertyName;
        }
    }

    public final Integer resourceId;
    public final Integer operationId;
    public final Boolean usable;
    public final Integer argumentsConfigurationId;

    public final String error;

    private OperationInfo(String resourceId, String operationId, String usable, String argumentsConfigurationId) {
        this.resourceId = resourceId == null ? null : Integer.valueOf(resourceId);
        this.operationId = operationId == null ? null : Integer.valueOf(operationId);
        this.usable = usable == null ? null : Boolean.valueOf(usable);
        this.argumentsConfigurationId = this.argumentsConfigurationId == null ? null : Integer
            .valueOf(argumentsConfigurationId);

        this.error = getErrorString(this);
    }

    private String getErrorString(OperationInfo info) {
        if (resourceId == null) {
            return "<no resource selected>";
        }
        if (operationId == null) {
            return "<no operation selected>";
        }
        if (usable == null) {
            return "<not configured yet>";
        }
        return null;
    }

    public static OperationInfo load(Configuration configuration) {
        String resourceId = configuration.getSimpleValue(Constants.RESOURCE.propertyName, null);
        String operationId = configuration.getSimpleValue(Constants.OPERATION.propertyName, null);
        String usable = configuration.getSimpleValue(Constants.USABLE.propertyName, null);
        String argumentsConfigurationId = configuration.getSimpleValue(Constants.PARAMETERS_CONFIG.propertyName, null);

        return new OperationInfo(resourceId, operationId, usable, argumentsConfigurationId);
    }

    public String toString() {
        if (resourceId == null) {
            return "<no resource selected>";
        }
        String lineage = getResourceLineage();

        if (operationId == null) {
            return "<no operation selected> : " + lineage;
        }

        return getOperationDefinition().getName() + ": " + lineage;
    }

    public String getResourceLineage() {
        List<Resource> lineage = LookupUtil.getResourceManager().getResourceLineage(resourceId);
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Resource next : lineage) {
            if (first) {
                first = false;
            } else {
                builder.append(" > ");
            }
            builder.append(next.getName());
        }
        return builder.toString();
    }

    public OperationDefinition getOperationDefinition() {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        OperationDefinition operation = LookupUtil.getOperationManager().getOperationDefinition(overlord, operationId);
        return operation;
    }

    public Configuration getArguments() {
        if (argumentsConfigurationId == null) {
            return null;
        }

        Configuration arguments = LookupUtil.getConfigurationManager().getConfigurationById(argumentsConfigurationId);
        return arguments;
    }
}
