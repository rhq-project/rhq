/*
  * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * Convienence class that encapsulates the different configuration properties that a notification
 * needs to support the resource operation notification feature.
 * 
 * @author John Mazzitelli
 */
public class ResourceOperationNotificationInfo {

    static Messages MSG = CoreGUI.getMessages();

    public enum Constants {
        SELECTION_MODE("selection-mode"), // self, specific, relative
        SPECIFIC_RESOURCE_ID("selection-specific-resource-id"), // 
        RELATIVE_ANCESTOR_TYPE_ID("selection-relative-ancestor-type-id"), //
        RELATIVE_DESCENDANT_TYPE_ID("selection-relative-descendant-type-id"), //
        RELATIVE_DESCENDANT_NAME("selection-relative-descendant-name"), //
        OPERATION_ID("operation-definition-id");

        private final String propertyName; // the actual name of the property stored in the config object - must match server-side expectations

        private Constants(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }; // gwt compiler needs this semi

    public enum ResourceSelectionMode {
        SELF, //
        SPECIFIC, //
        RELATIVE;

        public String getDisplayString() {
            switch (ordinal()) {
            case 0: {
                return MSG.view_alert_definition_notification_operation_editor_mode_this();
            }
            case 1: {
                return MSG.view_alert_definition_notification_operation_editor_mode_specific();
            }
            case 2: {
                return MSG.view_alert_definition_notification_operation_editor_mode_relative();
            }
            default: {
                return MSG.view_alert_definition_notification_operation_editor_mode_unknown(); // should never happen
            }
            }
        }
    }; // gwt compiler needs this semi

    private ResourceSelectionMode mode; // will never be null
    private Integer resourceId;
    private Integer ancestorTypeId;
    private Integer descendantTypeId;
    private String descendantName;
    private Integer operationId;
    private Configuration operationArguments; // may be null

    private ResourceOperationNotificationInfo(String mode, String resourceId, String ancestorTypeId,
        String descendantTypeId, String descendantName, String operationId, Configuration opArgs) {

        ResourceSelectionMode selectionMode = null;
        try {
            if (mode != null) {
                selectionMode = ResourceSelectionMode.valueOf(mode);
            }
        } catch (Throwable t) {
        }
        this.mode = selectionMode;
        this.resourceId = getInteger(resourceId);
        this.ancestorTypeId = getInteger(ancestorTypeId);
        this.descendantTypeId = getInteger(descendantTypeId);
        this.descendantName = descendantName;
        this.operationId = getInteger(operationId);
        this.operationArguments = opArgs;
    }

    private Integer getInteger(String data) {
        if (data == null || data.equals("") || data.equals("none")) {
            return null;
        }

        return Integer.parseInt(data);
    }

    public static ResourceOperationNotificationInfo load(Configuration configuration, Configuration extraConfiguration) {
        String mode = get(configuration, Constants.SELECTION_MODE, ResourceSelectionMode.SELF.name());
        String resourceId = get(configuration, Constants.SPECIFIC_RESOURCE_ID, null);
        String ancestorTypeId = get(configuration, Constants.RELATIVE_ANCESTOR_TYPE_ID, null);
        String descendantTypeId = get(configuration, Constants.RELATIVE_DESCENDANT_TYPE_ID, null);
        String descendantName = get(configuration, Constants.RELATIVE_DESCENDANT_NAME, null);
        String operationId = get(configuration, Constants.OPERATION_ID, null);

        return new ResourceOperationNotificationInfo(mode, resourceId, ancestorTypeId, descendantTypeId,
            descendantName, operationId, extraConfiguration);
    }

    private static String get(Configuration configuration, Constants operationInfoConstants, String defaultValue) {
        return configuration.getSimpleValue(operationInfoConstants.propertyName, defaultValue);
    }

    /**
     * Indicates how to interpret the rest of the data in this info object.
     * If this is <code>null</code>, there is no info that can be gleened from the rest of this object's data.
     * 
     * @return the operation selection mode - e.g. self, specific or relative
     */
    public ResourceSelectionMode getMode() {
        return mode;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public Integer getAncestorTypeId() {
        return ancestorTypeId;
    }

    public Integer getDescendantTypeId() {
        return descendantTypeId;
    }

    public String getDescendantName() {
        return descendantName;
    }

    public Integer getOperationId() {
        return operationId;
    }

    public Configuration getOperationArguments() {
        return operationArguments;
    }
}
