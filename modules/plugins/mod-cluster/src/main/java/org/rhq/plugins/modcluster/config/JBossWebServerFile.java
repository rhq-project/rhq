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
package org.rhq.plugins.modcluster.config;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Stefan Negrea
 *
 */
public class JBossWebServerFile extends AbstractConfigurationFile {

    private static final String CATALINA_LISTENER_CLASS_NAME = "org.jboss.modcluster.catalina.ModClusterListener";
    private static final String CATALINA_LISTENER_CLASS_NAME_STANDALONE = "org.jboss.modcluster.container.catalina.standalone.ModClusterListener";

    private Node listenerNode;

    public JBossWebServerFile(String configurationFileName) throws ParserConfigurationException, SAXException,
        IOException {
        super(configurationFileName);

        listenerNode = this.getListenerNode();
    }

    public JBossWebServerFile(File configurationFile) throws ParserConfigurationException, SAXException, IOException {
        super(configurationFile);

        listenerNode = this.getListenerNode();
    }

    @Override
    public void setPropertyValue(String propertyName, String value) {
        NamedNodeMap attributeList = listenerNode.getAttributes();

        if (attributeList.getNamedItem(propertyName) != null && value != null) {
            Node attributeNode = attributeList.getNamedItem(propertyName);
            attributeNode.setTextContent(value);
        } else if (attributeList.getNamedItem(propertyName) != null && value == null) {
            attributeList.removeNamedItem(propertyName);
        } else if (attributeList.getNamedItem(propertyName) == null && value != null) {
            Attr property = this.getDocument().createAttribute(propertyName);
            property.setValue(propertyName);
            property.setTextContent(value);
            attributeList.setNamedItem(property);
        }
    }

    @Override
    public String getPropertyValue(String propertyName) {
        throw new UnsupportedOperationException(
            "Property values should be retrieved from the JMX interface not from the configuration file.");
    }

    private Node getListenerNode() {
        NodeList nodeList = this.getDocument().getElementsByTagName("Listener");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getAttributes().getNamedItem("className") != null
                && (CATALINA_LISTENER_CLASS_NAME.equals(node.getAttributes().getNamedItem("className").getTextContent()) ||
                    CATALINA_LISTENER_CLASS_NAME_STANDALONE.equals(node.getAttributes().getNamedItem("className").getTextContent()))) {
                return node;
            }
        }

        return null;
    }
}
