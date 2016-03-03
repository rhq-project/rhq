/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.wildfly10.json.Address;

/**
 * This is the special implementation of {@link ConfigurationWriteDelegate} which reads configuration from resource prior applying an update. 
 * This way it can apply only configuration difference. Currently only simple properties are supported.
 * 
 * This delegate may not be used by default, because it creates additional load by reading configuration, but can be used in cases, 
 * when applying all config properties causes issues (for example EJB3 subsystem - where write to some attribute causes 
 * deployment restart - while user doing configuration change in different attribute does not expect/want deployments to restart)
 * 
 * @author lzoubek@redhat.com
 *
 */
public class ConfigurationReadWriteDelegate implements ConfigurationFacet {

    final Log log = LogFactory.getLog(this.getClass());

    private final Address address;
    private final ASConnection connection;
    private final ConfigurationDefinition configurationDefinition;

    /**
     * Create a new configuration delegate, that reads and writes the attributes for the resource at address.
     * @param configDef Configuration definition for the configuration
     * @param connection asConnection to use
     * @param address address of the resource.
     */
    public ConfigurationReadWriteDelegate(ConfigurationDefinition configDef, ASConnection connection, Address address) {
        this.configurationDefinition = configDef;
        this.connection = connection;
        this.address = address;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationLoadDelegate cle = new ConfigurationLoadDelegate(configurationDefinition, connection, address);
        return cle.loadResourceConfiguration();
    }

    /**
     * updates resource configuration by first loading it using {@link ConfigurationFacet#loadResourceConfiguration()} and
     * then going through all it's simple properties and updating only values that differ from properties coming from server. 
     * All other property types are applied without any change same way as {@link ConfigurationWriteDelegate} 
     */
    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            Configuration config = loadResourceConfiguration();
            ConfigurationDefinition configDefCopy = copyConfigurationDefinition(configurationDefinition);
            for (Property prop : report.getConfiguration().getAllProperties().values()) {
                if (prop instanceof PropertySimple) {
                    PropertySimple propSimple = (PropertySimple) prop;
                    String val1 = propSimple.getStringValue();
                    String val2 = config.getSimpleValue(propSimple.getName());
                    if (val1 == null && val2 == null) {
                        configDefCopy.getPropertyDefinitions().remove(propSimple.getName());
                    }
                    if (val1 != null) {
                        if (val1.equals(val2)) {
                            configDefCopy.getPropertyDefinitions().remove(propSimple.getName());
                        }
                    } else if (val2 != null) {
                        if (val2.equals(val1)) {
                            configDefCopy.getPropertyDefinitions().remove(propSimple.getName());
                        }
                    }
                }
            }
            ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDefCopy, connection, address);
            delegate.updateResourceConfiguration(report);
        } catch (Exception e) {
            log.error("Unable to update configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(e.getMessage());
        }
    }

    static ConfigurationDefinition copyConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        ConfigurationDefinition configDefCopy = new ConfigurationDefinition(configurationDefinition.getName(),
            configurationDefinition.getDescription());
        configDefCopy.setConfigurationFormat(configurationDefinition.getConfigurationFormat());
        configDefCopy.setPropertyDefinitions(new HashMap<String, PropertyDefinition>(configurationDefinition
            .getPropertyDefinitions()));
        return configDefCopy;
    }

}
