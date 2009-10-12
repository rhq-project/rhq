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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * Loads and edits Connection Factories in the connection factory xml format
 *
 * @author Mark Spritzler
 */
public class ConnectionFactoryConfigurationEditor {
    //private static final String NO_TX_TYPE = "no-tx-connection-factory";
    private static final String XA_TX_TYPE = "tx-connection-factory";

    private static final String[] COMMON_PROPS = { "jndi-name", "user-name", "password", "min-pool-size",
        "max-pool-size", "adapter-display-name", "rar-name", "connection-definition", "track-connection-by-tx",
        "use-java-context", "no-tx-separate-pools", "isSameRM-override-value", "use-strict-min", "depends", "prefill",
        "blocking-timeout-millis", "type-mapping", "new-connection-sql", "check-valid-connection-sql",
        "idle-timeout-minutes" };

    private static final List<String> blankProps;

    static {
        blankProps = new ArrayList<String>();
        blankProps.add("track-connection-by-tx");
    }

    private static final String[] XA_PROPS = { "xa-resource-timeout" };

    private static final String APPLICATION_MANAGED_SECURITY = "application-managed-security";
    private static final String SECURITY_AND_APPLICATION = "security-domain-and-application";
    private static final String SECURITY_APP_MANAGED = "security-domain";
    // Using a different constant for the same string, because the first two are for the elements tags
    // and this one below is for a Resource Configuration Property.
    private static final String SECURITY_DOMAIN = "security-domain";
    //private static final String DEPENDS_PROPERTY = "depends";
    private static final String TRANSACTION_TYPE_PROPERTY = "transaction-type";
    private static final String LOCAL_TX_TYPE_PROPERTY_VALUE = "Local Transaction";
    private static final String XA_TX_TYPE_PROPERTY_VALUE = "XA Transaction";
    private static final String CONFIG_PROPERTY = "config-property";
    private static final String CONFIG_PROPERTY_NAME = "config-property-name";
    private static final String CONFIG_PROPERTY_TYPE = "config-property-type";
    private static final String CONFIG_PROPERTY_VALUE = "config-property-value";

    private static Log log = LogFactory.getLog(ConnectionFactoryConfigurationEditor.class);

    public static Configuration loadConnectionFactory(File file, String name) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            // Get the root element
            Element root = doc.getRootElement();

            if (!root.getName().equals("connection-factories")) {
                return null;
            }

            Element connectionFactoryElement = findConnectionFactoryElement(root, name);

            if (connectionFactoryElement == null) {
                return null;
            }

            Configuration config = new Configuration();
            String type = connectionFactoryElement.getName();
            config.put(new PropertySimple("type", type));

            bindElements(connectionFactoryElement, config, COMMON_PROPS);
            bindSecurityInfo(connectionFactoryElement, config);
            bindTransactionType(connectionFactoryElement, config);
            if (type.equals(XA_TX_TYPE)) {
                bindElements(connectionFactoryElement, config, XA_PROPS);
            }
            bindList(connectionFactoryElement, config, CONFIG_PROPERTY);
            //bindList(connectionFactoryElement, config, DEPENDS_PROPERTY);

            return config;
        } catch (IOException e) {
            log.error("IO error occurred while reading file: " + file, e);
        } catch (JDOMException e) {
            log.error("Parsing error occurred while reading file: " + file, e);
        }

        return null;
    }

    /**
     * Writes out connection factory changes to the file system. If the file does not exist it will create a new file in the
     * requested location.
     *
     * @param deploymentFile XML File that holds the Connection Factory information
     * @param name Name of the Connection Factory
     * @param report ConfigurationUpdateReport object
     */
    public static void updateConnectionFactory(File deploymentFile, String name, ConfigurationUpdateReport report) {
        try {
            updateConnectionFactory(deploymentFile, name, report.getConfiguration());
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (IOException e) {
            report.setErrorMessageFromThrowable(e);
            log.error("IO error occurred while updating connection factory at file: " + deploymentFile, e);
        } catch (JDOMException e) {
            report.setErrorMessageFromThrowable(e);
            log.error("Parsing error occurred while updating connection factory at file: " + deploymentFile, e);
        } catch (Exception e) {
            report.setErrorMessageFromThrowable(e);
            log.error("Unable to update connection factory at file" + deploymentFile, e);
        }
    }

    public static void updateConnectionFactory(File deploymentFile, String name, CreateResourceReport report) {
        try {
            updateConnectionFactory(deploymentFile, name, report.getResourceConfiguration());
            report.setStatus(CreateResourceStatus.SUCCESS);
        } catch (IOException e) {
            report.setException(e);
            log.error("IO error occurred while creating connection factory at file: " + deploymentFile, e);
        } catch (JDOMException e) {
            report.setException(e);
            log.error("Parsing error occurred while creating connection factory at file: " + deploymentFile, e);
        } catch (Exception e) {
            report.setException(e);
            log.error("Unable to create connection factory at file" + deploymentFile, e);
        }
    }

    private static void updateConnectionFactory(File deploymentFile, String name, Configuration config)
        throws JDOMException, IOException {
        Document doc;
        Element root;
        if (deploymentFile.exists()) {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(deploymentFile);
            root = doc.getRootElement();
        } else {
            doc = new Document();
            root = new Element("connection-factories");
            doc.setRootElement(root);
        }

        if (!root.getName().equals("connection-factories")) {
            throw new RuntimeException("Connection Factory file format exception on [" + deploymentFile
                + "], expected [connection-factories] element but found [" + root.getName() + "]");
        }

        Element connectionFactoryElement = findConnectionFactoryElement(root, name);

        String type = config.getSimpleValue("type", null);

        boolean isNewConnectionFactory = false;
        if (connectionFactoryElement == null) {
            connectionFactoryElement = new Element(type);
            isNewConnectionFactory = true;
        } else if (!type.equals(connectionFactoryElement.getName())) {
            connectionFactoryElement.setName(type);
        }

        updateElements(connectionFactoryElement, config, COMMON_PROPS);
        updateSecurityInfo(connectionFactoryElement, config);
        updateTransactionType(connectionFactoryElement, config);
        if (type.equals(XA_TX_TYPE)) {
            updateElements(connectionFactoryElement, config, XA_PROPS);
        }

        updateList(connectionFactoryElement, config, CONFIG_PROPERTY);
        //updateList(connectionFactoryElement, config, DEPENDS_PROPERTY);

        if (isNewConnectionFactory) {
            root.addContent(connectionFactoryElement);
        }

        updateFile(deploymentFile, doc);
    }

    public static void deleteConnectionFactory(File deploymentFile, String name) {
        Document doc;
        Element root;
        if (deploymentFile != null && deploymentFile.exists()) {
            try {
                SAXBuilder builder = new SAXBuilder();
                doc = builder.build(deploymentFile);
                root = doc.getRootElement();

                if (root != null) {
                    if (!root.getName().equals("connection-factories")) {
                        throw new RuntimeException("Connection Factory file format exception on [" + deploymentFile
                            + "], expected [connection-factories] element but found [" + root.getName() + "]");
                    }

                    Element connectionFactoryElement = findConnectionFactoryElement(root, name);
                    root.removeContent(connectionFactoryElement);
                }

                updateFile(deploymentFile, doc);
            } catch (JDOMException e) {
                log.error("Parsing error occurred while deleting connection factory at file: " + deploymentFile, e);
            } catch (IOException e) {
                log.error("IO error occurred while deleting connection factory at file: " + deploymentFile, e);
            }
        } else {
            // Not able to get to the deploymentFile
        }
    }

    /*private static void updateListOfMaps(Element parent, Configuration configuration, String name) {
        PropertyList propertyList = configuration.getList(name);
        List elementList = parent.getChildren(name);

        if (!elementList.isEmpty()) {
            parent.removeChildren(name);
        }

        for (Property prop : propertyList.getList()) {
            Element element = new Element(name);
            PropertyMap propertyMap = (PropertyMap) prop;
            PropertySimple configPropertyName = (PropertySimple) propertyMap.getMap().get(CONFIG_PROPERTY_NAME);
            PropertySimple configPropertyType = (PropertySimple) propertyMap.getMap().get(CONFIG_PROPERTY_TYPE);
            PropertySimple configPropertyValue = (PropertySimple) propertyMap.getMap().get(CONFIG_PROPERTY_VALUE);

            element.setAttribute("name", configPropertyName.getStringValue());
            element.setAttribute("type", configPropertyType.getStringValue());
            element.setText(configPropertyValue.getStringValue());
            elementList.add(element);
            parent.addContent(elementList);
        }

    }*/

    private static void updateList(Element parent, Configuration config, String propertyName) {
        PropertyList propertyList = config.getList(propertyName);
        List elementList = parent.getChildren(propertyName);

        if (!elementList.isEmpty()) {
            parent.removeChildren(propertyName);
        }

        for (Property property : propertyList.getList()) {
            Element element = new Element(propertyName);
            if (propertyName.equals(CONFIG_PROPERTY)) {
                setListElementValuesForConfigProperties(property, element);
            } else {
                setListElementValuesForDepends(property, element);
            }
            parent.addContent(element);
        }
    }

    private static void setListElementValuesForConfigProperties(Property property, Element element) {
        PropertyMap propertyMap = (PropertyMap) property;
        PropertySimple configPropertyName = (PropertySimple) propertyMap.getMap().get(CONFIG_PROPERTY_NAME);
        PropertySimple configPropertyType = (PropertySimple) propertyMap.getMap().get(CONFIG_PROPERTY_TYPE);
        PropertySimple configPropertyValue = (PropertySimple) propertyMap.getMap().get(CONFIG_PROPERTY_VALUE);

        element.setAttribute("name", configPropertyName.getStringValue());
        element.setAttribute("type", configPropertyType.getStringValue());
        element.setText(configPropertyValue.getStringValue());
    }

    public static void setListElementValuesForDepends(Property property, Element element) {
        PropertySimple propertySimple = (PropertySimple) property;
        element.setText(propertySimple.getStringValue());
    }

    private static void updateElements(Element parent, Configuration config, String[] names) {
        for (String prop : names) {
            updateElement(parent, config, prop);
        }
    }

    private static void updateElement(Element parent, Configuration config, String name) {
        String value = config.getSimpleValue(name, null);
        Element child = parent.getChild(name);

        if (value == null || value.equals("")) {
            if (child != null) {
                parent.removeContent(child);
            }
        } else {
            if (child == null) {
                child = new Element(name);
                parent.addContent(child);
            }

            // If this is a blank boolean property, we do not want a value in the tag, just the tag itself
            if (blankProps.contains(name)) {
                if (value.equals("false")) {
                    parent.removeContent(child);
                }
            } else {
                child.setText(value);
            }
        }
    }

    /*
     * APPLICATION_MANAGED_SECURITY and SECURITY_APP_MANAGED are combined, you have to have both.
     * If you want a Security Domain that is not App Managed then you just need the SECURITY_AND_APPLICATION tags
     */
    private static void updateSecurityInfo(Element parent, Configuration config) {
        // In Jon's configurion SecurityDomain and SecurityDomainAndApplication is stored in the same property
        String appManagedValue = config.getSimpleValue(APPLICATION_MANAGED_SECURITY, null);
        String securityDomainValue = config.getSimpleValue(SECURITY_DOMAIN, null);
        Element useApplication = parent.getChild(APPLICATION_MANAGED_SECURITY);
        Element securityDomain = parent.getChild(SECURITY_APP_MANAGED);
        Element securityAndApp = parent.getChild(SECURITY_AND_APPLICATION);

        // App Managed means you need both the Application Managed Security and Security Domain tags in the xml
        if (appManagedValue == null) {
            // First make sure the APPLICATION_MANAGED_SECURITY and SECURITY_APP_MANAGED are not elements
            if (useApplication != null) {
                parent.removeContent(useApplication);
            }
            if (securityDomain != null) {
                parent.removeContent(securityDomain);
            }

            // If this is not app managed, and there is a value in the securityAndAppProperty then need the
            // security-domain element.
            if (securityDomainValue != null && !securityDomainValue.equals("")) {
                if (securityAndApp == null) {
                    securityAndApp = new Element(SECURITY_AND_APPLICATION);
                    parent.addContent(securityAndApp);
                }
                securityAndApp.setText(securityDomainValue);
            }
        } else {
            // If missing the security-domain information while being App Managed should cause the update to fail.
            if (securityDomainValue == null || securityDomainValue.equals("")) {
                log.error("Security Domain property is required if selecting to use Application Managed Security");
                throw new RuntimeException(
                    "Security Domain property is required if selecting to use Application Managed Security");
            } else {
                if (useApplication == null) {
                    useApplication = new Element(APPLICATION_MANAGED_SECURITY);
                    parent.addContent(useApplication);
                }
                // There should not be a security-domain-and-application element, if there is remove it
                if (securityAndApp != null) {
                    parent.removeContent(securityAndApp);
                }

                if (securityDomain == null) {
                    securityDomain = new Element(SECURITY_APP_MANAGED);
                }
                securityDomain.setText(securityDomainValue);
            }
        }
    }

    private static void updateTransactionType(Element parent, Configuration config) {
        String transactionType = config.getSimpleValue(TRANSACTION_TYPE_PROPERTY, null);
        Element useLocalTransactionType = parent.getChild("local-transaction");
        Element useXATransactionType = parent.getChild("xa-transaction");

        // delete any existing elements tags
        if (useLocalTransactionType != null) {
            parent.removeChild("local-transaction");
        }

        if (useXATransactionType != null) {
            parent.removeChild("xa-transaction");
        }

        // if a transactionType was specified add the appropriate element back
        if (transactionType != null) {
            if (transactionType.equals(LOCAL_TX_TYPE_PROPERTY_VALUE)) {
                Element child = new Element("local-transaction");
                parent.addContent(child);
            } else {
                Element child = new Element("xa-transaction");
                parent.addContent(child);
            }
        }
    }

    /*
        private static void bindListOfMaps(Element parent, Configuration config, String listName) {

            PropertyList listOfMaps = new PropertyList(listName);
            List<Property> maps = new ArrayList<Property>();

            for (Object child : parent.getChildren(listName)) {
                Element childElement = (Element) child;

                String name = childElement.getAttributeValue("name");
                String type = childElement.getAttributeValue("type");
                String value = childElement.getText();
                PropertyMap map = new PropertyMap("property-values");
                map.put(new PropertySimple(CONFIG_PROPERTY_NAME, name));
                map.put(new PropertySimple(CONFIG_PROPERTY_TYPE, type));
                map.put(new PropertySimple(CONFIG_PROPERTY_VALUE, value));
                maps.add(map);
            }
            listOfMaps.setList(maps);
            config.put(listOfMaps);
        }
    */

    private static void bindList(Element parent, Configuration config, String listName) {
        PropertyList propertyList = new PropertyList(listName);
        List<Property> properties = new ArrayList<Property>();

        for (Object child : parent.getChildren(listName)) {
            Element childElement = (Element) child;
            if (listName.equals(CONFIG_PROPERTY)) {
                setListOfConfigProperties(properties, childElement);
            } else {
                setListOfDependencyProperties(properties, childElement);
            }
        }
        propertyList.setList(properties);
        config.put(propertyList);
    }

    private static void setListOfConfigProperties(List<Property> propertyList, Element element) {
        String name = element.getAttributeValue("name");
        String type = element.getAttributeValue("type");
        String value = element.getText();
        PropertyMap map = new PropertyMap("property-values");
        map.put(new PropertySimple(CONFIG_PROPERTY_NAME, name));
        map.put(new PropertySimple(CONFIG_PROPERTY_TYPE, type));
        map.put(new PropertySimple(CONFIG_PROPERTY_VALUE, value));
        propertyList.add(map);
    }

    private static void setListOfDependencyProperties(List<Property> propertyList, Element element) {
        //String depends = element.getText();
        //PropertySimple propertySimple = new PropertySimple(DEPENDS_PROPERTY, depends);
        //propertyList.add(propertySimple);
    }

    private static void bindElements(Element parent, Configuration config, String[] names) {
        for (String prop : names) {
            bindElement(parent, config, prop);
        }
    }

    private static void bindElement(Element parent, Configuration config, String name) {
        Element child = parent.getChild(name);

        if (child != null) {
            // If this is a blank boolean property, we do not want a value in the tag, just the tag itself
            if (!blankProps.contains(name)) {
                config.put(new PropertySimple(name, child.getText()));
            } else {
                config.put(new PropertySimple(name, "true"));
            }
        }
    }

    private static void bindSecurityInfo(Element parent, Configuration config) {
        Element useApplication = parent.getChild(APPLICATION_MANAGED_SECURITY);
        if (useApplication != null) {
            config.put(new PropertySimple(APPLICATION_MANAGED_SECURITY, "true"));
            Element securityDomain = parent.getChild(SECURITY_APP_MANAGED);
            if (securityDomain != null) {
                config.put(new PropertySimple(SECURITY_DOMAIN, securityDomain.getValue()));
            } else {
                config.put(new PropertySimple(SECURITY_DOMAIN, "Value required with App Managed Security"));
            }
        } else {
            Element domainAndApplication = parent.getChild(SECURITY_AND_APPLICATION);
            if (domainAndApplication != null) {
                config.put(new PropertySimple(SECURITY_DOMAIN, domainAndApplication.getValue()));
            }
        }
    }

    private static void bindTransactionType(Element parent, Configuration config) {
        Element useLocalTransactionType = parent.getChild("local-transaction");
        Element useXATransactionType = parent.getChild("xa-transaction");

        if ((useXATransactionType != null && useLocalTransactionType != null)
            || ((useXATransactionType == null && useLocalTransactionType == null))) {
            // This is an either/or
            return;
        }

        if (useLocalTransactionType != null) {
            config.put(new PropertySimple(TRANSACTION_TYPE_PROPERTY, LOCAL_TX_TYPE_PROPERTY_VALUE));
        }

        if (useXATransactionType != null) {
            config.put(new PropertySimple(TRANSACTION_TYPE_PROPERTY, XA_TX_TYPE_PROPERTY_VALUE));
        }

    }

    private static Element findConnectionFactoryElement(Element root, String name) {
        for (Object child : root.getChildren()) {
            Element childElement = (Element) child;

            String jndiName = childElement.getChildText("jndi-name");
            if (name.equals(jndiName)) {
                return childElement;
            }
        }

        return null;
    }

    private static void updateFile(File deploymentFile, Document doc) throws JDOMException, IOException {
        FileOutputStream fos = new FileOutputStream(deploymentFile);
        XMLOutputter outp = new XMLOutputter(Format.getPrettyFormat());
        outp.output(doc, fos);
        fos.flush();
        fos.close();
    }
}