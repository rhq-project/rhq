package org.rhq.enterprise.gui.definition.group;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionExpressionBuilderManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class GroupDefinitionExpressionBuilderLibraryUIBean {
    private enum PropertyType {
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
            return null;
        }
    }

    /*
     * record the previous values so the next 
     * request can tell which one has changed
     */
    private String previousPropertyType;
    private String previousPlugin;
    private String previousResourceType;

    private String selectedPropertyType;
    private String selectedPlugin;
    private String selectedResourceType;

    private SelectItem[] propertyTypes;
    private SelectItem[] plugins;
    private SelectItem[] resourceTypes;
    private SelectItem[] properties;

    public GroupDefinitionExpressionBuilderLibraryUIBean() {
        // setup the PropertyType drop-down
        List<String> types = new ArrayList<String>();
        for (PropertyType type : PropertyType.values()) {
            types.add(type.getDisplayName());
        }
        setSelectedPropertyType(types.get(0));
        propertyTypes = SelectItemUtils.convertFromListString(types, false);

        // setup the Plugin drop-down
        List<String> pluginNames = new ArrayList<String>();
        for (Plugin plugin : LookupUtil.getResourceMetadataManager().getPlugins()) {
            pluginNames.add(plugin.getName());
        }
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

    private int getResourceTypeId(String resourceTypeName, String pluginName) {
        return LookupUtil.getResourceTypeManager().getResourceTypeByNameAndPlugin(resourceTypeName, pluginName).getId();
    }

    public String refreshData() {
        String requestParamPropertyType = FacesContextUtility.getRequiredRequestParameter("libraryForm:propertyType");
        String requestParamPlugin = FacesContextUtility.getRequiredRequestParameter("libraryForm:plugin");
        String requestParamResourceType = FacesContextUtility.getOptionalRequestParameter("libraryForm:resourceType");

        if (selectedPropertyTypeChanged()) {
            processPropertyTypeChange(requestParamPropertyType);
        } else if (selectedPluginChanged()) {
            processPluginChange(requestParamPlugin);
        } else if (selectedResourceTypeChanged()) {
            // check if selectedResourceType was null, if it was we can't have a change event on it
            processResourceTypeChange(requestParamResourceType);
        } else {
            // user submitted the page with no change, just redisplay data already in this UIBean
        }

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

        Plugin plugin = LookupUtil.getResourceMetadataManager().getPlugin(selectedPlugin);
        List<ResourceType> types = LookupUtil.getResourceTypeManager().getResourceTypesByPlugin(plugin.getName());

        List<String> typeNames = new ArrayList<String>();
        for (ResourceType type : types) {
            typeNames.add(type.getName());
        }
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
        }

        if (propertyNames.size() == 0) {
            properties = new SelectItem[0];
        } else {
            properties = SelectItemUtils.convertFromListString(propertyNames, false);
        }
    }

}
