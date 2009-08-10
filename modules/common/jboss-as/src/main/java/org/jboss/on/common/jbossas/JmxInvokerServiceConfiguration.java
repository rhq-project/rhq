/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.jboss.on.common.jbossas;

import java.io.File;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.jetbrains.annotations.Nullable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ian Springer
 */
public class JmxInvokerServiceConfiguration {
    private final Log log = LogFactory.getLog(this.getClass());

    private String source;
    private String securityDomain;

    public JmxInvokerServiceConfiguration(File jmxInvokerServiceXmlFile) throws Exception {
        this.source = jmxInvokerServiceXmlFile.getPath();
        DocumentBuilder docBuilder = getDocumentBuilder();
        Document doc = docBuilder.parse(jmxInvokerServiceXmlFile);
        parseDocument(doc);
    }

    public JmxInvokerServiceConfiguration(InputStream jmxInvokerServiceXmlInputStream) throws Exception {
        this.source = jmxInvokerServiceXmlInputStream.toString();
        DocumentBuilder builder = getDocumentBuilder();
        Document doc = builder.parse(jmxInvokerServiceXmlInputStream);
        parseDocument(doc);
    }

    @Nullable
    public String getSecurityDomain() {
        return this.securityDomain;
    }

    private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        return docBuilderFactory.newDocumentBuilder();
    }

    private void parseDocument(Document doc) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        // Use a bunch of incremental XPath expressions rather than one big XPath expression,
        // so we can provide better log messages.
        Node invokerMBeanNode = (Node)xPath.evaluate("//server/mbean[@name = 'jboss.jmx:type=adaptor,name=Invoker']",
                    doc, XPathConstants.NODE);
        if (invokerMBeanNode == null) {
            // This is uncommon and therefore suspicious.
            log.warn("'jboss.jmx:type=adaptor,name=Invoker' mbean not found while parsing '" + this.source + "'.");
            return;
        }
        Node invokeOperationNode = (Node)xPath.evaluate("xmbean/operation[name = 'invoke']",
                    invokerMBeanNode, XPathConstants.NODE);
        if (invokeOperationNode == null) {
            // This is uncommon and therefore suspicious.
            log.warn("'invoke' operation not found for 'jboss.jmx:type=adaptor,name=Invoker' mbean while parsing '" + this.source + "'.");
            return;
        }
        Node interceptorsNode = (Node)xPath.evaluate("descriptors/interceptors",
                    invokeOperationNode, XPathConstants.NODE);
        if (interceptorsNode == null) {
            log.debug("No interceptors are defined for 'invoke' operation for 'jboss.jmx:type=adaptor,name=Invoker' mbean while parsing '" + this.source + "'.");
            return;
        }
        Node authenticationInterceptorNode = (Node)xPath.evaluate("interceptor[@code = 'org.jboss.jmx.connector.invoker.AuthenticationInterceptor']",
                    interceptorsNode, XPathConstants.NODE);
        if (authenticationInterceptorNode == null) {
            // This is normal. It just means the authentication interceptor isn't enabled (typically it's commented out).
            return;
        }
        // e.g. "java:/jaas/jmx-console"
        String securityDomainJndiName;
        try {
            securityDomainJndiName = xPath.evaluate("@securityDomain", authenticationInterceptorNode);
        }
        catch (XPathExpressionException e) {
            throw new Exception("'securityDomain' attribute not found on 'org.jboss.jmx.connector.invoker.AuthenticationInterceptor' interceptor while parsing '" + this.source + "'.");            
        }
        // e.g. "jmx-console"
        this.securityDomain = securityDomainJndiName.replaceFirst("^java:/jaas/", "");
    }
}
