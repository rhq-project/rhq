/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.hadoop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.JMXServerComponent;

public class HadoopServiceComponent extends JMXServerComponent<ResourceComponent<?>> implements
    JMXComponent<ResourceComponent<?>>, MeasurementFacet, OperationFacet, ConfigurationFacet {

    private static final Log LOG = LogFactory.getLog(HadoopServiceComponent.class);

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private static final String PROPERTY_TAG_NAME = "property";
    private static final String NAME_TAG_NAME = "name";
    private static final String VALUE_TAG_NAME = "value";
    
    private HadoopOperationsDelegate operationsDelegate;

    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        return getResourceContext().getNativeProcess().isRunning() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    @Override
    public EmsConnection getEmsConnection() {
        EmsConnection conn = super.getEmsConnection();
        if (LOG.isTraceEnabled()) {
            LOG.trace("EmsConnection is " + conn.toString());
        }
        return conn;

    }

    @Override
    public void start(ResourceContext context) throws Exception {
        super.start(context);
        this.operationsDelegate = new HadoopOperationsDelegate(context);
    }
    
    
    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        for (MeasurementScheduleRequest request : metrics) {
            String name = request.getName();
            int delimIndex = name.lastIndexOf(':');
            String beanName = name.substring(0, delimIndex);
            String attributeName = name.substring(delimIndex + 1);
            try {
                EmsConnection emsConnection = getEmsConnection();
                EmsBean bean = emsConnection.getBean(beanName);
                if (bean != null) {
                    bean.refreshAttributes();
                    EmsAttribute attribute = bean.getAttribute(attributeName);
                    if (attribute != null) {
                        Object valueObject = attribute.refresh();
                        if (valueObject instanceof Number) {
                            Number value = (Number) valueObject;
                            report.addData(new MeasurementDataNumeric(request, value.doubleValue()));
                        } else {
                            report.addData(new MeasurementDataTrait(request, valueObject.toString()));
                        }
                    } else {
                        LOG.warn("Attribute " + attributeName + " not found");
                    }
                } else {
                    LOG.warn("MBean " + beanName + " not found");
                }
            } catch (Exception e) {
                LOG.error("Failed to obtain measurement [" + name + "]", e);
            }
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        ConfigurationDefinition definition = getResourceContext().getResourceType()
            .getResourceConfigurationDefinition();
        Configuration config = new Configuration();

        File homeDir = new File(getResourceContext().getPluginConfiguration().getSimpleValue(
            HadoopServiceDiscovery.HOME_DIR_PROPERTY));

        if (!homeDir.exists()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") no longer exists.");
        }

        if (!homeDir.isDirectory()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") is not a directory.");
        }

        if (!homeDir.canRead()) {
            throw new IllegalArgumentException("The configured home directory of this Hadoop instance ("
                + homeDir.getAbsolutePath() + ") is not readable.");
        }

        fillResourceConfiguration(homeDir, config, definition);

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // TODO Auto-generated method stub

    }

    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        HadoopSupportedOperations operation = HadoopSupportedOperations.valueOf(name); 
        return operationsDelegate.invoke(operation, params);
    }

    public static void fillResourceConfiguration(File homeDir, Configuration config, ConfigurationDefinition definition)
        throws XMLStreamException, IOException {
        //the config is just a bunch of simples, so this is rather easy.. no cumbersome traversal of property maps and lists

        Map<String, PropertySimple> propertiesToFind = new HashMap<String, PropertySimple>();
        Set<File> configFilesToParse = new HashSet<File>();

        for (PropertyDefinition pd : definition.getPropertyDefinitions().values()) {
            if (!(pd instanceof PropertyDefinitionSimple)) {
                //hmm... well, someone thought it's enough to change the config and the code would be clever.
                //it's not ;)
                continue;
            }

            String propertyName = pd.getName();
            String[] parts = propertyName.split(":");
            String fileName = parts[0];
            String configName = parts[1];

            File configFile = new File(homeDir, fileName);

            if (!configFile.exists()) {
                throw new IllegalArgumentException("The expected configuration file (" + configFile.getAbsolutePath()
                    + ") doesn't exist.");
            }

            configFilesToParse.add(configFile);

            PropertySimple prop = new PropertySimple();
            prop.setName(propertyName);
            config.put(prop);

            propertiesToFind.put(configName, prop);
        }

        for (File configFile : configFilesToParse) {
            parseAndAssignProps(configFile, propertiesToFind);
        }
    }

    private static void parseAndAssignProps(File configFile, Map<String, PropertySimple> props)
        throws XMLStreamException, IOException {
        FileInputStream in = new FileInputStream(configFile);
        XMLStreamReader rdr = XML_INPUT_FACTORY.createXMLStreamReader(in);
        try {
            boolean inProperty = false;
            String propertyName = null;
            String propertyValue = null;

            while (rdr.hasNext()) {
                int event = rdr.next();

                String tag = null;

                switch (event) {
                case XMLStreamReader.START_ELEMENT:
                    tag = rdr.getName().getLocalPart();
                    if (PROPERTY_TAG_NAME.equals(tag)) {
                        inProperty = true;
                    } else if (inProperty && NAME_TAG_NAME.equals(tag)) {
                        propertyName = rdr.getElementText();
                    } else if (inProperty && VALUE_TAG_NAME.equals(tag)) {
                        propertyValue = rdr.getElementText();
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    tag = rdr.getName().getLocalPart();
                    if (PROPERTY_TAG_NAME.equals(tag)) {
                        inProperty = false;

                        PropertySimple prop = props.get(propertyName);
                        if (prop != null) {
                            prop.setValue(propertyValue);
                        }

                        propertyName = null;
                        propertyValue = null;
                    }
                    break;
                }
            }
        } finally {
            rdr.close();
            in.close();
        }
    }

}
