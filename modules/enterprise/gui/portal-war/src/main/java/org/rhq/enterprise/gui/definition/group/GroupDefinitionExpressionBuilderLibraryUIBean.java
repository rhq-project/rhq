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
package org.rhq.enterprise.gui.definition.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionExpressionBuilderManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 *
 * @author Joseph Marques
 * @author Greg Hinkle
 */
public class GroupDefinitionExpressionBuilderLibraryUIBean {

    private enum PropertyType {
        RESOURCE("Resource"), //
        RESOURCE_TYPE("Resource Type"), //
        RESOURCE_CATEGORY("Resource Category"), //
        TRAIT("Traits"), // resource.trait[<property_name>]
        PLUGIN_CONFIGURATION("Plugin Configuration"), // resource.pluginConfiguration[<property_name>]
        RESOURCE_CONFIGURATION("Resource Configuration"); // resource.resourceConfiguration[<property_name>]

        private String displayName;

        private PropertyType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static PropertyType getFromDisplayName(String displayName) {
            for (PropertyType type : values()) {
                if (type.getDisplayName().equals(displayName)) {
                    return type;
                }
            }
            return RESOURCE;
        }
    }

    private enum ResourceLevel {
        RESOURCE("Resource"), CHILD("Child"), PARENT("Parent"), GRANDPARENT("Grandparent"), GREATGRANDPARENT(
            "GreatGrandparent"), GREATGREATGRANDPARENT("GreatGreatGrandparent");
        private String displayName;

        private ResourceLevel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ResourceLevel getFromDisplayName(String displayName) {
            for (ResourceLevel type : values()) {
                if (type.getDisplayName().equals(displayName)) {
                    return type;
                }
            }
            return null;
        }
    }

    private enum Comparison {
        EQUALS("="), STARTS_WITH("starts with"), ENDS_WITH("ends with"), CONTAINS("contains");
        private String displayName;

        private Comparison(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Comparison getFromDisplayName(String displayName) {
            for (Comparison type : values()) {
                if (type.getDisplayName().equals(displayName)) {
                    return type;
                }
            }
            return EQUALS;
        }
    }

    /*
     * record the previous values so the next 
     * request can tell which one has changed
     */
    private String previousPropertyType;
    private String previousPlugin;
    private String previousResourceType;

    private String selectedResourceLevel;
    private String selectedPropertyType;
    private String selectedPlugin;
    private String selectedResourceType;
    private String selectedProperty;
    private String selectedComparison;
    private String enteredValue = "";
    private String selectedGroupBy;
    private String selectedUnset;
    private boolean groupby;
    private boolean unset;
    private boolean typeSelectionDisabled;

    private SelectItem[] resourceLevels;
    private SelectItem[] propertyTypes;
    private SelectItem[] plugins;
    private SelectItem[] resourceTypes;
    private SelectItem[] properties;
    private SelectItem[] comparisonTypes;

    public GroupDefinitionExpressionBuilderLibraryUIBean() {
        // setup the PropertyType drop-down
        List<String> types = new ArrayList<String>();
        for (PropertyType type : PropertyType.values()) {
            types.add(type.getDisplayName());
        }
        setSelectedPropertyType(types.get(0));
        propertyTypes = SelectItemUtils.convertFromListString(types, false);

        List<String> resourceLevels = new ArrayList<String>();
        for (ResourceLevel level : ResourceLevel.values()) {
            resourceLevels.add(level.getDisplayName());
        }
        setSelectedResourceLevel(resourceLevels.get(0));
        this.resourceLevels = SelectItemUtils.convertFromListString(resourceLevels, false);

        List<String> comparisonTypes = new ArrayList<String>();
        for (Comparison comparison : Comparison.values()) {
            comparisonTypes.add(comparison.getDisplayName());
        }
        setSelectedComparison(resourceLevels.get(0));
        this.comparisonTypes = SelectItemUtils.convertFromListString(comparisonTypes, false);

        // setup the Plugin drop-down
        List<String> pluginNames = new ArrayList<String>();
        for (Plugin plugin : LookupUtil.getPluginManager().getPlugins()) {
            // TODO: do we want to do this only when plugin.isEnabled() is true?
            pluginNames.add(plugin.getName());
        }
        Collections.sort(pluginNames);
        // processPropertyTypeChange needs some Plugin to be selected
        selectedPlugin = pluginNames.get(0);
        plugins = SelectItemUtils.convertFromListString(pluginNames, false);

        processPluginChange(selectedPlugin);
        processPropertiesForRendering();
    }

    private boolean selectedPropertyTypeChanged() {
        return (changed(previousPropertyType, getSelectedPropertyType()));
    }

    private boolean selectedPluginChanged() {
        return (changed(previousPlugin, getSelectedPlugin()));
    }

    private boolean selectedResourceTypeChanged() {
        return (changed(previousResourceType, getSelectedResourceType()));
    }

    private boolean changed(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return false;
        } else if (s1 == null || s2 == null) {
            return true;
        } else {
            return !s1.equals(s2);
        }
    }

    public String getSelectedPropertyType() {
        return selectedPropertyType;
    }

    public void setSelectedPropertyType(String propertyType) {
        this.selectedPropertyType = propertyType;
    }

    public String getSelectedPlugin() {
        return selectedPlugin;
    }

    public void setSelectedPlugin(String plugin) {
        this.selectedPlugin = plugin;
    }

    public String getSelectedResourceType() {
        return selectedResourceType;
    }

    public void setSelectedResourceType(String resourceType) {
        this.selectedResourceType = resourceType;
    }

    public String getSelectedResourceLevel() {
        return selectedResourceLevel;
    }

    public void setSelectedResourceLevel(String selectedResourceLevel) {
        this.selectedResourceLevel = selectedResourceLevel;
    }

    public String getSelectedProperty() {
        return selectedProperty;
    }

    public void setSelectedProperty(String selectedProperty) {
        this.selectedProperty = selectedProperty;
    }

    public String getSelectedComparison() {
        return selectedComparison;
    }

    public String getSelectedGroupBy() {
        return selectedGroupBy;
    }

    public void setSelectedGroupBy(String selectedGroupBy) {
        this.selectedGroupBy = selectedGroupBy;
    }

    public String getSelectedUnset() {
        return selectedUnset;
    }

    public void setSelectedUnset(String selectedUnset) {
        this.selectedUnset = selectedUnset;
    }

    public void setSelectedComparison(String selectedComparison) {
        this.selectedComparison = selectedComparison;
    }

    public String getEnteredValue() {
        return enteredValue;
    }

    public void setEnteredValue(String enteredValue) {
        this.enteredValue = enteredValue;
    }

    public boolean isGroupby() {
        return groupby;
    }

    public void setGroupby(boolean groupby) {
        this.groupby = groupby;
    }

    public boolean isUnset() {
        return unset;
    }

    public void setUnset(boolean unset) {
        this.unset = unset;
    }

    public SelectItem[] getPropertyTypes() {
        return propertyTypes;
    }

    public SelectItem[] getPlugins() {
        return plugins;
    }

    public SelectItem[] getResourceTypes() {
        return resourceTypes;
    }

    public SelectItem[] getProperties() {

        return properties;
    }

    public SelectItem[] getResourceLevels() {
        return resourceLevels;
    }

    public SelectItem[] getComparisonTypes() {
        return comparisonTypes;
    }

    public boolean isTypeSelectionDisabled() {
        this.typeSelectionDisabled = PropertyType.getFromDisplayName(this.selectedPropertyType) == PropertyType.RESOURCE
            || (isGroupby() && PropertyType.getFromDisplayName(this.selectedPropertyType) == PropertyType.RESOURCE_TYPE)
            || PropertyType.getFromDisplayName(this.selectedPropertyType) == PropertyType.RESOURCE_CATEGORY;
        return typeSelectionDisabled;
    }

    private int getResourceTypeId(String resourceTypeName, String pluginName) {
        try {
            return LookupUtil.getResourceTypeManager().getResourceTypeByNameAndPlugin(resourceTypeName, pluginName)
                .getId();
        } catch (Exception e) {
            return 0;
        }
    }

    public String refreshData() {
        String requestParamPropertyType = FacesContextUtility.getOptionalRequestParameter("libraryForm:propertyType",
            String.class, this.selectedPropertyType);
        String requestParamPlugin = FacesContextUtility.getOptionalRequestParameter("libraryForm:plugin", String.class,
            this.selectedPlugin);
        String requestParamResourceType = FacesContextUtility.getOptionalRequestParameter("libraryForm:resourceType",
            String.class, this.selectedResourceType);

        this.selectedResourceLevel = FacesContextUtility.getOptionalRequestParameter("libraryForm:resourceLevel",
            String.class, "Resource");
        this.selectedProperty = FacesContextUtility.getOptionalRequestParameter("libraryForm:property");
        this.selectedGroupBy = FacesContextUtility.getOptionalRequestParameter("libraryForm:selectedGroupBy");
        this.selectedUnset = FacesContextUtility.getOptionalRequestParameter("libraryForm:selectedUnset");
        this.groupby = Boolean.valueOf(this.selectedGroupBy);
        this.unset = Boolean.valueOf(this.selectedUnset);
        this.enteredValue = FacesContextUtility.getOptionalRequestParameter("libraryForm:value", String.class, "");
        this.selectedComparison = FacesContextUtility.getOptionalRequestParameter("libraryForm:comparison");

        processPropertyTypeChange(requestParamPropertyType);
        processResourceTypeChange(requestParamResourceType);
        if (requestParamPlugin != null && !requestParamPlugin.equals(this.selectedPlugin)) {
            processPluginChange(requestParamPlugin);
        }

        this.typeSelectionDisabled = PropertyType.getFromDisplayName(this.selectedPropertyType) == PropertyType.RESOURCE
            || PropertyType.getFromDisplayName(this.selectedPropertyType) == PropertyType.RESOURCE_CATEGORY;

        /*
         * the other process methods have already reset / updated the other drop-downs as necessary,
         * so the only things left to do is to re-render the Properties drop-down for display.
         */
        processPropertiesForRendering();

        return "success";
    }

    private void processPropertyTypeChange(String newPropertyType) {
        /*
         * if the PropertyType changes, the only other element that needs updating
         * is the Properties drop-down.  The Plugin / ResourceType drop-down can
         * remain where they are.
         */
        selectedPropertyType = newPropertyType;
    }

    private void processPluginChange(String newPlugin) {
        selectedPlugin = newPlugin;

        try {
            Plugin plugin = LookupUtil.getPluginManager().getPlugin(selectedPlugin);
            List<ResourceType> types = LookupUtil.getResourceTypeManager().getResourceTypesByPlugin(plugin.getName());

            List<String> typeNames = new ArrayList<String>();
            for (ResourceType type : types) {
                typeNames.add(type.getName());
            }

            Collections.sort(typeNames);

            if (typeNames.size() > 0) {
                selectedResourceType = typeNames.get(0);
            } else {
                selectedResourceType = null;
            }

            if (typeNames.size() == 0) {
                resourceTypes = new SelectItem[0];
            } else {
                resourceTypes = SelectItemUtils.convertFromListString(typeNames, false);
            }
        } catch (Exception e) {
        }
    }

    private void processResourceTypeChange(String newResourceType) {
        selectedResourceType = newResourceType;
    }

    private void processPropertiesForRendering() {
        previousPropertyType = selectedPropertyType;
        previousPlugin = selectedPlugin;
        previousResourceType = selectedResourceType;

        /*
         * even though the drop-down boxes are being updated as necessary according to their
         * logical dependency graph, it still may be the case that there are no ResourceTypes
         * for the given PropertyType/Plugin drop-down combo.  In this case, make sure we 
         * return a 0-sized array so that UI renders properly.
         */
        if (resourceTypes.length == 0) {
            properties = new SelectItem[0];
            return;
        }

        List<String> propertyNames = new ArrayList<String>();

        PropertyType type = PropertyType.getFromDisplayName(selectedPropertyType);
        String resourceType = selectedResourceType;
        String plugin = selectedPlugin;
        if (resourceType != null && plugin != null) {
            int resourceTypeId = getResourceTypeId(resourceType, plugin);

            GroupDefinitionExpressionBuilderManagerLocal expressionBuilderManager = null;
            expressionBuilderManager = LookupUtil.getGroupDefinitionExpressionBuilderManager();

            if (type == null) {
                throw new IllegalArgumentException("No property support for '" + selectedPropertyType + " yet.");
            } else if (type == PropertyType.TRAIT) {
                propertyNames = expressionBuilderManager.getTraitPropertyNames(resourceTypeId);
            } else if (type == PropertyType.PLUGIN_CONFIGURATION) {
                propertyNames = expressionBuilderManager.getPluginConfigurationPropertyNames(resourceTypeId);
            } else if (type == PropertyType.RESOURCE_CONFIGURATION) {
                propertyNames = expressionBuilderManager.getResourceConfigurationPropertyNames(resourceTypeId);
            } else if (type == PropertyType.RESOURCE) {
                propertyNames = Arrays.asList(new String[] { "id", "name", "version", "availability" });
            }

            if (propertyNames.size() == 0) {
                properties = new SelectItem[0];
            } else {
                properties = SelectItemUtils.convertFromListString(propertyNames, false);
                if (this.selectedProperty == null || !propertyNames.contains(this.selectedProperty)) {
                    this.selectedProperty = propertyNames.get(0);
                }
            }
        }
    }

    public String getExpression() {

        refreshData();

        StringBuilder buf = new StringBuilder();
        if (this.isGroupby()) {
            buf.append("groupby ");
        }
        if (this.isUnset()) {
            buf.append("empty ");
        }

        buf.append("resource.");

        switch (ResourceLevel.getFromDisplayName(this.selectedResourceLevel)) {
        case RESOURCE:
            break;
        case CHILD:
            buf.append("child.");
            break;
        case PARENT:
            buf.append("parent.");
            break;
        case GRANDPARENT:
            buf.append("grandParent.");
            break;
        case GREATGRANDPARENT:
            buf.append("greatGrandParent.");
            break;
        case GREATGREATGRANDPARENT:
            buf.append("greatGreatGrandParent.");
            break;
        }

        switch (PropertyType.getFromDisplayName(this.selectedPropertyType)) {
        case RESOURCE:
            buf.append(this.selectedProperty);
            break;
        case RESOURCE_TYPE:
            buf.append("type.plugin");
            break;
        case RESOURCE_CATEGORY:
            buf.append("type.category");
            break;
        case TRAIT:
            buf.append("trait[" + this.selectedProperty + "]");
            break;
        case PLUGIN_CONFIGURATION:
            buf.append("pluginConfiguration[" + this.selectedProperty + "]");
            break;
        case RESOURCE_CONFIGURATION:
            buf.append("resourceConfiguration[" + this.selectedProperty + "]");
            break;
        }

        if (!groupby && !unset) {

            switch (Comparison.getFromDisplayName(this.selectedComparison)) {
            case EQUALS:
                break;
            case CONTAINS:
                buf.append(".contains");
                break;
            case STARTS_WITH:
                buf.append(".startsWith");
                break;
            case ENDS_WITH:
                buf.append(".endsWith");
                break;
            }

            buf.append(" = ");

            switch (PropertyType.getFromDisplayName(this.selectedPropertyType)) {
            case RESOURCE:
            case RESOURCE_CATEGORY:
                buf.append(this.enteredValue);
                break;
            case RESOURCE_TYPE:
                String d = buf.toString();
                buf.append(this.selectedPlugin);
                buf.append("\n");
                buf.append(d.replaceAll("plugin", "name"));
                buf.append(this.selectedResourceType);
                break;
            case TRAIT:
            case PLUGIN_CONFIGURATION:
            case RESOURCE_CONFIGURATION:
                buf.append(enteredValue);
                break;
            }
        } else if (PropertyType.getFromDisplayName(this.selectedPropertyType) == PropertyType.RESOURCE_TYPE) {
            String d = buf.toString();
            buf.append("\n");
            buf.append(d.replaceAll("plugin", "name"));
        }
        return buf.toString();
    }
}
