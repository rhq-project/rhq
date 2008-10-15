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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

/**
 * Common base class for MessagingConfig related stuff
 * @author Heiko W. Rupp
 */
public class AbstractMessagingConfigurationEditor {

    protected static final Log LOG = LogFactory.getLog(JBossMessagingConfigurationEditor.class);
    private static final String JNDI_NAME = "JNDIName";
    protected static final String DEPENDS = "depends";
    /**
     * Holds the type of JMS
     */
    protected String type;

    protected String code;
    /**
     * Root Element of the xml file. The component should be a child element of the root element
     */
    protected String rootElementString = "server";
    /**
     * Root Element of the XML File. Always check for NPE in your code. This is assigned in the main interface methods.
     * There are not getters or setters for this attribute.
     */
    protected Element root;
    /**
     * Configuration object for the resource. Always check for NPE in your code. Configuration object is assigned in the
     * update methods of the public interface, so there are no setters or getters needed The configuration is retrieved
     * directly from either the @see CreateResourceReport or
     *
     * @see org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport objects
     */
    protected Configuration config;
    protected CreateResourceReport createReport;
    protected ConfigurationUpdateReport updateReport;
    /**
     * File object of the XML file that will store the resource's configuration values. Always check for NPE in your
     * code. The File object is assigned in the public interface methods, so there are no setters or getters needed. The
     * File object is passed in through the APIs parameters
     */
    protected File deploymentFile;
    protected String securityConfig;
    private static final String MBEAN_NAME = "MBeanName";
    protected static final String ATTRIBUTE = "attribute";

    public AbstractMessagingConfigurationEditor() {
        super();
    }

    public void deleteComponent(File deploymentFile, String name) {
        this.deploymentFile = deploymentFile;
        Document doc;
        if (deploymentFile.exists()) {
            try {
                SAXBuilder builder = new SAXBuilder();
                doc = builder.build(deploymentFile);
                root = doc.getRootElement();

                if (root != null) {
                    if (!root.getName().equals(rootElementString)) {
                        throw new RuntimeException(rootElementString + " file format exception on [" + deploymentFile
                            + "], expected [" + rootElementString + "] element but found [" + root.getName() + "]");
                    }

                    Element datasourceElement = findComponentElement(name);
                    root.removeContent(datasourceElement);
                }

                updateFile(doc);
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Takes the child elements of the componentElement and loops through them to find the particular property via the
     * looped elements attribute and creates the PropertySimple objects to attach to the @see Configuration object
     *
     * @param elementName      String representing the type of Element to get.
     * @param attributeName    What is the property name to find within the children
     * @param componentElement Element representing this Topic or Queue
     */
    protected void bindSimplePropertiesToXML(String elementName, String attributeName, Element componentElement) {
        List<Element> children = componentElement.getChildren(elementName);
        for (Element element : children) {
            Attribute attribute = element.getAttribute(attributeName);
            String propertyName = null;

            if (attribute != null) {
                propertyName = attribute.getValue();
            }

            String elementValue = element.getValue();

            if ((propertyName != null) && (elementValue != null)) {
                PropertySimple dependProperty = new PropertySimple(propertyName, elementValue);
                config.put(dependProperty);
            }
        }
    }

    protected void bindSimplePropertiesToConfiguration(String[] properties, String elementName, String attributeName,
        Element componentElement) {
        for (String simpleAttribute : properties) {
            PropertySimple property = config.getSimple(simpleAttribute);
            if (property != null) {
                String value = property.getStringValue();
                if (value != null) {
                    if (elementName.equals(DEPENDS)) {
                        try {
                            ObjectName.getInstance(value);
                        } catch (MalformedObjectNameException e) {
                            LOG
                                .error("Dependency to "
                                    + value
                                    + " does not exist or is not deployed. Because of this invalid dependency, this Topic/Queue will not be deployed.");
                            property.setErrorMessage("'" + value + "' is not a valid JMX object name.");
                            if (updateReport != null) {
                                updateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
                            }

                            if (createReport != null) {
                                createReport.setStatus(CreateResourceStatus.FAILURE);
                            }
                        }
                    }

                    Element element = new Element(elementName);
                    Attribute attribute = new Attribute(attributeName, simpleAttribute);
                    element.setAttribute(attribute);
                    element.setText(value);
                    componentElement.addContent(element);
                }
            }
        }
    }

    /**
     * Finds the main Element object that holds all the configuration for the particular component. For example in JMS
     * the tag will be <mbean> with a name attribute for the particular Topic or Queue name
     *
     * @param  name String Topic or Queue name
     *
     * @return The Element object that holds the entire configuration.
     */
    protected Element findComponentElement(String name) {
        for (Object child : root.getChildren()) {
            Element childElement = (Element) child;

            String tagValue = childElement.getName();

            if (tagValue.equals("mbean")) {
                String mBeanName = childElement.getAttribute("name").getValue();
                Element jndiElement = childElement.getChild(JNDI_NAME);
                if (jndiElement != null) {
                    if (name.equals(jndiElement.getValue())) {
                        return childElement;
                    }
                } else {
                    String nameWithinMBean = type + ",name=" + name;
                    if (nameWithinMBean.equals(mBeanName)) {
                        return childElement;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the Element for the component that holds the Security Confs for Roles.
     *
     * @param  componentElement components main Element
     *
     * @return SecurityConf Element
     */
    protected Element findSecurityConf(Element componentElement) {
        List<Element> attributeElements = componentElement.getChildren("attribute");
        if (attributeElements != null) {
            for (Element attributeElement : attributeElements) {
                Attribute attributeName = attributeElement.getAttribute("name");
                if (attributeName.getValue().equals(securityConfig)) {
                    return attributeElement;
                }
            }
        }

        return null;
    }

    private void checkDependencies(String value, Property property) {
        try {
            ObjectName name = ObjectName.getInstance(value);
        } catch (MalformedObjectNameException e) {
            LOG
                .error("Dependency to "
                    + value
                    + " does not exist or is not deployed. because of this dependency this Topic/Queue will not deploy either.");
            property.setErrorMessage(value + " is not a valid value.");
            if (updateReport != null) {
                updateReport.setStatus(ConfigurationUpdateStatus.FAILURE);
            }

            if (createReport != null) {
                createReport.setStatus(CreateResourceStatus.FAILURE);
            }
        }
    }

    protected void loadNameProperty(Element mbeanElement) {
        String name = getNameFromMBeanElement(mbeanElement);
        PropertySimple dependProperty = new PropertySimple("MBeanName", name);
        config.put(dependProperty);
    }

    protected String getNameProperty() {
        String name = "";
        PropertySimple property = config.getSimple(MBEAN_NAME);
        if (property != null) {
            name = property.getStringValue();
        }

        return name;
    }

    protected void updateIfNameChanged(Element mbeanElement) {
        String nameInXML = getNameFromMBeanElement(mbeanElement);
        String nameInConfiguration = getNameProperty();
        if (!nameInXML.equals(nameInConfiguration)) {
            addNameAttributeToMBeanElement(mbeanElement, nameInConfiguration);
        }
    }

    protected void addNameAttributeToMBeanElement(Element mbeanElement, String name) {
        Attribute nameAttribute = new Attribute("name", "");
        nameAttribute.setValue(type + ",name=" + name);
        mbeanElement.removeAttribute("name"); // If it exists we need to remove it
        mbeanElement.setAttribute(nameAttribute);
    }

    private String getNameFromMBeanElement(Element mbeanElement) {
        Attribute name = mbeanElement.getAttribute("name");
        String attributeValue = name.getValue();
        String[] piecesOfAttribute = attributeValue.split(",name=");
        String valueInAttribute = piecesOfAttribute[1];

        return valueInAttribute;
    }

    /**
     * Updates the file.
     *
     * @param  doc JDom Document object for the file
     *
     * @throws org.jdom.JDOMException If JDOM cannot process the Document object
     * @throws java.io.IOException   If there is an exception when dealing with the File object.
     */
    protected void updateFile(Document doc) throws JDOMException, IOException {
        if (!isSetToFailure()) {
            FileOutputStream fos = new FileOutputStream(deploymentFile);
            XMLOutputter outp = new XMLOutputter(Format.getPrettyFormat());
            outp.output(doc, fos);
            fos.flush();
            fos.close();
        }
    }

    /**
     * Used to determine if we should be updating the actual file if there is a failure.
     *
     * @return boolean whether the update or create report is set to Failure status.
     */
    private boolean isSetToFailure() {
        boolean isFailure = false;
        if (updateReport != null) {
            isFailure = (updateReport.getStatus().equals(ConfigurationUpdateStatus.FAILURE));
        }

        if (createReport != null) {
            isFailure = (createReport.getStatus().equals(CreateResourceStatus.FAILURE));
        }

        return isFailure;
    }

    /**
     * Getter method
     *
     * @return String root Element name. i.e. "datasources"
     */
    public String getRootElementString() {
        return rootElementString;
    }

    /**
     * Setter method
     *
     * @param rootElement root element name. i.e. "datasources"
     */
    public void setRootElementString(String rootElement) {
        this.rootElementString = rootElement;
    }

    /**
     * Getter method
     *
     * @return String component type. i.e. "local-tx-datasource"
     */
    public String getType() {
        return type;
    }

    /**
     * Setter method
     *
     * @param type component type. i.e. "local-tx-datasource"
     */
    public void setType(String type) {
        this.type = type;
    }

    public Configuration loadConfiguration(File file, String name) {
        deploymentFile = file;
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(deploymentFile);

            // Get the root element
            root = doc.getRootElement();

            if (!root.getName().equals(rootElementString)) {
                return null;
            }

            Element componentElement = findComponentElement(name);

            if (componentElement == null) {
                return null;
            }

            config = new Configuration();

            bindSimplePropertiesToXML(DEPENDS, "optional-attribute-name", componentElement);
            bindSimplePropertiesToXML(ATTRIBUTE, "name", componentElement);
            loadNameProperty(componentElement);

            //Got to get the elements
            Element securityConf = findSecurityConf(componentElement);
            if (securityConf != null) {
                Element security = securityConf.getChild("security");
                List<Element> roles = security.getChildren();
                PropertyList rolesList = new PropertyList(securityConfig);
                for (Element role : roles) {
                    PropertyMap map = new PropertyMap("role");
                    List<Attribute> attributes = role.getAttributes();
                    for (Attribute attribute : attributes) {
                        PropertySimple property = new PropertySimple(attribute.getName(), attribute.getValue());
                        map.put(property);
                    }

                    rolesList.add(map);
                }

                config.put(rolesList);
            }

            return config;
        } catch (IOException e) {
            LOG.error("IOException when trying to read xml file for type " + type + " component " + name, e);
        } catch (JDOMException e) {
            LOG.error("Unable to convert resource into xml file elements", e);
        }

        return null;
    }

}