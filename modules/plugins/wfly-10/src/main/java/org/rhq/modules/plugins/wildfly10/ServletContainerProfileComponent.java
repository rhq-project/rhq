package org.rhq.modules.plugins.wildfly10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.modules.plugins.wildfly10.json.ReadChildrenNames;
import org.rhq.modules.plugins.wildfly10.json.Result;

public class ServletContainerProfileComponent<T extends ResourceComponent<?>> extends BaseComponent<T> {

    // profile=load-balancer doesn't have setting=jsp or setting=websocket.
    // Ignore these properties/definitions if aren't found on current address to avoid any error.
    private final String[] groups = new String[] { "jsp", "websockets" };
    private final String groupsParent = "setting";

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = super.loadResourceConfiguration();
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        List<String> groupsToIgnore = groupsToIgnore();
        for (String group : groupsToIgnore) {
            // This resource doesn't have this setting, set a warning on data if type = string.
            List<PropertyDefinition> definitions = configDef.getPropertiesInGroup(group);
            for (PropertyDefinition propDef : definitions) {
                if (propDef instanceof PropertyDefinitionSimple) {
                    PropertyDefinitionSimple propDefSimple = (PropertyDefinitionSimple) propDef;
                    boolean isString = propDefSimple.getType() == PropertySimpleType.STRING ||
                            propDefSimple.getType() == PropertySimpleType.LONG_STRING;
                    boolean isEnum = propDefSimple.getOptionsSource() != null ||
                            !propDefSimple.getEnumeratedValues().isEmpty();
                    if (isString && !isEnum) {
                        configuration.setSimpleValue(propDef.getName(), "*** Not used on this profile ***");
                    }
                    if (isEnum) {
                        configuration.remove(propDef.getName());
                    }
                }
            }
        }
        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition().copy();
        List<String> groupsToIgnore = groupsToIgnore();
        Configuration configuration = report.getConfiguration();
        Map<String, PropertyDefinition> propDefinitions = configDef.getPropertyDefinitions();
        for (String group : groupsToIgnore) {
            // This resource doesn't have this setting, removing all the related properties.
            List<PropertyDefinition> definitions = configDef.getPropertiesInGroup(group);
            for (PropertyDefinition propDef : definitions) {
                configuration.remove(propDef.getName());
                propDefinitions.remove(propDef.getName());
            }
        }
        // Continue with the default update logic
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }

    private List<String> groupsToIgnore() {
        ArrayList<String> groupsToIgnore = new ArrayList<String>();
        ReadChildrenNames op = new ReadChildrenNames(address,groupsParent);
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            List<String> foundGroups = (List<String>)res.getResult();
            for (String group : groups) {
                if (!foundGroups.contains(group)) {
                    groupsToIgnore.add("child:setting=" + group);
                }
            }
        }
        return groupsToIgnore;
    }
}
