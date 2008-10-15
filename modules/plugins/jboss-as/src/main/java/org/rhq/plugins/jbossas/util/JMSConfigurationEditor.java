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
package org.rhq.plugins.jbossas.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jbossas.JMSComponent;

/**
 * @author Mark Spritzler
 */
public class JMSConfigurationEditor extends AbstractMessagingConfigurationEditor implements XMLConfigurationEditor {
    // Logger
    private static final Log LOG = LogFactory.getLog(JMSConfigurationEditor.class);
    private static final String SECURITY_CONF = "SecurityConf";

    private static Map<String, String> types = new HashMap<String, String>();

    {
        types.put("JMQ JMS Topic", JMSComponent.TOPIC_MBEAN_NAME);
        types.put("JMQ JMS Queue", JMSComponent.QUEUE_MBEAN_NAME);
    }

    private static Map<String, String> codes = new HashMap<String, String>();

    {
        codes.put("JMQ JMS Topic", JMQ_TOPIC_CODE);
        codes.put("JMQ JMS Queue", JMQ_QUEUE_CODE);
    }
    // Instance vars

    private String[] dependsProperties = { "DestinationManager", "SecurityManager", "ExpiryDestination" };

    private String[] simpleAttributeProperties = { "JNDIName", "InMemory", "RedeliveryLimit", "RedeliveryDelay",
        "MessageCounterHistoryDayLimit", "MaxDepth", "RecoveryRetries" };

    public JMSConfigurationEditor(String type) {
        this.type = types.get(type);
        code = codes.get(type);
        securityConfig = SECURITY_CONF;
    }

    public void updateConfiguration(File deploymentFile, String name, ConfigurationUpdateReport report) {
        report.setStatus(ConfigurationUpdateStatus.INPROGRESS);
        this.deploymentFile = deploymentFile;
        this.updateReport = report;
        try {
            config = report.getConfiguration();
            updateConfiguration(deploymentFile, name);

            if (report.getStatus() != ConfigurationUpdateStatus.FAILURE) {
                report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            }
        } catch (IOException e) {
            LOG.error("IOException when trying to read xml file for type " + type + " component " + name, e);
            report.setErrorMessageFromThrowable(e);
        } catch (JDOMException e) {
            LOG.error("JDOMException when trying to read xml file for type " + type + " component " + name, e);
            report.setErrorMessageFromThrowable(e);
        }
    }

    public void updateConfiguration(File deploymentFile, String name, CreateResourceReport report) {
        report.setStatus(CreateResourceStatus.IN_PROGRESS);
        this.deploymentFile = deploymentFile;
        this.createReport = report;
        try {
            config = report.getResourceConfiguration();
            updateConfiguration(deploymentFile, name);
            if (report.getStatus() != CreateResourceStatus.FAILURE) {
                report.setStatus(CreateResourceStatus.SUCCESS);
            }
        } catch (IOException e) {
            LOG.error("Unable to write to a new file", e);
            report.setException(e);
        } catch (JDOMException e) {
            LOG.error("Unable to convert resource into xml file elements", e);
            report.setException(e);
        }
    }

    /**
     * Method that actually will build and update the XML @see Document object from the @see Configuration object. This
     * is called from the public interface methods.
     *
     * @param  file XML File to save to
     * @param  name name of the Component
     *
     * @throws org.jdom.JDOMException If there is a parsing or creation exception of the document
     * @throws java.io.IOException    If there is IO reading errors for the File
     */
    void updateConfiguration(File file, String name) throws JDOMException, IOException {
        deploymentFile = file;
        Document doc;
        if (deploymentFile.exists()) {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(deploymentFile);
            root = doc.getRootElement();
        } else {
            doc = new Document();
            root = new Element(rootElementString);
            doc.setRootElement(root);
        }

        if (!root.getName().equals(rootElementString)) {
            throw new RuntimeException(rootElementString + " file format exception on [" + deploymentFile
                + "], expected [" + rootElementString + "] element but found [" + root.getName() + "]");
        }

        Element componentElement = findComponentElement(name);

        boolean isNewComponent = false;
        if (componentElement == null) {
            isNewComponent = true;
            componentElement = new Element("mbean");
            Attribute codeAttribute = new Attribute("code", code);
            componentElement.setAttribute(codeAttribute);
            addNameAttributeToMBeanElement(componentElement, getNameProperty());
        } else {
            componentElement.removeContent();

            // update if the Name has changed
            updateIfNameChanged(componentElement);
        }

        // Make the Elements from the Configuration object here
        //First the depends properties
        bindSimplePropertiesToConfiguration(dependsProperties, DEPENDS, "optional-attribute-name", componentElement);

        //Now the simple attributes tags
        bindSimplePropertiesToConfiguration(simpleAttributeProperties, ATTRIBUTE, "name", componentElement);

        //Now the fun part, getting securityConfig into Elements
        PropertyList securityListProperty = config.getList(SECURITY_CONF);
        if (securityListProperty != null) {
            // Create the basic elements for putting all roles into
            Element securityConf = new Element("attribute");
            Attribute secConfAttribute = new Attribute("name", SECURITY_CONF);
            securityConf.setAttribute(secConfAttribute);
            Element security = new Element("security");
            securityConf.addContent(security);

            List<Property> roles = securityListProperty.getList();
            for (Property role : roles) {
                PropertyMap roleMap = (PropertyMap) role;
                Map<String, Property> map = roleMap.getMap();
                Element roleElement = new Element(roleMap.getName());
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    PropertySimple attribute = (PropertySimple) map.get(key);
                    if (attribute.getStringValue() != null) {
                        Attribute keyAttribute = new Attribute(attribute.getName(), attribute.getStringValue());
                        roleElement.setAttribute(keyAttribute);
                    }
                }

                security.addContent(roleElement);
            }

            componentElement.addContent(securityConf);
        }

        if (isNewComponent) {
            root.addContent(componentElement);
        }

        updateFile(doc);
    }

}