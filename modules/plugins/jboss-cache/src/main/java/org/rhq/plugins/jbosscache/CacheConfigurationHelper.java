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
package org.rhq.plugins.jbosscache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Utility that deals with processing JBossCache configurations 
 *
 * @author Heiko W. Rupp
 */
public class CacheConfigurationHelper {

    private Log log = LogFactory.getLog(CacheConfigurationHelper.class);

    /**
     * Write a configuration to the cache
     * @param file File to write to
     * @param config The configuration to write
     * @param mbeanName The name of the cache mbean in the file
     * @param isUpdate Are we updating a file or creating a new one
     * @throws JDOMException
     * @throws IOException
     */
    public void writeConfig(File file, Configuration config, String mbeanName, boolean isUpdate) throws JDOMException,
        IOException {

        Document doc = null;
        Element root = null;
        Element cacheMbean = null;

        String flavour = config.getSimple("Flavour").getStringValue();
        boolean isTc = false;
        if (flavour != null && flavour.startsWith("tree"))
            isTc = true;

        if (isUpdate) {
            if (!file.exists() || !file.canWrite())
                throw new IllegalStateException("Can't update file, as it is not updateaable ");
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(file);
            root = doc.getRootElement();
            cacheMbean = findComponentElement(root, mbeanName);
            if (cacheMbean == null)
                throw new IllegalStateException("File does not contain an MBean with name '" + mbeanName + "'");
        } else { // new file
            doc = new Document();
            root = new Element("server");
            doc.setRootElement(root);
            cacheMbean = new Element("mbean");
            cacheMbean.setAttribute("name", mbeanName);
            root.addContent(cacheMbean);
        }

        if (isTc)
            cacheMbean.setAttribute("code", "org.jboss.cache.TreeCache");
        else
            cacheMbean.setAttribute("code", "org.jboss.cache.PojoCache");

        // now process the individual elements
        Element depends = new Element("depends");
        depends.setText("jboss:service=TransactionManager");
        cacheMbean.addContent(depends);

        for (String propName : config.getSimpleProperties().keySet()) {

            if (propName.equals("Flavour")) // Skip this, this is to set the mbean class
                continue;

            String propVal = config.getSimple(propName).getStringValue();
            Element attribute = null;
            if (isUpdate) { // on update we need to find the existing element and update it
                attribute = findAttributeNodeWithName(cacheMbean, propName);
            }
            if (attribute == null) {
                attribute = new Element("attribute");
                attribute.setAttribute("name", propName);
                cacheMbean.addContent(attribute);
            }
            attribute.setText(propVal);
        }

        // TODO add the cluster config

        // now write the changes to disk
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(doc, fos);
            fos.flush();
        } catch (IOException ioe) {
            log.error("Can't write the config : " + ioe);
        } finally {
            if (fos != null)
                fos.close();
        }
    }

    /**
     * Find the &lt;attribute name="XX"&gt; node below base, where XX matches the input.
     * If no node is found, null is returned.
     * @param base The base element under which to search
     * @param propName The name attribute for which to search 
     * @return The element found or null
     */
    private Element findAttributeNodeWithName(Element base, String propName) {

        if (propName == null)
            return null;

        for (Object attrObj : base.getChildren("attribute")) {
            if (attrObj instanceof Element) {
                Element attr = (Element) attrObj;
                String nameAttrib = attr.getAttributeValue("name");
                if (propName.equals(nameAttrib))
                    return attr;
            }
        }
        return null;
    }

    private Element findComponentElement(Element base, String mbeanName) {

        for (Object mbeanObj : base.getChildren("mbean")) {
            if (mbeanObj instanceof Element) {
                Element mbean = (Element) mbeanObj;
                // normalize the content of 'name'
                String nameAttrib = mbean.getAttributeValue("name");
                try {
                    ObjectName on = new ObjectName(nameAttrib);
                    nameAttrib = on.getCanonicalName();
                } catch (MalformedObjectNameException e) {
                    log.warn("Can't canonicalize " + nameAttrib);
                }
                if (nameAttrib.equals(mbeanName))
                    return mbean;
            }
        }
        return null;
    }

}
