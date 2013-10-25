/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.jboss.on.plugins.tomcat;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.jmx.util.ObjectNameQueryUtility;

/**
 * Plugin component for representing Tomcat connectors. Much of the functionality is left to the super class,
 * however the metrics required special handling.
 *
 * @author Jay Shaughnessy
 * @author Jason Dobies
 */
public class TomcatConnectorComponent extends MBeanResourceComponent<TomcatServerComponent<?>> {

    /**
     * property name for the protocol of the connector
     */
    public static final String CONFIG_PROTOCOL = "protocol";

    /**
     * V5 only property
     */
    public static final String CONFIG_V5_KEEP_ALIVE_TIMEOUT = "keepAliveTimeout";

    /**
     * Plugin property name for the address the connector is bound to.
     */
    public static final String PLUGIN_CONFIG_ADDRESS = "address";
    /**
     * Plugin property name for the connector type the connector is bound to.
     */
    public static final String PLUGIN_CONFIG_CONNECTOR = "connector";
    /**
     * Plugin property name for the protocol handler. This prefix is used in the associated GlobalRequestProcessor object name.
     */
    public static final String PLUGIN_CONFIG_HANDLER = "handler";
    /**
     * Plugin property name for the name.
     */
    public static final String PLUGIN_CONFIG_NAME = "name";
    /**
     * Plugin property name for the port the connector is listening on.
     */
    public static final String PLUGIN_CONFIG_PORT = "port";
    /**
     * Plugin property name for the optional shared executor the connector is using for its threadpool.
     * If this property is left unset, the connector is not using a shared executor.
     */
    public static final String PLUGIN_CONFIG_SHARED_EXECUTOR = "sharedExecutorName";

    public static final String UNKNOWN = "?";

    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public AvailabilityType getAvailability() {
        // First, ensure the underlying mbean for the connector is active
        AvailabilityType result = super.getAvailability();

        if (AvailabilityType.UP == result) {
            // When the connector is stopped its associated GlobalRequestProcessor will not exist. We test
            // for availability by checking the existence of objectName Catalina:type=GlobalRequestProcessor,name=%handler%[%address%]-%port%.
            String objectName = getGlobalRequestProcessorName();
            EmsConnection connection = getEmsConnection();
            ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectName);
            List<EmsBean> beans = connection.queryBeans(queryUtility.getTranslatedQuery());

            result = (beans.isEmpty()) ? AvailabilityType.DOWN : AvailabilityType.UP;
        }

        return result;
    }

    @Override
    public void start(ResourceContext<TomcatServerComponent<?>> context) {
        if (UNKNOWN.equals(context.getPluginConfiguration().getSimple(PLUGIN_CONFIG_HANDLER).getStringValue())) {
        	throw new InvalidPluginConfigurationException(
        			"The connector is not listening for requests on the configured port. This is most likely due to the configured port being in use at Tomcat startup. In some cases (AJP connectors) Tomcat will assign an open port. This happens most often when there are multiple Tomcat servers running on the same platform. Check your Tomcat configuration for conflicts: "
                    + context.getResourceKey());
        }

        super.start(context);
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        getEmsConnection(); // reload the EMS connection

        for (MeasurementScheduleRequest request : requests) {
            String req = request.getName();
            req = switchConnectorThreadpoolName(req);
            req = getAttributeName(req);

            String beanName = req.substring(0, req.lastIndexOf(':'));
            String attributeName = req.substring(req.lastIndexOf(':') + 1);

            try {
                // Bean is cached by EMS, so no problem with getting the bean from the connection on each call
                EmsBean eBean = loadBean(beanName);
                if (eBean == null) {
                    log.warn("Bean " + beanName + " not found, skipping ...");
                    continue;
                }

                EmsAttribute attribute = eBean.getAttribute(attributeName);

                Object valueObject = attribute.refresh();
                Number value = (Number) valueObject;

                report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
            } catch (Exception e) {
                log.error("Failed to obtain measurement [" + req + "]", e);
            }
        }
    }

    /**
     * If the given name represents a property name for a threadpool metric, this will switch
     * the name IF the connector is using a shared executor for its threadpool. If the connector is
     * not using a shared executor, nothing has to be switched and we can use the original name
     * passed to this method.
     *
     * See BZ 795531.
     *
     * @param property the metric property name that may need to be switched if it is a threadpool metric
     * @return the name for the metric property, switched to use the shared executor name if appropriate
     */
    private String switchConnectorThreadpoolName(String property) {
        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        String sharedExecutorName = pluginConfiguration.getSimpleValue(PLUGIN_CONFIG_SHARED_EXECUTOR, "");
        if (sharedExecutorName == null || sharedExecutorName.trim().isEmpty()) {
            return property; // there is nothing special to do if the connector isn't using a shared executor for its threadpool
        }

        // 1) Catalina:type=ThreadPool,name=%name%:currentThreadsBusy
        //    will be replaced with:
        //    Catalina:type=Executor,name=<name of shared executor>:activeCount
        //
        // 2) Catalina:type=ThreadPool,name=%name%:currentThreadCount
        //    will be replaced with:
        //    Catalina:type=Executor,name=<name of shared executor>:poolSize
        //
        // 3) Catalina:type=ThreadPool,name=%name%:maxThreads
        //    will be replaced with
        //    Catalina:type=Executor,name=<name of shared executor>:maxThreads
        final String NON_SHARED_THREADS_ACTIVE = "Catalina:type=ThreadPool,name=%name%:currentThreadsBusy";
        final String NON_SHARED_THREADS_ALLOCATED = "Catalina:type=ThreadPool,name=%name%:currentThreadCount";
        final String NON_SHARED_THREADS_MAX = "Catalina:type=ThreadPool,name=%name%:maxThreads";
        final String SHARED_THREADS_ACTIVE = "Catalina:type=Executor,name=XXX:activeCount";
        final String SHARED_THREADS_ALLOCATED = "Catalina:type=Executor,name=XXX:poolSize";
        final String SHARED_THREADS_MAX = "Catalina:type=Executor,name=XXX:maxThreads";

        if (property.equals(NON_SHARED_THREADS_ACTIVE)) {
            property = SHARED_THREADS_ACTIVE;
        } else if (property.equals(NON_SHARED_THREADS_ALLOCATED)) {
            property = SHARED_THREADS_ALLOCATED;
        } else if (property.equals(NON_SHARED_THREADS_MAX)) {
            property = SHARED_THREADS_MAX;
        } else {
            return property; // this isn't one of the names we need to switch, immediate return the original name as-is
        }

        property = property.replace("XXX", sharedExecutorName);
        return property;
    }

    /**
     * Get the real name of the passed property for a concrete connector. The actual object name will begin with:
     * Catalina:type=GlobalRequestProcessor,name=handler[-address]-port. We need to
     * substitute in the address and port of this particular connector before the value can be read. In the plugin
     * descriptor, these are written as %handler%, %address% and %port% respectively, so we can replace on those.
     */
    @Override
    protected String getAttributeName(String property) {
        String theProperty = replaceGlobalRequestProcessorNameProps(property);

        if (log.isDebugEnabled()) {
            log.debug("Finding metrics for: " + theProperty);
        }

        return theProperty;
    }

    private String getGlobalRequestProcessorName() {
        return replaceGlobalRequestProcessorNameProps("Catalina:type=GlobalRequestProcessor,name=%name%");
    }

    private String replaceGlobalRequestProcessorNameProps(String property) {

        Configuration pluginConfiguration = getResourceContext().getPluginConfiguration();
        String name = pluginConfiguration.getSimple(PLUGIN_CONFIG_NAME).getStringValue();

        String result = property.replace("%name%", name);

        return result;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        // When starting the component get the connector type specific property keys
        ResourceContext<TomcatServerComponent<?>> context = getResourceContext();
        ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition();
        String protocol = report.getConfiguration().getSimpleValue(CONFIG_PROTOCOL, null);
        if ((null == protocol) || protocol.toUpperCase().contains("HTTP")) {
            // remove AJP only properties            
            for (PropertyDefinition propDef : configDef.getPropertiesInGroup("AJP")) {
                report.getConfiguration().remove(propDef.getName());
            }
        }
        if ((null == protocol) || protocol.toUpperCase().contains("AJP")) {
            for (PropertyDefinition propDef : configDef.getPropertiesInGroup("HTTP")) {
                report.getConfiguration().remove(propDef.getName());
            }
            for (PropertyDefinition propDef : configDef.getPropertiesInGroup("HTTP SSL")) {
                report.getConfiguration().remove(propDef.getName());
            }
        }

        if (getResourceContext().getParentResourceComponent().getResourceContext().getVersion().startsWith("5")) {
            report.getConfiguration().remove(CONFIG_V5_KEEP_ALIVE_TIMEOUT);
        }
        
        EmsBean bean = super.getEmsBean();
        for (String key : report.getConfiguration().getSimpleProperties().keySet()) {
        	EmsAttribute attribute = bean.getAttribute(key); 
        	if (attribute == null) {
        		log.debug("Removing " + key + " does correspond to an attribut");
        		report.getConfiguration().remove(key);
        		continue; // skip unsupported attributes
        	}
        	PropertyDefinitionSimple def = configDef.getPropertyDefinitionSimple(key);
        	if (!def.isRequired()) {
        		PropertySimple prop = report.getConfiguration().getSimple(key); 
        		if (prop instanceof PropertySimple) {
        			PropertySimple pro = (PropertySimple) prop;
        			if (pro.getStringValue() == null || (pro.getStringValue() != null && pro.getStringValue().equals("null"))) {
        				String p = context.getResourceType().getResourceConfigurationDefinition().getDefaultTemplate().getConfiguration().getSimpleValue(key);
        				log.debug("Using default value for " + key + " value: " + def.getDefaultValue());
        				switch (def.getType()) {
        				case INTEGER: {
        					pro.setIntegerValue(Integer.valueOf(p));
        					break;
        				}
        				case LONG: {
        					pro.setLongValue((long) Long.valueOf(p));
        					break;
        				}
        				case BOOLEAN: {
        					pro.setBooleanValue(Boolean.valueOf(p));
        				}
        				/* Other type are not used in storeconfig */
        				default:
        					if (p !=null)
        						pro.setStringValue(p);
        					else if (pro.getStringValue() != null)
        						pro.setStringValue(null);
        					break;
        				}
        			}
        		}
        	}
        }

        super.updateResourceConfiguration(report);

        // if the mbean update failed, return now
        if (ConfigurationUpdateStatus.SUCCESS != report.getStatus()) {
            return;
        }

        // If all went well, persist the changes to the Tomcat server.xml
        try {
            storeConfig();
        } catch (Exception e) {
            report
                .setErrorMessage("Failed to persist configuration change.  Changes will not survive Tomcat restart unless a successful Store Configuration operation is performed.");
        }
    }

    /** Persist local changes to the server.xml */
    void storeConfig() throws Exception {
        this.getResourceContext().getParentResourceComponent().storeConfig();
    }

}