package org.rhq.modules.plugins.wildfly10;

import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.modules.plugins.wildfly10.json.ReadChildrenNames;
import org.rhq.modules.plugins.wildfly10.json.Result;

public class ServletContainerProfileComponent<T extends ResourceComponent<?>> extends BaseComponent<T> {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition().copy();

        // profile=load-balancer doesn't have setting=jsp or setting=websocket.
        // Remove these properties/definitions if aren't found on current address to avoid any error.
        String[] groups = new String[]{ "jsp", "websockets" };
        ReadChildrenNames op = new ReadChildrenNames(address,"setting");
        Result res = getASConnection().execute(op);
        if (res.isSuccess()) {
            List<String> foundGroups = (List<String>)res.getResult();
            Configuration configuration = report.getConfiguration();
            Map<String, PropertyDefinition> propDefinitions = configDef.getPropertyDefinitions();

            for (String group : groups) {
                if (!foundGroups.contains(group)) {
                    // This resource doesn't have this setting, removing all the related properties.
                    List<PropertyDefinition> definitions = configDef.getPropertiesInGroup("child:setting=" + group);
                    for (PropertyDefinition propDef : definitions) {
                        configuration.remove(propDef.getName());
                        propDefinitions.remove(propDef.getName());
                    }
                }
            }
        }

        // Continue with the default update logic
        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }
}
