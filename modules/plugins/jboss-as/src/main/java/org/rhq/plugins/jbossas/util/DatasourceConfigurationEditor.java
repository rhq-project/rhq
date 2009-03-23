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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

/**
 * Loads and edits Datasources in the datasource xml format
 *
 * @author Greg Hinkle
 * @author Mark Spritzler
 */
public class DatasourceConfigurationEditor {
    public static final String NO_TX_TYPE = "no-tx-datasource";
    public static final String LOCAL_TX_TYPE = "local-tx-datasource";
    public static final String XA_TX_TYPE = "xa-datasource";

    // These lists don't include the one multi-valued prop for each type "connection-property" / "xa-datasource-property"
    public static final String[] COMMON_PROPS = { "jndi-name", "connection-url", "user-name", "password",
        "min-pool-size", "max-pool-size", "transaction-isolation", "blocking-timeout-millis", "idle-timeout-minutes",
        "prepared-statement-cache-size", "valid-connection-checker-class-name", "use-java-context", "security-domain",
        "new-connection-sql", "exception-sorter-class-name", "check-valid-connection-sql", "track-statements",
        "no-tx-separate-pools", "application-managed-security", "security-domain-and-application" };

    public static final String[] NON_XA_PROPS = { "driver-class" };

    public static final String[] XA_PROPS = { "xa-datasource-class", "track-connection-by-tx",
        "isSameRM-override-value" };

    /* All props with customizations
     * jndi-name driver-class  <!-- xa-datasource-class --> connection-url user-name password min-pool-size
     * max-pool-size transaction-isolation blocking-timeout-millis idle-timeout-minutes prepared-statement-cache-size
     * valid-connection-checker-class-name use-java-context security-domain new-connection-sql
     * exception-sorter-class-name check-valid-connection-sql track-statements connection-property <!--
     * xa-datasource-property --> no-tx-separate-pools application-managed-security security-domain-and-application
     * track-connection-by-tx" <!-- XA only --> isSameRM-override-value" <!-- XA only -->
     */

    private static Log log = LogFactory.getLog(DatasourceConfigurationEditor.class);

    public static Configuration loadDatasource(File file, String name) {
        /*
         *    <local-tx-datasource>       <jndi-name>RHQDS</jndi-name>
         * <connection-url>${rhq.server.database.connection-url}</connection-url>
         * <driver-class>${rhq.server.database.driver-class}</driver-class>
         * <user-name>${rhq.server.database.user-name}</user-name>
         * <password>${rhq.server.database.password}</password>
         *
         *     <!-- You can include connection properties that will get passed in            the
         * DriverManager.getConnection(props) call;            look at your Driver docs to see what these might be. -->
         *      <connection-property name="char.encoding">UTF-8</connection-property>       <!-- Tells an oracle
         * 10.2.0.2 driver to properly implement clobs. -->       <connection-property
         * name="SetBigStringTryClob">true</connection-property>
         *
         *     <transaction-isolation>TRANSACTION_READ_COMMITTED</transaction-isolation>
         * <min-pool-size>25</min-pool-size>       <max-pool-size>100</max-pool-size>
         * <blocking-timeout-millis>5000</blocking-timeout-millis>       <idle-timeout-minutes>15</idle-timeout-minutes>
         *       <prepared-statement-cache-size>75</prepared-statement-cache-size>
         *
         *     <type-mapping>${rhq.server.database.type-mapping}</type-mapping>
         *
         *   </local-tx-datasource>
         */
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            // Get the root element
            Element root = doc.getRootElement();

            if (!root.getName().equals("datasources")) {
                return null;
            }

            Element datasourceElement = findDatasourceElement(root, name);

            if (datasourceElement == null) {
                return null;
            }

            Configuration config = new Configuration();
            String type = datasourceElement.getName();
            config.put(new PropertySimple("type", type));

            bindElements(datasourceElement, config, COMMON_PROPS);

            if (type.equals(XA_TX_TYPE)) {
                bindElements(datasourceElement, config, XA_PROPS);
                bindMap(datasourceElement, config, "xa-datasource-properties");
            } else {
                bindElements(datasourceElement, config, NON_XA_PROPS);
                bindMap(datasourceElement, config, "connection-property");
            }

            return config;
        } catch (IOException e) {
            log.error("IO error occurred while reading file: " + file, e);
        } catch (JDOMException e) {
            log.error("Parsing error occurred while reading file: " + file, e);
        }

        return null;
    }

    /**
     * Writes out datasource changes to the file system. If the file does not exist it will create a new file in the
     * requested location.
     *
     * @param deploymentFile
     * @param name
     * @param report
     */
    public static void updateDatasource(File deploymentFile, String name, ConfigurationUpdateReport report) {
        try {
            updateDatasource(deploymentFile, name, report.getConfiguration());
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (IOException e) {
            report.setErrorMessageFromThrowable(e);
            log.error("IO error occurred while updating datasource at file: " + deploymentFile, e);
        } catch (JDOMException e) {
            report.setErrorMessageFromThrowable(e);
            log.error("Parsing error occurred while updating datasource at file: " + deploymentFile, e);
        }
    }

    public static void updateDatasource(File deploymentFile, String name, CreateResourceReport report) {
        try {
            updateDatasource(deploymentFile, name, report.getResourceConfiguration());
            report.setStatus(CreateResourceStatus.SUCCESS);
        } catch (IOException e) {
            report.setException(e);
            log.error("IO error occurred while updating datasource at file: " + deploymentFile, e);
        } catch (JDOMException e) {
            report.setException(e);
            log.error("Parsing error occurred while updating datasource at file: " + deploymentFile, e);
        }
    }

    private static void updateDatasource(File deploymentFile, String name, Configuration config) throws JDOMException,
        IOException {
        Document doc;
        Element root;
        if (deploymentFile.exists()) {
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(deploymentFile);
            root = doc.getRootElement();
        } else {
            doc = new Document();
            root = new Element("datasources");
            doc.setRootElement(root);
        }

        if (!root.getName().equals("datasources")) {
            throw new RuntimeException("Datasource file format exception on [" + deploymentFile
                + "], expected [datasources] element but found [" + root.getName() + "]");
        }

        Element datasourceElement = findDatasourceElement(root, name);

        String type = config.getSimpleValue("type", null);

        boolean isNewDatasource = false;
        if (datasourceElement == null) {
            datasourceElement = new Element(type);
            isNewDatasource = true;
        } else if (!type.equals(datasourceElement.getName())) {
            datasourceElement.setName(type);
        }

        updateElements(datasourceElement, config, COMMON_PROPS);

        if (type.equals(XA_TX_TYPE)) {
            updateElements(datasourceElement, config, XA_PROPS);
            updateMap(datasourceElement, config, "xa-datasource-property");
        } else {
            updateElements(datasourceElement, config, NON_XA_PROPS);
            updateMap(datasourceElement, config, "connection-property");
        }

        if (isNewDatasource) {
            root.addContent(datasourceElement);
        }

        updateFile(deploymentFile, doc);
    }

    public static void deleteDataSource(File deploymentFile, String name) {
        Document doc;
        Element root;
        if (deploymentFile.exists()) {
            try {
                SAXBuilder builder = new SAXBuilder();
                doc = builder.build(deploymentFile);
                root = doc.getRootElement();

                if (root != null) {
                    if (!root.getName().equals("datasources")) {
                        throw new RuntimeException("Datasource file format exception on [" + deploymentFile
                            + "], expected [datasources] element but found [" + root.getName() + "]");
                    }

                    Element datasourceElement = findDatasourceElement(root, name);
                    root.removeContent(datasourceElement);
                }

                updateFile(deploymentFile, doc);
            } catch (JDOMException e) {
                log.error("Parsing error occurred while deleting datasource at file: " + deploymentFile, e);
            } catch (IOException e) {
                log.error("IO error occurred while deleting datasource at file: " + deploymentFile, e);
            }
        }
    }

    private static void updateMap(Element parent, Configuration configuration, String name) {
        PropertyMap map = configuration.getMap(name);
        // Wrap in ArrayList to avoid ConcurrentModificationException when adding or removing children while iterating.
        List<Element> mapElements = new ArrayList<Element>(parent.getChildren(name));

        if ((map == null) || map.getMap().isEmpty()) {
            if (!mapElements.isEmpty()) {
                parent.removeChildren(name);
            }

            return;
        }

        Map<String, Element> elements = new HashMap<String, Element>();
        for (Element el : mapElements) {
            elements.put(el.getAttributeValue("name"), el);
            if (map.get(el.getAttributeValue("name")) == null) {
                parent.removeContent(el);
            }
        }

        for (Property prop : map.getMap().values()) {
            Element element = elements.get(prop.getName());
            if (element == null) {
                element = new Element(name);
                element.setAttribute("name", prop.getName());
                parent.addContent(element);
            }

            element.setText(((PropertySimple) prop).getStringValue());
        }
    }

    private static void updateElements(Element parent, Configuration config, String[] names) {
        for (String prop : names) {
            updateElement(parent, config, prop);
        }
    }

    private static void updateElement(Element parent, Configuration config, String name) {
        String value = config.getSimpleValue(name, null);
        Element child = parent.getChild(name);

        if (value == null) {
            if (child != null) {
                parent.removeContent(child);
            }
        } else {
            if (child == null) {
                child = new Element(name);
                parent.addContent(child);
            }

            child.setText(value);
        }
    }

    private static void bindMap(Element parent, Configuration config, String mapName) {
        PropertyMap map = new PropertyMap(mapName);

        for (Object child : parent.getChildren(mapName)) {
            Element childElement = (Element) child;
            String name = childElement.getAttributeValue("name");
            map.put(new PropertySimple(name, childElement.getText()));
        }

        config.put(map);
    }

    private static void bindElements(Element parent, Configuration config, String[] names) {
        for (String prop : names) {
            bindElement(parent, config, prop);
        }
    }

    private static void bindElement(Element parent, Configuration config, String name) {
        Element child = parent.getChild(name);

        if (child != null) {
            config.put(new PropertySimple(name, child.getText()));
        }
    }

    private static Element findDatasourceElement(Element root, String name) {
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