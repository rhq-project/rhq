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

package org.rhq.plugins.modcluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.modcluster.config.ModClusterBeanFile;

/**
 * @author Stefan Negrea
 *
 */
@SuppressWarnings({ "deprecation" })
public class FileConfiguredMBeanResourceComponent extends ClassNameMBeanComponent {

    private static final Log log = LogFactory.getLog(FileConfiguredMBeanResourceComponent.class);

    private static final String BEAN_CLASS_NAME_PROPERTY = "className";
    private static final String DEPENDENCY_BEAN_CLASS_NAME_PROPERTY = "dependencyClassName";
    private static final String SERVER_HOME_DIR = "serverHomeDir";
    private static final String CONFIGURATION_FILE_RELATIVE_PATH = "/deploy/mod_cluster.sar/META-INF/mod_cluster-jboss-beans.xml";

    /**
     * This default setup of configuration properties can map to mbean attributes
     *
     * @return the configuration of the component
     */
    @Override
    public Configuration loadResourceConfiguration() {

        Configuration configuration = new Configuration();
        ConfigurationDefinition configurationDefinition = this.resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        try {
            ModClusterBeanFile modClusterBeanFile = this.getModClusterBeanFileInstance();

            for (PropertyDefinition property : configurationDefinition.getPropertyDefinitions().values()) {
                if (property instanceof PropertyDefinitionSimple) {
                    String value = modClusterBeanFile.getPropertyValue(property.getName());
                    if (value != null) {
                        configuration.put(new PropertySimple(property.getName(), value));
                    } else {
                        PropertyDefinitionSimple propertyDefinitionSimple = (PropertyDefinitionSimple) property;
                        configuration.put(new PropertySimple(property.getName(), propertyDefinitionSimple
                            .getDefaultValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Unable to load mod_cluster configuration file.", e);
        }

        return configuration;
    }

    /**
     * Equivalent to updateResourceConfiguration(report, false);
     */
    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        updateResourceConfiguration(report, false);
    }

    private ModClusterBeanFile getModClusterBeanFileInstance() throws Exception {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String beanClassName = pluginConfig.getSimple(BEAN_CLASS_NAME_PROPERTY).getStringValue();

        String fileName = this.getServerHomeDirectory() + CONFIGURATION_FILE_RELATIVE_PATH;

        if (pluginConfig.getSimple(DEPENDENCY_BEAN_CLASS_NAME_PROPERTY) != null) {
            String dependencyBeanClassName = pluginConfig.getSimple(DEPENDENCY_BEAN_CLASS_NAME_PROPERTY)
                .getStringValue();
            return new ModClusterBeanFile(beanClassName, dependencyBeanClassName, fileName);
        }

        return new ModClusterBeanFile(beanClassName, fileName);
    }

    private String getServerHomeDirectory() {
        ModClusterServerComponent modClusterComponent = (ModClusterServerComponent) this.resourceContext
            .getParentResourceComponent();

        PropertySimple property = modClusterComponent.getResourceContext().getPluginConfiguration()
            .getSimple(SERVER_HOME_DIR);

        if (property != null) {
            return property.getStringValue();
        }

        return null;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report, boolean ignoreReadOnly) {
        ConfigurationDefinition configurationDefinition = this.getResourceContext().getResourceType()
            .getResourceConfigurationDefinition();

        // assume we succeed - we'll set to failure if we can't set all properties
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);

        try {
            ModClusterBeanFile modClusterBeanFile = this.getModClusterBeanFileInstance();

            for (String key : report.getConfiguration().getSimpleProperties().keySet()) {
                PropertySimple property = report.getConfiguration().getSimple(key);
                if (property != null) {
                    try {
                        PropertyDefinitionSimple def = configurationDefinition.getPropertyDefinitionSimple(property
                            .getName());
                        if (!(ignoreReadOnly && def.isReadOnly())) {
                            modClusterBeanFile.setPropertyValue(property.getName(), property.getStringValue());
                        }
                    } catch (Exception e) {
                        property.setErrorMessage(ThrowableUtil.getStackAsString(e));
                        report.setErrorMessage("Failed setting resource configuration. " + e.getMessage());
                        log.info("Failure setting MBean Resource configuration value for " + key, e);
                    }
                }
            }

            modClusterBeanFile.saveConfigurationFile();
        } catch (Exception e) {
            report.setErrorMessage("Failed to save the resource configuration to file. " + e.getMessage());
            log.debug("Unable to save mod_cluster configuration file.", e);
        }
    }

}
