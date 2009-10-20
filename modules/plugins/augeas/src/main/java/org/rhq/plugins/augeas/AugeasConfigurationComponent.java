/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.augeas;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.augeas.Augeas;
import net.augeas.AugeasException;
import net.augeas.jna.Aug;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * @author Ian Springer
 */
public class AugeasConfigurationComponent implements ResourceComponent<PlatformComponent>, ConfigurationFacet {
    public static final String CONFIGURATION_FILE_PROP = "configurationFile";

    private static final boolean IS_WINDOWS = (File.separatorChar == '\\');
    private static final String AUGEAS_LOAD_PATH = "/usr/local/share/augeas/lenses";
    private static final String AUGEAS_ROOT_PATH = "/";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private File configFile;
    private Augeas augeas;
    private AugeasNode augeasConfigFileNode;
    private AugeasNode augeasConfigFileMetadataNode;

    public void start(ResourceContext<PlatformComponent> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        this.resourceContext = resourceContext;
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String configFilePath = pluginConfig.getSimple(CONFIGURATION_FILE_PROP).getStringValue();
        this.configFile = new File(configFilePath);
        this.augeas = createAugeas();
        if (this.augeas != null) {
            this.augeasConfigFileNode = new AugeasNode("/files" + AugeasNode.SEPARATOR_CHAR + this.configFile.getPath());
            this.augeasConfigFileMetadataNode = new AugeasNode("/augeas/files" + AugeasNode.SEPARATOR_CHAR
                + this.configFile.getPath());
        }
    }

    public void stop() {
        this.augeas.close();
    }

    public AvailabilityType getAvailability() {
        if (!this.configFile.isAbsolute()) {
            throw new InvalidPluginConfigurationException("Location specified by '" + CONFIGURATION_FILE_PROP
                + "' connection property is not an absolute path.");
        }
        if (!this.configFile.exists()) {
            throw new InvalidPluginConfigurationException("Location specified by '" + CONFIGURATION_FILE_PROP
                + "' connection property does not exist.");
        }
        if (this.configFile.isDirectory()) {
            throw new InvalidPluginConfigurationException("Location specified by '" + CONFIGURATION_FILE_PROP
                + "' connection property is a directory, not a regular file.");
        }
        return this.configFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        // Load the config file from disk and build a tree representation of it.
        this.augeas.load();

        ConfigurationDefinition resourceConfigDef = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();
        Configuration resourceConfig = new Configuration();
        resourceConfig.setNotes("Loaded from Augeas at " + new Date());

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

        for (PropertyDefinition propDef : propDefs) {
            loadProperty(propDef, resourceConfig, this.augeas, this.augeasConfigFileNode);
        }

        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        if (!validateResourceConfiguration(report)) {
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            return;
        }

        if (!this.configFile.canWrite()) {
            report.setErrorMessage("Configuration file '" + this.configFile + "' is not writeable.");
            return;
        }

        // Load the config file from disk and build a tree representation of it.
        this.augeas.load();

        ConfigurationDefinition resourceConfigDef = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();
        Configuration resourceConfig = report.getConfiguration();

        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
        for (PropertyDefinition propDef : propDefs) {
            setNode(propDef, resourceConfig, this.augeas, this.augeasConfigFileNode);
        }

        // Write the updated tree out to the config file.
        saveConfigurationFile();

        // If we got this far, we've succeeded in our mission.
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
    }

    /**
     * Subclasses should override this method in order to perform any validation that is not encapsulated
     * in the Configuration metadata.
     *
     * @param report the report to which any validation errors should be added
     *
     * @return true if the Configuration is valid, or false if it is not
     */
    protected boolean validateResourceConfiguration(ConfigurationUpdateReport report) {
        return true;
    }

    protected AugeasNode getExistingChildNodeForListMemberPropertyMap(AugeasNode parentNode,
        PropertyDefinitionList propDefList, PropertyMap propMap) {
        String mapKey = getListMemberMapKey(propDefList);
        if (mapKey != null) {
            String existingChildNodeName = propMap.getSimple(mapKey).getStringValue();
            AugeasNode existingChildNode = new AugeasNode(parentNode, existingChildNodeName);
            return (this.augeas.exists(existingChildNode.getPath())) ? existingChildNode : null;
        } else {
            return null;
        }
    }

    public ResourceContext getResourceContext() {
        return resourceContext;
    }

    public File getConfigurationFile() {
        return this.configFile;
    }

    public Augeas getAugeas() {
        return this.augeas;
    }

    private Augeas createAugeas() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String augeasModuleName = pluginConfig.getSimpleValue("augeasModuleName", null);
        if (augeasModuleName == null) {
            return null;
        }

        if (!initAugeasJnaProxy()) {
            return null;
        }

        Augeas augeas;
        try {
            augeas = new Augeas(AUGEAS_ROOT_PATH, AUGEAS_LOAD_PATH, Augeas.NO_MODL_AUTOLOAD);
            augeas.set("/augeas/load/" + augeasModuleName + "/lens", augeasModuleName + ".lns");
            augeas.set("/augeas/load/" + augeasModuleName + "/incl", this.configFile.getPath());
        } catch (RuntimeException e) {
            augeas = null;
            log.warn("Failed to initialize Augeas Java API.", e);
        }
        return augeas;
    }

    private boolean initAugeasJnaProxy() {
        Aug aug;
        try {
            aug = Aug.INSTANCE;
        } catch (NoClassDefFoundError e) {
            if (!IS_WINDOWS) {
                log.warn("Augeas shared library not found. If on Fedora or RHEL, yum install augeas.");
            }
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace("Aug JNA object: " + aug);
        }
        return true;
    }

    private void loadProperty(PropertyDefinition propDef, AbstractPropertyMap parentPropMap, Augeas augeas,
        AugeasNode parentNode) {
        String propName = propDef.getName();
        AugeasNode node = (propName.equals(".")) ? parentNode : new AugeasNode(parentNode, propName);
        Property prop;
        if (propDef instanceof PropertyDefinitionSimple) {
            prop = createPropertySimple((PropertyDefinitionSimple) propDef, augeas, node);
        } else if (propDef instanceof PropertyDefinitionMap) {
            prop = createPropertyMap((PropertyDefinitionMap) propDef, augeas, node);
        } else if (propDef instanceof PropertyDefinitionList) {
            prop = createPropertyList((PropertyDefinitionList) propDef, augeas, node);
        } else {
            throw new IllegalStateException("Unsupported PropertyDefinition subclass: " + propDef.getClass().getName());
        }
        parentPropMap.put(prop);
    }

    private Property createPropertySimple(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        String value;
        if (propDefSimple.getType() == PropertySimpleType.LONG_STRING) {
            List<String> childPaths = augeas.match(node.getPath());
            if (childPaths.isEmpty()) {
                return null;
            }
            StringBuilder propValue = new StringBuilder();
            for (String childPath : childPaths) {
                String childValue = augeas.get(childPath);
                propValue.append(childValue).append("\n");
            }
            // Chop the final newline char.
            propValue.deleteCharAt(propValue.length() - 1);
            value = propValue.toString();
        } else {
            value = augeas.get(node.getPath());
        }
        return new PropertySimple(propDefSimple.getName(), value);
    }

    private PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, Augeas augeas, AugeasNode node) {
        PropertyMap propMap = new PropertyMap(propDefMap.getName());
        populatePropertyMap(propDefMap, propMap, augeas, node);
        return propMap;
    }

    private Property createPropertyList(PropertyDefinitionList propDefList, Augeas augeas, AugeasNode node) {
        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();
        if (!(listMemberPropDef instanceof PropertyDefinitionMap)) {
            throw new IllegalArgumentException(
                "Invalid Resource ConfigurationDefinition - only lists of maps are supported.");
        }
        PropertyDefinitionMap listMemberPropDefMap = (PropertyDefinitionMap) listMemberPropDef;

        PropertyList propList = new PropertyList(propDefList.getName());

        String mapKey = getListMemberMapKey(propDefList);
        List<String> listMemberPaths = augeas.match(node.getPath());
        for (String listMemberPath : listMemberPaths) {
            AugeasNode listMemberNode = new AugeasNode(listMemberPath);

            PropertyMap listMemberPropMap = new PropertyMap(listMemberPropDefMap.getName());
            propList.add(listMemberPropMap);

            // Add the "key" prop, if defined, to the map.
            if (mapKey != null) {
                PropertySimple keyProp = new PropertySimple(mapKey, listMemberNode.getName());
                listMemberPropMap.put(keyProp);
            }
            // Populate the rest of the map child properties.
            populatePropertyMap(listMemberPropDefMap, listMemberPropMap, augeas, listMemberNode);
        }

        return propList;
    }

    private void populatePropertyMap(PropertyDefinitionMap propDefMap, PropertyMap propMap, Augeas augeas,
        AugeasNode mapNode) {
        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
            loadProperty(mapEntryPropDef, propMap, augeas, mapNode);
        }
    }

    private void setNode(PropertyDefinition propDef, AbstractPropertyMap parentPropMap, Augeas augeas,
        AugeasNode parentNode) {
        String propName = propDef.getName();
        AugeasNode node = (propName.equals(".")) ? parentNode : new AugeasNode(parentNode, propName);

        if (isPropertyDefined(propDef, parentPropMap)) {
            // The property *is* defined, which means we either need to add or update the corresponding node in the
            // Augeas tree.
            if (propDef instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple propDefSimple = (PropertyDefinitionSimple) propDef;
                PropertySimple propSimple = parentPropMap.getSimple(propDefSimple.getName());
                setNodeFromPropertySimple(augeas, node, propDefSimple, propSimple);
            } else if (propDef instanceof PropertyDefinitionMap) {
                PropertyDefinitionMap propDefMap = (PropertyDefinitionMap) propDef;
                PropertyMap propMap = parentPropMap.getMap(propDefMap.getName());
                setNodeFromPropertyMap(propDefMap, propMap, augeas, node);
            } else if (propDef instanceof PropertyDefinitionList) {
                PropertyDefinitionList propDefList = (PropertyDefinitionList) propDef;
                PropertyList propList = parentPropMap.getList(propDefList.getName());
                setNodeFromPropertyList(propDefList, propList, augeas, node);
            } else {
                throw new IllegalStateException("Unsupported PropertyDefinition subclass: "
                    + propDef.getClass().getName());
            }
        } else {
            // The property *is not* defined - remove the corresponding node from the Augeas tree if it exists.
            removeNodeIfItExists(augeas, node);
        }
    }

    private void setNodeFromPropertySimple(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        String value = propSimple.getStringValue();
        if (propDefSimple.getType() == PropertySimpleType.LONG_STRING) {
            // First remove the existing items.
            List<String> childPaths = augeas.match(node.getPath());
            for (String childPath : childPaths) {
                augeas.remove(childPath);
            }

            // Now add the updated items.
            String[] tokens = value.trim().split("\\s+");
            for (int i = 0, tokensLength = tokens.length; i < tokensLength; i++) {
                String itemPath = node.getPath() + "[" + (i + 1) + "]";
                String itemValue = tokens[i];
                augeas.set(itemPath, itemValue);
            }
        } else {
            // Update the value of the existing node.
            augeas.set(node.getPath(), value);
        }
    }

    private void setNodeFromPropertyMap(PropertyDefinitionMap propDefMap, PropertyMap propMap, Augeas augeas,
        AugeasNode mapNode) {
        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
            setNode(mapEntryPropDef, propMap, augeas, mapNode);
        }
    }

    private void setNodeFromPropertyList(PropertyDefinitionList propDefList, PropertyList propList, Augeas augeas,
        AugeasNode listNode) {
        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();
        if (!(listMemberPropDef instanceof PropertyDefinitionMap)) {
            throw new IllegalArgumentException(
                "Invalid Resource ConfigurationDefinition - only lists of maps are supported.");
        }
        PropertyDefinitionMap listMemberPropDefMap = (PropertyDefinitionMap) listMemberPropDef;
        String mapKey = getListMemberMapKey(propDefList);

        Set<String> keys = new HashSet<String>();
        int listIndex = 0;
        Set<AugeasNode> updatedMemberNodes = new HashSet<AugeasNode>();
        for (Property listMemberProp : propList.getList()) {
            PropertyMap listMemberPropMap = (PropertyMap) listMemberProp;
            AugeasNode memberNodeToUpdate = getExistingChildNodeForListMemberPropertyMap(listNode, propDefList,
                listMemberPropMap);
            if (memberNodeToUpdate != null) {
                // Keep track of the existing nodes that we'll be updating, so that we can remove all other existing
                // nodes.
                updatedMemberNodes.add(memberNodeToUpdate);
            } else {
                // The maps in the list are non-keyed, or there is no map in the list with the same key as the map
                // being added, so create a new node for the map to add to the list.
                memberNodeToUpdate = new AugeasNode(listNode, "0" + listIndex);
            }

            // Update the node's children.
            setNodeFromPropertyMap(listMemberPropDefMap, listMemberPropMap, augeas, memberNodeToUpdate);
        }

        // Now remove any existing nodes that we did not update in the previous loop.
        List<String> existingListMemberPaths = augeas.match(listNode.getPath() + "/*");
        for (String existingListMemberPath : existingListMemberPaths) {
            AugeasNode existingListMemberNode = new AugeasNode(existingListMemberPath);
            if (!updatedMemberNodes.contains(existingListMemberNode)) {
                augeas.remove(existingListMemberPath);
            }
        }
    }

    private boolean isPropertyDefined(PropertyDefinition propDef, AbstractPropertyMap parentPropMap) {
        Property prop = parentPropMap.get(propDef.getName());
        if (prop == null) {
            return false;
        } else {
            return (!(prop instanceof PropertySimple) || ((PropertySimple) prop).getStringValue() != null);
        }
    }

    private void removeNodeIfItExists(Augeas augeas, AugeasNode node) {
        if (augeas.exists(node.getPath())) {
            log.debug("Removing node " + node + " from Augeas tree...");
            augeas.remove(node.getPath());
        }
    }

    @Nullable
    private String getListMemberMapKey(PropertyDefinitionList propDefList) {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertyMap mapKeyNames = pluginConfig.getMap("listMemberMapKeyNames");
        if (mapKeyNames == null) {
            return null;
        }
        String listName = propDefList.getName();
        return mapKeyNames.getSimpleValue(listName, null);
    }

    private void saveConfigurationFile() {
        // TODO: Backup original file.
        try {
            this.augeas.save();
        } catch (AugeasException e) {
            StringBuilder buffer = new StringBuilder("Failed to save " + this.configFile + ".");
            AugeasNode errorNode = new AugeasNode(this.augeasConfigFileMetadataNode, "error");
            String error = this.augeas.get(errorNode.getPath());
            if (error != null) {
                buffer.append(" Cause: ").append(error);
                AugeasNode errorMessageNode = new AugeasNode(errorNode, "message");
                String errorMessage = this.augeas.get(errorMessageNode.getPath());
                if (errorMessage != null) {
                    buffer.append(": ").append(errorMessage);
                }
            }
            throw new RuntimeException(buffer.toString(), e);
        }
    }
}
