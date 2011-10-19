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
package org.rhq.plugins.modcluster;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.modcluster.config.JBossWebServerFile;

@SuppressWarnings({ "deprecation" })
public class CatalinaServiceComponent extends MBeanResourceComponent<JMXComponent<?>> {

    private static final Log log = LogFactory.getLog(CatalinaServiceComponent.class);

    private final static String MOD_CLUSTER_CONFIG_FILE = "modclusterConfigFile";

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        //Update JMX properties
        super.updateResourceConfiguration(report);

        if (report.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
            //Propagate the JMX configuration updates to HTTPD
            try {
                super.invokeOperation("refresh", new Configuration());
            } catch (Exception e) {
                report.setErrorMessage("Failed to save the resource configuration to file. " + e.getMessage());
            }

            //Persist configuration changes to configuration file
            if (report.getStatus() == ConfigurationUpdateStatus.SUCCESS) {
                saveResouceConfigurationToFile(report, true);
            }
        }
    }

    @Override
    protected Object getPropertyValueAsType(PropertySimple propSimple, String typeName) {
        if (typeName.equals(TimeUnit.class.getName())) {
            return TimeUnit.valueOf(propSimple.getStringValue());
        }

        return super.getPropertyValueAsType(propSimple, typeName);
    }

    private void saveResouceConfigurationToFile(ConfigurationUpdateReport report, boolean ignoreReadOnly) {
        ConfigurationDefinition configurationDefinition = this.getResourceContext().getResourceType()
            .getResourceConfigurationDefinition();

        // assume we succeed - we'll set to failure if we can't set all properties
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);

        try {
            JBossWebServerFile jbossWebServerFile = getJBossWebServerFileInstance();

            for (String key : report.getConfiguration().getSimpleProperties().keySet()) {
                PropertySimple property = report.getConfiguration().getSimple(key);
                if (property != null) {
                    try {
                        PropertyDefinitionSimple def = configurationDefinition.getPropertyDefinitionSimple(property
                            .getName());
                        if (!(ignoreReadOnly && def.isReadOnly())) {
                            jbossWebServerFile.setPropertyValue(property.getName(), property.getStringValue());
                        }
                    } catch (Exception e) {
                        property.setErrorMessage(ThrowableUtil.getStackAsString(e));
                        report.setErrorMessage("Failed setting resource configuration. " + e.getMessage());
                        log.info("Failure setting MBean Resource configuration value for " + key, e);
                    }
                }
            }

            jbossWebServerFile.saveConfigurationFile();
        } catch (Exception e) {
            report.setErrorMessage("Failed to save the resource configuration to file. " + e.getMessage());
            log.debug("Unable to save mod_cluster configuration file.", e);
        }
    }

    private JBossWebServerFile getJBossWebServerFileInstance() throws Exception {
        ModClusterServerComponent modClusterComponent = (ModClusterServerComponent) this.resourceContext
            .getParentResourceComponent();

        PropertySimple property = modClusterComponent.getResourceContext().getPluginConfiguration()
            .getSimple(MOD_CLUSTER_CONFIG_FILE);

        if (property != null) {
            String fileName = property.getStringValue();
            return new JBossWebServerFile(fileName);
        }

        return null;
    }
}
