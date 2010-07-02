package org.rhq.enterprise.server.plugins.alertOperations;

import java.util.List;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
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
        RELATIVE_ANCESTOR_TYPE_ID("selection-relative-ancestor-type-id"), //
        RELATIVE_DESCENDANT_TYPE_ID("selection-relative-descendant-type-id"), //
        RELATIVE_DESCENDANT_NAME("selection-relative-descendant-name"), //
        OPERATION_ID("operation-definition-id");

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
    public final Configuration arguments;;

    public final String error;

    private Subject overlord;

    private OperationInfo(String mode, String resourceId, String ancestorTypeId, String descendantTypeId,
        String descendantName, String operationId, Configuration arguments) {
        ResourceSelectionMode selectionMode = null;
        try {
            if (mode != null) {
                selectionMode = ResourceSelectionMode.valueOf(mode);
            }
        } catch (Throwable t) {
        }
        this.mode = selectionMode;
        this.resourceId = get(resourceId);
        this.ancestorTypeId = get(ancestorTypeId);
        this.descendantTypeId = get(descendantTypeId);
        this.descendantName = descendantName;
        this.operationId = get(operationId);
        this.arguments = arguments;

        this.error = getErrorString();
    }

    private Integer get(String data) {
        if (data == null || data.equals("") || data.equals("none")) {
            return null;
        }

        return Integer.parseInt(data);
    }

    private String getErrorString() {
        if (mode == ResourceSelectionMode.RELATIVE) {
            if (ancestorTypeId == null) {
                return "<no 'start search from' selected>";
            }
            // if (descendantTypeId == null) wants to execute operation on direct ancestor
        } else if (mode == ResourceSelectionMode.SPECIFIC) {
            if (resourceId == null) {
                return "<no resource selected>";
            }
        }

        if (operationId == null) {
            return "<no operation selected>";
        }

        return null;
    }

    public static OperationInfo load(Configuration configuration, Configuration extraConfiguration) {
        String mode = get(configuration, Constants.SELECTION_MODE, null);
        String resourceId = get(configuration, Constants.SPECIFIC_RESOURCE_ID, null);
        String ancestorTypeId = get(configuration, Constants.RELATIVE_ANCESTOR_TYPE_ID, null);
        String descendantTypeId = get(configuration, Constants.RELATIVE_DESCENDANT_TYPE_ID, null);
        String descendantName = get(configuration, Constants.RELATIVE_DESCENDANT_NAME, null);
        String operationId = get(configuration, Constants.OPERATION_ID, null);

        return new OperationInfo(mode, resourceId, ancestorTypeId, descendantTypeId, descendantName, operationId,
            extraConfiguration);
    }

    private static String get(Configuration configuration, Constants operationInfoConstants, String defaultValue) {
        return configuration.getSimpleValue(operationInfoConstants.propertyName, defaultValue);
    }

    public String toString() {
        String errorInfo = getErrorString();
        if (errorInfo != null) {
            return errorInfo;
        }

        String resourceInfo = getResourceInfo();
        OperationDefinition operation = getOperationDefinition();
        return "'" + operation.getDisplayName() + "' on " + resourceInfo;
    }

    public String getResourceInfo() {
        /*
         * 1) "on" this resource 
         * 2) "on" the ( <resource ancestry> ) resource 
         * 3a) "on" the <ancestorType> ancestor
         * 3b) "on" the '{name}' <descendantType> descendant 
         * 3c) "on" the '{name}' <descendantType> descendant under the <ancestorType> ancestor
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
            return "the ( " + builder.toString() + " ) resource";
        } else if (mode == ResourceSelectionMode.RELATIVE) {
            ResourceType ancestor = getType(ancestorTypeId);
            ResourceType descendant = getType(descendantTypeId);

            StringBuilder builder = new StringBuilder();
            builder.append(" the ");
            if (descendant != null) {
                if (descendantName != null) {
                    builder.append('\'');
                    builder.append(descendantName); // name only relevant if type is chosen
                    builder.append('\'');
                }
                builder.append(' ');
                builder.append(descendant.getName());
                builder.append(" descendant");

                if (ancestor != null) {
                    builder.append(" under the ");
                }
            }

            if (ancestor != null) {
                builder.append(ancestor.getName());
                builder.append(" ancestor");
            }

            return builder.toString();
        } else {
            return "<unknown selection mode>";
        }
    }

    public OperationDefinition getOperationDefinition() {
        OperationDefinition operation = LookupUtil.getOperationManager().getOperationDefinition(getOverlord(),
            operationId);
        return operation;
    }

    public Configuration getArguments() {
        return arguments;
    }

    public Resource getTargetResource(Alert alert) {
        if (mode == null) {
            return null;
        } else if (mode == ResourceSelectionMode.SELF) {
            AlertDefinition definition = alert.getAlertDefinition();
            Resource contextResource = definition.getResource();
            return contextResource;
        } else if (mode == ResourceSelectionMode.SPECIFIC) {
            return LookupUtil.getResourceManager().getResourceById(getOverlord(), resourceId);
        } else if (mode == ResourceSelectionMode.RELATIVE) {
            AlertDefinition definition = alert.getAlertDefinition();
            Resource contextResource = definition.getResource();

            Resource searchFrom = contextResource;
            if (ancestorTypeId != null) {
                List<Resource> contextLineage = LookupUtil.getResourceManager().getResourceLineage(
                    contextResource.getId());
                for (Resource nextResource : contextLineage) {
                    if (nextResource.getResourceType().getId() == ancestorTypeId) {
                        searchFrom = nextResource;
                        break;
                    }
                }
            }

            Resource targetResource = null;
            if (descendantTypeId != null) {
                List<Integer> candidateResourceIds = LookupUtil.getResourceManager()
                    .getResourceDescendantsByTypeAndName(getOverlord(), searchFrom.getId(), descendantTypeId,
                        descendantName);
                if (candidateResourceIds.size() == 0) {
                    throw new IllegalStateException("Could not find target resource");
                } else if (candidateResourceIds.size() != 1) {
                    throw new IllegalStateException("Found multiple resources, need exactly one match");
                } else {
                    int targetResourceId = candidateResourceIds.get(0);
                    targetResource = LookupUtil.getResourceManager().getResourceById(getOverlord(), targetResourceId);
                }
            } else {
                targetResource = searchFrom;
            }
            return targetResource;
        } else {
            return null;
        }
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
