/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Stefan Negrea
 */
class UpdateFile implements Comparable<UpdateFile> {

    private static final String UPDATE_PLAN_ELEMENT = "updatePlan";
    private static final String STEP_ELEMENT = "step";

    private final Log log = LogFactory.getLog(UpdateFile.class);

    private final String file;

    public UpdateFile(String file) {
        this.file = file;
    }

    public String getFile() {
        return this.file;
    }

    /**
     * Retrieve a named step from the list of steps ready to be executed
     * on Cassandra.
     *
     * @param name step name
     * @return step
     * @throws Exception
     */
    public String getNamedStep(String name) {
        return getNamedStep(name, null);
    }

    /**
     * Retrieve a named step from the list of steps ready to be executed
     * on Cassandra. The step will go through variable binding process with the
     * provided properties.
     *
     * @param name step name
     * @param properties properties to bind
     * @return step
     */
    public String getNamedStep(String name, Properties properties) {
        List<Node> stepNodes = getStepNodes();
        for (Node stepNode : stepNodes) {
            Node nameNode = stepNode.getAttributes().getNamedItem("name");
            if (nameNode != null && nameNode.getNodeValue().equals(name)) {
                return bind(stepNode.getTextContent(), properties);
            }
        }

        return null;
    }

    /**
     * Retrieve all the steps in the file in declaration order. The steps are ready to
     * be executed.
     *
     * @return list of steps
     */
    public List<String> getOrderedSteps() {
        return getOrderedSteps(null);
    }

    /**
     * Retrieve all the steps in the file in declaration order. The steps are ready to
     * be executed. Each step will go through variable binding process with the
     * provided properties.
     *
     * @param properties properties to bind.
     * @return
     */
    public List<String> getOrderedSteps(Properties properties) {
        List<String> boundSteps = new ArrayList<String>();
        List<Node> stepNodes = getStepNodes();

        for (Node stepNode : stepNodes) {
            boundSteps.add(bind(stepNode.getTextContent(), properties));
        }

        return boundSteps;
    }

    /**
     * Retrieve unbound list of steps from the file in declaration order.
     *
     * @return unbound list of steps.
     */
    private List<Node> getStepNodes() {
        InputStream stream = null;
        try {
            stream = SchemaManager.class.getClassLoader().getResourceAsStream(file);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);

            NodeList updateElements = doc.getElementsByTagName(UPDATE_PLAN_ELEMENT);
            if (updateElements == null || updateElements.getLength() != 1) {
                throw new RuntimeException("No <updatePlan> elements found");
            }

            Node rootDocument = updateElements.item(0);
            NodeList updateStepElements = rootDocument.getChildNodes();

            List<Node> stepList = new ArrayList<Node>();
            for (int index = 0; index < updateStepElements.getLength(); index++) {
                Node updateStepElement = updateStepElements.item(index);
                if (STEP_ELEMENT.equals(updateStepElement.getNodeName()) && updateStepElement.getTextContent() != null) {
                    stepList.add(updateStepElements.item(index));
                }
            }

            return stepList;
        } catch (Exception e) {
            log.error("Error reading the list of steps from " + file + " file.", e);
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    log.error("Error closing the stream with the list of steps from " + file + " file.", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Bind the set of provided properties to the input step. The text should have
     * all the variable to be bound in %variable_name% form.
     *
     * This method should be called even if no properties are provided because it will
     * throw a runtime exception if the text contains properties that are expected to be
     * bound but the list of variable is either empty or does not contain
     * them.
     *
     * @param unboundText unbound text
     * @param properties properties to bind
     * @return properties bound text
     */
    private String bind(String unboundText, Properties properties) {
        Set<String> foundProperties = new HashSet<String>();
        Pattern regex = Pattern.compile("\\%([^%]*)\\%");
        Matcher matchPattern = regex.matcher(unboundText);
        while (matchPattern.find()) {
            String matchedString = matchPattern.group();
            String property = matchedString.substring(1, matchedString.length() - 1);
            foundProperties.add(property);
        }

        String boundText = unboundText;

        if( foundProperties.size() !=0 && properties == null){
            throw new RuntimeException("No properties provided but " + foundProperties.size()
                + " required for binding.");
        } else if (foundProperties.size() != 0) {
            for (String foundProperty : foundProperties) {
                String propertyValue = properties.getProperty(foundProperty);
                if (propertyValue == null) {
                    throw new RuntimeException("Cannot bind query. Property [" + foundProperty + "] not found.");
                }

                boundText = boundText.replaceAll("\\%" + foundProperty + "\\%", propertyValue);
            }
        }

        return boundText;
    }

    /**
     * Extract the version from the file name.
     *
     * @return version
     */
    public int extractVersion() {
        String filename = this.getFile();
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        filename = filename.substring(0, filename.indexOf('.'));
        return Integer.parseInt(filename);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(UpdateFile o) {
        return this.getFile().compareTo(o.getFile());
    }
}
