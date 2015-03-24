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

import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.core.pluginapi.operation.OperationResult;

import org.rhq.plugins.modcluster.ModClusterServerComponent.SupportedOperations;

/**
 * @author Stefan Negrea, Maxime Beck
 */

@SuppressWarnings({"deprecation"})
public class CatalinaServiceComponent extends MBeanResourceComponent {

    private static final Log log = LogFactory.getLog(CatalinaServiceComponent.class);

    private static final String MOD_CLUSTER_MBEAN_NAME = "Catalina:type=ModClusterListener";

    private ModClusterOperationsDelegate operationsDelegate;

    public CatalinaServiceComponent() {
        this.operationsDelegate = new ModClusterOperationsDelegate((MBeanResourceComponent) this);
    }

    @Override
    public Configuration loadResourceConfiguration() {
        Configuration configuration = new Configuration();
        ConfigurationDefinition configurationDefinition = this.resourceContext.getResourceType()
                .getResourceConfigurationDefinition();

        try {
            EmsBean bean = getEmsConnection().getBean(MOD_CLUSTER_MBEAN_NAME);

            for (PropertyDefinition property : configurationDefinition.getPropertyDefinitions().values()) {
                if (property instanceof PropertyDefinitionSimple) {
                    EmsAttribute attribute = bean.getAttribute(property.getName());
                    if (attribute != null) {
                        configuration.put(new PropertySimple(property.getName(), attribute.refresh()));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Unable to load mod_cluster configuration file.", e);
        }

        return configuration;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        this.saveResouceConfigurationToFile(report, false);

        try {
            storeConfig();
        } catch (Exception e) {
            report.setErrorMessage("Failed to persist configuration change.");
        }
    }

    void storeConfig() throws Exception {
        invokeOperation(SupportedOperations.STORECONFIG.name(), new Configuration());
    }

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
            Exception {
        SupportedOperations operation = Enum.valueOf(SupportedOperations.class, name.toUpperCase());
        return this.operationsDelegate.invoke(operation, parameters);
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
        EmsBean bean = null;

        // assume we succeed - we'll set to failure if we can't set all properties
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);

        for (String key : report.getConfiguration().getSimpleProperties().keySet()) {
            PropertySimple property = report.getConfiguration().getSimple(key);
            if (property != null) {
                try {
                    if(null == bean)
                        bean = getEmsConnection().getBean(MOD_CLUSTER_MBEAN_NAME);
                    EmsAttribute attribute = bean.getAttribute(key);
                    if (attribute == null) {
                        log.debug("Removing " + key + " does correspond to an attribut");
                        report.getConfiguration().remove(key);
                        continue; // skip unsupported attributes
                    }
                    PropertyDefinitionSimple def = configurationDefinition.getPropertyDefinitionSimple(property
                            .getName());
                    if (!(ignoreReadOnly && def.isReadOnly())) {
                        switch (def.getType()) {
                            case INTEGER: {
                                if (property.getIntegerValue() != null)
                                    attribute.setValue(property.getIntegerValue());
                                break;
                            }

                            case LONG: {
                                if (property.getLongValue() != null)
                                    attribute.setValue(property.getLongValue());
                                break;
                            }

                            case BOOLEAN: {
                                if (property.getBooleanValue() != null)
                                    attribute.setValue(property.getBooleanValue());
                                break;
                            }

                            /*With StoreConfig, every other types are Sting*/
                            default: {
                                if (property.getStringValue() != null)
                                    attribute.setValue(property.getStringValue());
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    property.setErrorMessage(ThrowableUtil.getStackAsString(e));
                    report.setErrorMessage("Failed setting resource configuration - see property error messages for details");
                    log.info("Failure setting MBean Resource configuration value for " + key, e);
                }
            }
        }
    }
}
