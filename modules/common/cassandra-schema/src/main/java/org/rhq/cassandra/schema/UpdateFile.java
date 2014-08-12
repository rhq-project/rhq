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
import java.util.List;

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
     * @return All of the {@link Step steps} in the file in declaration order.
     */
    public List<Step> getOrderedSteps() {
        return getStepNodes();
    }

    /**
     * Retrieve unbound list of steps from the file in declaration order.
     *
     * @return unbound list of steps.
     */
    private List<Step> getStepNodes() {
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

            List<Step> stepList = new ArrayList<Step>();
            for (int index = 0; index < updateStepElements.getLength(); index++) {
                Node updateStepElement = updateStepElements.item(index);
                Step step;
                if (STEP_ELEMENT.equals(updateStepElement.getNodeName()) && updateStepElement.getTextContent() != null) {
                    Node classAttribute = updateStepElement.getAttributes().getNamedItem("class");
                    if (classAttribute == null || classAttribute.getNodeValue().equals(CQLStep.class.getSimpleName()) ||
                        classAttribute.getNodeValue().toUpperCase().equals("CQL")) {
                        step = new CQLStep(updateStepElement.getTextContent());
                    } else {
                        String stepClass = classAttribute.getNodeValue();
                        Class<? extends Step> clazz = (Class<? extends Step>) Class.forName(stepClass);
                        step = clazz.newInstance();
                    }
                    stepList.add(step);
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

    @Override
    public String toString() {
        return this.getFile().toString();
    }
}
