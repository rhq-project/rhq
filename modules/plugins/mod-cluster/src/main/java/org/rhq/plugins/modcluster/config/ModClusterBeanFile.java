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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Stefan Negrea
 *
 */
public class ModClusterBeanFile extends AbstractConfigurationFile {

    private Node beanNode;

    /**
     * @param className
     * @param configurationFile
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public ModClusterBeanFile(String className, String configurationFileName) throws ParserConfigurationException,
        SAXException, IOException {
        this(className, new File(configurationFileName));
    }

    /**
     * @param className
     * @param configurationFile
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public ModClusterBeanFile(String className, File configurationFile) throws ParserConfigurationException,
        SAXException, IOException {
        super(configurationFile);

        beanNode = this.getBeanNodeByClass(className);
    }

    /**
     * @param className
     * @param constructorArgumentClassName
     * @param configurationFile
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public ModClusterBeanFile(String className, String constructorArgumentClassName, String configurationFileName)
        throws ParserConfigurationException, SAXException, IOException {
        this(className, constructorArgumentClassName, new File(configurationFileName));
    }

    /**
     * @param className
     * @param constructorArgumentClassName
     * @param configurationFile
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public ModClusterBeanFile(String className, String constructorArgumentClassName, File configurationFile)
        throws ParserConfigurationException, SAXException, IOException {
        super(configurationFile);

        Node primaryBeanNode = this.getBeanNodeByClass(className);
        String dependencyName = this.getBeanFromConstructorArgument(primaryBeanNode, constructorArgumentClassName);

        beanNode = this.getBeanNodeByName(dependencyName);
    }

    /**
     * @param propertyName
     * @param value
     */
    public void setPropertyValue(String propertyName, String value) {
        boolean propertyFound = false;
        NodeList nodeList = beanNode.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);

            if (currentNode.getNodeName().equals("property")
                && currentNode.getAttributes().getNamedItem("name") != null
                && propertyName.equals(currentNode.getAttributes().getNamedItem("name").getTextContent())) {

                if (value != null) {
                    currentNode.setTextContent(value);
                } else {
                    beanNode.removeChild(currentNode);
                }

                propertyFound = true;
            }
        }

        if (value != null && !propertyFound) {
            Node propertyChild = this.getDocument().createElement("property");
            Attr nameProperty = this.getDocument().createAttribute("name");
            nameProperty.setValue(propertyName);
            propertyChild.setTextContent(value);
            propertyChild.getAttributes().setNamedItem(nameProperty);
            beanNode.appendChild(propertyChild);
        }
    }

    /**
     * @param propertyName
     * @return
     */
    public String getPropertyValue(String propertyName) {
        NodeList nodeList = beanNode.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);

            if (currentNode.getNodeName().equals("property")
                && currentNode.getAttributes().getNamedItem("name") != null
                && propertyName.equals(currentNode.getAttributes().getNamedItem("name").getTextContent())) {

                return currentNode.getTextContent();
            }
        }

        return null;
    }

    /**
     * @param className
     * @return
     */
    private Node getBeanNodeByClass(String className) {
        NodeList nodeList = this.getDocument().getElementsByTagName("bean");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getAttributes().getNamedItem("class") != null
                && className.equals(node.getAttributes().getNamedItem("class").getTextContent())) {
                return node;
            }
        }

        return null;
    }

    /**
     * @param beanName
     * @return
     */
    private Node getBeanNodeByName(String beanName) {
        NodeList nodeList = this.getDocument().getElementsByTagName("bean");

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node.getAttributes().getNamedItem("name") != null
                && beanName.equals(node.getAttributes().getNamedItem("name").getTextContent())) {
                return node;
            }
        }

        return null;
    }

    /**
     * @param beanNode
     * @param constructorArgumentClassName
     * @return
     */
    private String getBeanFromConstructorArgument(Node beanNode, String constructorArgumentClassName) {
        List<Node> nodeList = this.getChildrenNodesByName(beanNode, "constructor");

        if (nodeList.size() > 0) {
            Node constructorNode = nodeList.get(0);

            nodeList = this.getChildrenNodesByName(constructorNode, "parameter");

            if (nodeList.size() > 0) {
                Node parameterNode = null;

                for (Node currentNode : nodeList) {
                    if (currentNode.getAttributes().getNamedItem("class") != null
                        && constructorArgumentClassName.equals(currentNode.getAttributes().getNamedItem("class")
                            .getTextContent())) {
                        parameterNode = currentNode;
                        break;
                    }
                }

                if (parameterNode != null) {
                    nodeList = this.getChildrenNodesByName(parameterNode, "inject");

                    if (nodeList.size() > 0) {
                        Node injectNode = nodeList.get(0);
                        Node beanAttribute = injectNode.getAttributes().getNamedItem("bean");

                        if (beanAttribute != null) {
                            return beanAttribute.getTextContent();
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param node
     * @param nodeName
     * @return
     */
    private List<Node> getChildrenNodesByName(Node node, String nodeName) {
        List<Node> nodeList = new ArrayList<Node>();

        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node currentNode = node.getChildNodes().item(i);

            if (nodeName.equals(currentNode.getNodeName())) {
                nodeList.add(currentNode);
            }
        }

        return nodeList;
    }
}
