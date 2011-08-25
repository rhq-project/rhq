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
package org.rhq.plugins.modcluster.config;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Stefan Negrea
 */
public abstract class AbstractConfigurationFile {
    private File configurationFile;
    private Document document;

    public AbstractConfigurationFile(String fileName) throws ParserConfigurationException, SAXException, IOException {
        this(new File(fileName));
    }

    public AbstractConfigurationFile(File configurationFile) throws ParserConfigurationException, SAXException,
        IOException {
        this.configurationFile = configurationFile;

        loadConfiguratonFile();
    }

    abstract void setPropertyValue(String propertyName, String value);

    abstract String getPropertyValue(String propertyName);

    /**
     * @return the doc
     */
    public Document getDocument() {
        return document;
    }

    public void saveConfigurationFile() throws Exception {
        StreamResult result = new StreamResult(this.configurationFile);
        Source source = new DOMSource(this.getDocument());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    private void loadConfiguratonFile() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        this.document = docBuilder.parse(this.configurationFile);
    }
}
