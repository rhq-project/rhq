/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.rhq.core.util.file.FileUtil;

/**
 * A JBoss CLI configuration - loaded from jboss-cli.xml
 *
 * @author Libor Zoubek
 */
public class JBossCliConfiguration {

    private final Log log = LogFactory.getLog(JBossCliConfiguration.class);

    private Document document;
    private XPathFactory xpathFactory;
    private final File jbossCliXml;
    private final ServerPluginConfiguration serverConfig;

    /**
     *
     * @param jbossCliXml absolute path to jboss-cli.xml file
     */
    public JBossCliConfiguration(File jbossCliXml, ServerPluginConfiguration serverConfig) throws Exception {
        this.jbossCliXml = jbossCliXml;
        this.serverConfig = serverConfig;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = new FileInputStream(jbossCliXml);
        try {
            this.document = builder.parse(is);
        } finally {
            is.close();
        }
        this.xpathFactory = XPathFactory.newInstance();
    }

    private Comment createComment() {
        return this.document.createComment(" added by RHQ plugin ");
    }

    /**
     * Setup SSL configuration properties by reading it from HostConfiguration (XML File) and expecting VAULT 
     * to be present. 
     * @param hostConfig
     * @return
     */
    public String configureSecurityUsingVault(HostConfiguration hostConfig) {
        Map<String, String> vaultOptions = hostConfig.getVault();
        if (vaultOptions == null) {
            return "Vault definition was not found in server configuration file";
        }
        TruststoreConfig serverIdentity = hostConfig.getServerIdentityKeystore();
        if (serverIdentity == null) {
            return "Could not find ssl configuration for management interface";
        }

        JBossCliConstants constants = getCliConstants();
        if (constants.version().compareTo("1.3") < 0) {
            return "Cannot store truststore passwords using vault, because it is not supported by this version of EAP";
        }

        Node sslNode = (Node) xpathExpression("/jboss-cli/ssl", XPathConstants.NODE);
        // clean-up existing ssl node
        if (sslNode != null) {
            this.document.getDocumentElement().removeChild(sslNode);
        }
        sslNode = addChildElement(document.getDocumentElement(), "ssl");
        sslNode.appendChild(createComment());

        Node vaultNode = this.document.createElement("vault");
        sslNode.appendChild(vaultNode);

        for (Entry<String, String> vaultOpt : vaultOptions.entrySet()) {
            Element opt = addChildElement(vaultNode, "vault-option");
            opt.setAttribute("name", vaultOpt.getKey());
            opt.setAttribute("value", vaultOpt.getValue());
        }

        addChildElement(sslNode, constants.alias(), serverIdentity.getAlias());
        addChildElement(sslNode, constants.truststore(), serverIdentity.getPath());
        // in standalone.xml vault value is referred as ${VAULT:...} we need to strip it for jboss-cli.xml
        addChildElement(sslNode, constants.truststorePassword(), stripBrackets(serverIdentity.getKeystorePassword()));

        TruststoreConfig clientKeystore = hostConfig.getClientAuthenticationTruststore();
        if (clientKeystore != null) { // 2-way authentication properties
            addChildElement(sslNode, constants.keystore(), clientKeystore.getPath());
            addChildElement(sslNode, constants.keystorePassword(), stripBrackets(clientKeystore.getKeystorePassword()));
            addChildElement(sslNode, constants.keyPassword(), stripBrackets(clientKeystore.getKeyPassword()));
        }
        return null;
    }

    /**
     * Setup SSL configuration properties by reading it from ServerPluginConfiguration and writing it as plain text
     * @return null if any configuration change has been made, otherwise message indicating reason why it was not changed
     */
    public String configureSecurity() {
        if (serverConfig.isSecure()) {
            if (serverConfig.getTruststore() != null) {
                Node sslNode = (Node) xpathExpression("/jboss-cli/ssl", XPathConstants.NODE);
                // clean-up existing ssl node
                if (sslNode != null) {
                    this.document.getDocumentElement().removeChild(sslNode);
                }
                sslNode = addChildElement(document.getDocumentElement(), "ssl");
                sslNode.appendChild(createComment());

                JBossCliConstants constants = getCliConstants();

                addChildElement(sslNode, constants.truststore(), serverConfig.getTruststore());
                addChildElement(sslNode, constants.truststorePassword(), serverConfig.getTruststorePassword());
                if (serverConfig.isClientcertAuthentication()) {
                    addChildElement(sslNode, constants.keystore(), serverConfig.getKeystore());
                    addChildElement(sslNode, constants.keystorePassword(), serverConfig.getKeystorePassword());
                    addChildElement(sslNode, constants.keyPassword(), serverConfig.getKeyPassword());
                }
                return null;
            }
            return "Truststore path is not set";
        }
        return "Secure connection is not enabled";
    }


    /**
     * 
     * @return corresponding constants based on xml namespace version
     */
    JBossCliConstants getCliConstants() {
        String ns = this.document.getDocumentElement().getAttribute("xmlns");
        String[] split = ns.split(":"); // urn:jboss:cli:1.3
        if (split.length != 4) {
            // unable to parse
            return new JBossCliConstants10();
        }
        String versionStr = split[3];
        // 1.3 and all future versions
        if (versionStr.compareTo("1.3") >= 0) {
            return new JBossCliConstants13();
        }
        if (versionStr.compareTo("1.2") == 0) {
            return new JBossCliConstants12();
        }
        if (versionStr.compareTo("1.1") == 0) {
            return new JBossCliConstants11();
        }
        return new JBossCliConstants10();
    }
    /**
     * strips ${} expression from given string
     * @param value
     * @return
     */
    private String stripBrackets(String value) {
        if (value != null && value.length() > 3 && value.startsWith("${")) {
            return value.substring(2, value.length() - 1);
        }
        return value;
    }

    /**
     * Setup controller host and port defaults
     * @return null if any configuration change has been made, otherwise message indicating reason why it was not changed
     */
    public String configureDefaultController() {
        Node ctrlNode = this.document.createElement("default-controller");
        ctrlNode.appendChild(createComment());
        addChildElement(ctrlNode, "host", serverConfig.getHostname());
        addChildElement(ctrlNode, "port", String.valueOf(serverConfig.getPort()));
        Node existing = (Node) xpathExpression("/jboss-cli/default-controller", XPathConstants.NODE);
        if (existing != null) {
            this.document.getDocumentElement().replaceChild(ctrlNode, existing);
        }
        return null;
    }

    /**
     * Write changes to file - flushes changes made by i.e. {@link #configureSecurity()}. This also creates backup of the 
     * file (appends ".original" suffix)
     * @throws Exception
     */
    public void writeToFile() throws Exception {
        if (!jbossCliXml.canWrite()) {
            throw new IOException(jbossCliXml + " is not writable");
        }
        File backup = new File(jbossCliXml.getParentFile(), jbossCliXml.getName() + ".original");
        try {
            log.debug("Backup " + jbossCliXml + " to " + backup);
            FileUtil.copyFile(jbossCliXml, backup);
        } catch (IOException ex) {
            throw new IOException("Could not create backup file " + backup, ex);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        //initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(this.jbossCliXml);
        DOMSource source = new DOMSource(this.document);
        transformer.transform(source, result);
    }

    private void addChildElement(Node parent, String tagName, String textContent) {
        if (tagName != null && textContent != null && !textContent.isEmpty()) {
            Node element = this.document.createElement(tagName);
            element.setTextContent(textContent);
            parent.appendChild(element);
        }
    }

    private Element addChildElement(Node parent, String tagName) {
        Element element = this.document.createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    public String obtainXmlPropertyViaXPath(String xpathExpression) {
        return (String) xpathExpression(xpathExpression, XPathConstants.STRING);
    }

    private Object xpathExpression(String xpathExpression, QName returnType) {
        XPath xpath = this.xpathFactory.newXPath();
        try {
            XPathExpression expr = xpath.compile(xpathExpression);
            return expr.evaluate(this.document, returnType);
        } catch (XPathExpressionException e) {
            log.error("Evaluation of XPath expression failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * tagNames in jboss-cli.xml were changing overtime. We need to provide the right set of tagNames for 
     * each known version of jboss-cli.xml schema
     * @author lzoubek
     *
     */
    static interface JBossCliConstants {
        String version();

        String truststore();

        String truststorePassword();

        String keystore();

        String keystorePassword();

        String alias();

        String keyPassword();
    }

    private static class JBossCliConstants10 implements JBossCliConstants {

        @Override
        public String version() {
            return "1.0";
        }

        @Override
        public String truststore() {
            return "trustStore";
        }

        @Override
        public String truststorePassword() {
            return "trustStorePassword";
        }

        @Override
        public String keystore() {
            return "keyStore";
        }

        @Override
        public String keystorePassword() {
            return "keyStorePassword";
        }

        @Override
        public String alias() {
            return null;
        }

        @Override
        public String keyPassword() {
            return null;
        }
    }

    private static class JBossCliConstants11 extends JBossCliConstants10 {
        @Override
        public String version() {
            return "1.1";
        }

        @Override
        public String truststore() {
            return "trust-store";
        }

        @Override
        public String truststorePassword() {
            return "trust-store-password";
        }

        @Override
        public String keystore() {
            return "key-store";
        }

        @Override
        public String keystorePassword() {
            return "key-store-password";
        }

        @Override
        public String alias() {
            return "alias";
        }

        @Override
        public String keyPassword() {
            return "key-password";
        }
    }

    static class JBossCliConstants12 extends JBossCliConstants11 {
        @Override
        public String version() {
            return "1.2";
        }
    }

    static class JBossCliConstants13 extends JBossCliConstants12 {
        @Override
        public String version() {
            return "1.3";
        }
    }
}
