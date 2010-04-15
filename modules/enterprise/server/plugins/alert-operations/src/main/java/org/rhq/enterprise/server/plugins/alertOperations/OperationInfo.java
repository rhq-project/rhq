package org.rhq.enterprise.server.plugins.alertOperations;

import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.util.LookupUtil;

public class OperationInfo {
    public enum Constants {
        SELECTION_MODE("selection-mode"), // self, specific, relative
        SPECIFIC_RESOURCE_ID("selection-specific-resource-id"), // 
        RELATIVE_ANCESTOR_TYPE_ID("selection-relative-descendant-type-id"), //
        RELATIVE_DESCENDANT_TYPE_ID("selection-relative-descendant-name"), //
        RELATIVE_DESCENDANT_NAME("selection-relative-descendant-name"), //
        OPERATION_ID("operation-definition-id"), //
        ARGUMENTS_CONFIG_ID("operation-arguments-configuration-id");

        public final String propertyName;

        private Constants(String propertyName) {
            this.propertyName = propertyName;
        }
    }

    public enum ResourceSelectionMode {
        SELF("This Resource"), //
        SPECIFIC("Specific Resource"), //
        RELATIVE("Relative Resource");

        public final String displayString;

        private ResourceSelectionMode(String displayString) {
            this.displayString = displayString;
        }
    }

    public final ResourceSelectionMode mode;
    public final Integer resourceId;
    public final Integer ancestorTypeId;
    public final Integer descendantTypeId;
    public final String descendantName;
    public final Integer operationId;
    public final Integer argumentsConfigurationId;

    public final String error;

    private Subject overlord;

    private OperationInfo(String mode, String resourceId, String ancestorTypeId, String descendantTypeId,
        String descendantName, String operationId, String argumentsConfigurationId) {
        ResourceSelectionMode selectionMode = null;
        try {
            selectionMode = ResourceSelectionMode.valueOf(mode);
        } catch (Throwable t) {
        }
        this.mode = selectionMode;
        this.resourceId = get(resourceId);
        this.ancestorTypeId = get(ancestorTypeId);
        this.descendantTypeId = get(descendantTypeId);
        this.descendantName = descendantName;
        this.operationId = get(operationId);
        this.argumentsConfigurationId = get(argumentsConfigurationId);

        this.error = getErrorString(this);
    }

    private Integer get(String data) {
        if (data == null || data.equals("") || data.equals("none")) {
            return null;
        }

        return Integer.parseInt(data);
    }

    private String getErrorString(OperationInfo info) {
        if (resourceId == null) {
            return "<no resource selected>";
        }
        if (operationId == null) {
            return "<no operation selected>";
        }
        return null;
    }

    public static OperationInfo load(Configuration configuration) {
        String mode = get(configuration, Constants.SELECTION_MODE, null);
        String resourceId = get(configuration, Constants.SPECIFIC_RESOURCE_ID, null);
        String ancestorTypeId = get(configuration, Constants.RELATIVE_ANCESTOR_TYPE_ID, null);
        String descendantTypeId = get(configuration, Constants.RELATIVE_DESCENDANT_TYPE_ID, null);
        String descendantName = get(configuration, Constants.RELATIVE_DESCENDANT_NAME, null);
        String operationId = get(configuration, Constants.OPERATION_ID, null);
        String argumentsConfigurationId = get(configuration, Constants.ARGUMENTS_CONFIG_ID, null);

        return new OperationInfo(mode, resourceId, ancestorTypeId, descendantTypeId, descendantName, operationId,
            argumentsConfigurationId);
    }

    private static String get(Configuration configuration, Constants operationInfoConstants, String defaultValue) {
        return configuration.getSimpleValue(operationInfoConstants.propertyName, defaultValue);
    }

    public String toString() {
        String resourceInfo = getResourceInfo();

        if (resourceInfo == null) {
            return "<no resource selected>";
        }

        if (operationId == null) {
            return "<no operation selected> for " + resourceInfo;
        }

        return getOperationDefinition().getDisplayName() + " on " + resourceInfo;
    }

    public String getResourceInfo() {
        /*
         * <operation> on this resource (<resource ancestry>)
         * <operation> on other resource (<resource ancestry>)
         * <operation> on { { name } <descendant> } { under <ancestor> }
         */
        if (mode == null) {
            return null;
        } else if (mode == ResourceSelectionMode.SELF) {
            return "this resource";
        } else if (mode == ResourceSelectionMode.SPECIFIC) {
            if (resourceId == null) {
                return null;
            }
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
            return "other resource ( " + builder.toString() + " )";
        } else if (mode == ResourceSelectionMode.RELATIVE) {
            ResourceType ancestor = getType(ancestorTypeId);
            ResourceType descendant = getType(descendantTypeId);

            StringBuilder builder = new StringBuilder();
            if (descendant != null) {
                if (descendantName != null) {
                    builder.append(descendantName); // name only relevant if type is chosen
                }
                builder.append(' ');
                builder.append(descendant.getName());
            }

            if (ancestor != null) {
                builder.append(" under ");
                builder.append(ancestor.getName());
            }

            return builder.toString();
        } else {
            return "<unknown selection mode>";
        }
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

    private Subject getOverlord() {
        if (overlord == null) {
            overlord = LookupUtil.getSubjectManager().getOverlord();
        }
        return overlord;
    }

    private ResourceType getType(Integer typeId) {
        if (typeId == null) {
            return null;
        }

        try {
            return LookupUtil.getResourceTypeManager().getResourceTypeById(getOverlord(), typeId);
        } catch (Throwable t) {
            return null;
        }
    }
}
