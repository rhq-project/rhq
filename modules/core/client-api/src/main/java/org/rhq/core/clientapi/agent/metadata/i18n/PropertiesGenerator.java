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
package org.rhq.core.clientapi.agent.metadata.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Generates localization properties files for a plugin descriptor. Can update existing files by appending properties
 * that are missing to the end.
 *
 * @author Greg Hinkle
 */
public class PropertiesGenerator {
    private Map<String, String> properties = new LinkedHashMap();

    private boolean update;
    private File xmlFile;
    private File propertiesFile;

    private PrintWriter contentWriter;

    private Properties previousProperties = new Properties();

    private static Map<String, String> TAG_KEY_ATTRIBUTES = new HashMap<String, String>();
    private static Map<String, Set<String>> TAG_LOCALIZED_ATTRIBUTES = new HashMap<String, Set<String>>();

    static {
        putLocalization("plugin", "displayName");

        putLocalization("server", "displayName", "description");
        putLocalization("service", "displayName", "description");

        putLocalization("simple-property", "displayName", "description");
        putLocalization("list-property", "displayName", "description");
        putLocalization("map-property", "displayName", "description");

        putLocalization("artifact", "displayName", "description");

        TAG_KEY_ATTRIBUTES.put("metric", "property");
        putLocalization("metric", "displayName", "description");

        putLocalization("operation", "displayName");
    }

    public static Set<String> getLocalizedAttributes(String tagName) {
        return TAG_LOCALIZED_ATTRIBUTES.get(tagName);
    }

    public PropertiesGenerator(File xmlFile, File propertiesFile, boolean update) {
        this.xmlFile = xmlFile;
        this.propertiesFile = propertiesFile;
        this.update = update;
    }

    private static void putLocalization(String tagName, String... attributes) {
        HashSet<String> lp = new HashSet<String>();
        for (String attr : attributes) {
            lp.add(attr);
        }

        TAG_LOCALIZED_ATTRIBUTES.put(tagName, lp);
    }

    public void generateI18NProperties() {
        try {
            if (update) {
                // First load into properties we can check for existence
                previousProperties = new Properties();
                previousProperties.load(new FileInputStream(propertiesFile));
                this.contentWriter.println("\n\n# Contents added " + new Date() + "\n\n");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            FileOutputStream fos = new FileOutputStream(propertiesFile, this.update);
            this.contentWriter = new PrintWriter(fos);
            generateNode(doc.getDocumentElement(), "");
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            if (this.contentWriter != null) {
                this.contentWriter.close();
            }
        }
    }

    private void generateNode(Element element, String partialKey) {
        String childKey = partialKey + element.getTagName();

        String keyAttribute = TAG_KEY_ATTRIBUTES.get(element.getNodeName());
        if (keyAttribute == null) {
            keyAttribute = "name";
        }

        String tagKey = element.getAttribute(keyAttribute);
        if ((tagKey != null) && (tagKey.length() > 0)) {
            childKey += "[" + tagKey + "].";
        } else {
            childKey += ".";
        }

        generateAtributes(element, childKey);

        generateChildren(element, childKey);
    }

    private void generateChildren(Element element, String partialKey) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child instanceof Element) {
                generateNode((Element) child, partialKey);
            }
        }
    }

    private void generateAtributes(Element element, String partialKey) {
        if (element.getNodeName().equals("server") || element.getNodeName().equals("service")) {
            this.contentWriter.println();
        }

        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node n = attrs.item(i);

            Set<String> localizedProperties = TAG_LOCALIZED_ATTRIBUTES.get(element.getTagName());
            if ((localizedProperties != null) && localizedProperties.contains(n.getNodeName())) {
                String key = partialKey + n.getNodeName();
                String value = "    # " + n.getNodeValue();

                if (!this.previousProperties.containsKey(key)) {
                    this.contentWriter.println(key + "=" + value);
                    properties.put(key, value);
                }
            }
        }
    }
}