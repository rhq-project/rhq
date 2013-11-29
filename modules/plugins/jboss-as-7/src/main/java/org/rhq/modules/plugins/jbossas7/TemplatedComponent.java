/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Stefan Negrea
 * @author Simeon Pinder
 *
 */
public class TemplatedComponent extends BaseComponent<ResourceComponent<?>> {

    private final static String TYPE_CONFIGURATION = "__type";
    private final static String NAME_CONFIGURATION = "__name";

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();

        if (configDef.getDefaultTemplate().getConfiguration().get(TYPE_CONFIGURATION) != null) {
            //__type is a fake property, do not attempt to load it from the managed server
            configDef.getPropertyDefinitions().remove(TYPE_CONFIGURATION);
            ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, getASConnection(), address);
            Configuration configuration = delegate.loadResourceConfiguration();

            //manually load type based on the resource path
            PropertySimple pathProperty = (PropertySimple) context.getPluginConfiguration().get("path");
            String type = pathProperty.getStringValue();
            type = type.substring(type.lastIndexOf(',') + 1, type.lastIndexOf('='));
            configuration.put(new PropertySimple(TYPE_CONFIGURATION, type));

            return configuration;
        } else if (configDef.getDefaultTemplate().getConfiguration().get(NAME_CONFIGURATION) != null) {
            //__name is a fake property, do not attempt to load it from the managed server
            configDef.getPropertyDefinitions().remove(NAME_CONFIGURATION);
            ConfigurationLoadDelegate delegate = new ConfigurationLoadDelegate(configDef, getASConnection(), address);
            Configuration configuration = delegate.loadResourceConfiguration();

            //manually load name based on the resource path
            PropertySimple pathProperty = (PropertySimple) context.getPluginConfiguration().get("path");
            String name = pathProperty.getStringValue();
            name = name.substring(name.lastIndexOf('=') + 1);
            configuration.put(new PropertySimple(NAME_CONFIGURATION, name));

            return configuration;
        }

        return super.loadResourceConfiguration();
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();

        boolean templatedComponentUpdate = false;

        if (configDef.getDefaultTemplate().getConfiguration().get(TYPE_CONFIGURATION) != null) {
            //__type is a fake property, do not attempt to save it.
            configDef.getPropertyDefinitions().remove(TYPE_CONFIGURATION);
            report.getConfiguration().remove(TYPE_CONFIGURATION);
            templatedComponentUpdate = true;
        } else if (configDef.getDefaultTemplate().getConfiguration().get(NAME_CONFIGURATION) != null) {
            //__name is a fake property, do not attempt to save it.
            configDef.getPropertyDefinitions().remove(NAME_CONFIGURATION);
            report.getConfiguration().remove(NAME_CONFIGURATION);
            templatedComponentUpdate = true;
        }

        if (templatedComponentUpdate) {
            //For templated resources we need to parse only the specific subset of attributes
            //supported by this component
            Map<String, Object> currentAttributeList = null;
            Operation currentAttributesOp = new ReadResource(address);
            Map<String, Object> additionalProperties = new HashMap<String, Object>();
            //includes operation request attributes applicable to 6.0 & 6.1
            additionalProperties.put("proxies", "true");
            additionalProperties.put("include-runtime", "true");
            additionalProperties.put("include-defaults", "true");
            additionalProperties.put("attributes-only", "true");
            currentAttributesOp.setAdditionalProperties(additionalProperties);
            Result currentAttributes = getASConnection().execute(currentAttributesOp);
            if (currentAttributes.isSuccess()) {
                currentAttributeList = (Map<String, Object>) currentAttributes.getResult();
            }

            for (PropertyDefinition propDef : configDef.getNonGroupedProperties()) {
                //with templated resources we should only parse the properties being used by this specific resource.
                if (currentAttributeList != null) {
                    //take care to strip of as7 plugin specific identifiers here when comparing attributes.
                    if (!currentAttributeList.containsKey(removeAttributeMarkup(propDef.getName()))) {
                        configDef.getPropertyDefinitions().remove(propDef.getName());
                        report.getConfiguration().remove(propDef.getName());
                    }
                }
            }
        }

        ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
        delegate.updateResourceConfiguration(report);
    }

    /** Method removes attribute metadata mark up so that the attributes can be 
     *  compared directly to results from stock as7/eap resources.
     *  Ex. queue-length:expr -> queue-length.  
     * 
     * @param name : string to scrub metadata from.
     * @return String minus known metadata markup.
     */
    private String removeAttributeMarkup(String name) {
        int index = -1;
        //Ex. Markup :pname,:key,:name,:expr,:collapsed,:nullable,:# .. where # represent [0-9]
        if ((index = name.indexOf(":")) > -1) {
            name = name.substring(0, index);
        }
        return name;
    }
}
