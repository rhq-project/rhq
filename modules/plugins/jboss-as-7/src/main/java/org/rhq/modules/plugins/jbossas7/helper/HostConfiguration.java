/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.modules.plugins.jbossas7.helper;

import static org.rhq.core.util.StringUtil.EMPTY_STRING;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.core.pluginapi.util.CommandLineOption;
import org.rhq.modules.plugins.jbossas7.AS7CommandLine;
import org.rhq.modules.plugins.jbossas7.AS7Mode;

/**
 * A host configuration - loaded from either standalone.xml or host.xml.
 *
 * @author Heiko Rupp
 */
public class HostConfiguration {

    public static final int DEFAULT_MGMT_PORT = 9990;
    public static final int DEFAULT_NATIVE_PORT = 9999;
    private static final String BIND_ADDRESS_MANAGEMENT_SYSPROP = "jboss.bind.address.management";
    private static final String DOMAIN_MASTER_ADDRESS_SYSPROP = "jboss.domain.master.address";
    private static final String DOMAIN_MASTER_PORT_SYSPROP = "jboss.domain.master.port";
    private static final String SOCKET_BINDING_PORT_OFFSET_SYSPROP = "jboss.socket.binding.port-offset";

    private static final CommandLineOption BIND_ADDRESS_MANAGEMENT_OPTION = new CommandLineOption("bmanagement", null);
    private static final CommandLineOption MASTER_ADDRESS_OPTION = new CommandLineOption(null, "master-address");
    private static final CommandLineOption MASTER_PORT_OPTION = new CommandLineOption(null, "master-port");

    private final Log log = LogFactory.getLog(HostConfiguration.class);

    private Document document;
    private XPathFactory xpathFactory;

    /**
     *
     * @param hostXmlFile absolute path to standalone.xml or host.xml file
     */
    public HostConfiguration(File hostXmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream is = new FileInputStream(hostXmlFile);
        try {
            this.document = builder.parse(is);
        } finally {
            is.close();
        }

        this.xpathFactory = XPathFactory.newInstance();
    }
    /**
     * Try to obtain the management IP and port from the already parsed host.xml or standalone.xml
     *
     * @param commandLine Command line arguments of the process to
     *
     * @return an Object containing host and port
     */
    public HostPort getManagementHostPort(AS7CommandLine commandLine, AS7Mode mode) {
        // There are three ways to configure the http(s) management endpoint
        //
        // 1. Standalone servers favored style (socket-binding style)
        // The socket-binding node attribute is *either* 'https' *or* 'http'
        //     <management>
        //         <management-interfaces>
        //             <http-interface security-realm="ManagementRealm">
        //                 <socket-binding https="management-https"/>
        //             </http-interface>
        //         </management-interfaces>
        //     </management>
        //
        // 2. Host controllers style, unfavored standalone servers style (socket style)
        //     <management>
        //         <management-interfaces>
        //             <http-interface security-realm="ManagementRealm">
        //                 <socket interface="management" port="9990" secure-port="9443"/>
        //             </http-interface>
        //         </management-interfaces>
        //     </management>
        //
        //
        // 3. Very old and deprecated style (early as7 style)
        //     <management>
        //         <management-interfaces>
        //             <http-interface security-realm="ManagementRealm" interface="management" port="9990" secure-port="9443"/>
        //         </management-interfaces>
        //     </management>

        String portString;
        String interfaceExpression;
        String socketBindingName;
        String portOffsetRaw = null;
        boolean isSecure = false;
        // detect http interface
        if (findMatchingElements("//management/management-interfaces/http-interface/socket-binding").getLength() != 0) {
            // This is case 1
            socketBindingName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket-binding/@https");
            if (!socketBindingName.isEmpty()) {
                isSecure = true;
            } else {
                socketBindingName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket-binding/@http");
            }
            portString = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@port");
            String interfaceName = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@interface");
            String xpathExpression = "/server/socket-binding-group/@port-offset";
            portOffsetRaw = obtainXmlPropertyViaXPath(xpathExpression);

            interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                    + "']/loopback-address/@value");
            }
        } else if (findMatchingElements("//management/management-interfaces/http-interface/socket").getLength() != 0) {
            // This is case 2
            String socketInterface = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@interface");
            interfaceExpression = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                    + "']/loopback-address/@value");
            }
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@secure-port");
            if (!portString.isEmpty()) {
                isSecure = true;
            } else {
                portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@port");
            }
        } else {
            // This is case 3
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@secure-port");
            if (!portString.isEmpty()) {
                isSecure = true;
            } else {
                portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@port");
            }
            String interfaceName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@interface");
            interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                    + "']/loopback-address/@value");
            }
        }

        HostPort hp = new HostPort();
        hp.isSecure = isSecure;

        if (!interfaceExpression.isEmpty()) {
            hp.host = replaceDollarExpression(interfaceExpression, commandLine, "127.0.0.1");
        } else {
            hp.host = "127.0.0.1"; // Fallback
        }

        hp.port = 0;
        if (portString != null && !portString.isEmpty()) {
            String tmp = replaceDollarExpression(portString, commandLine, String.valueOf(DEFAULT_MGMT_PORT));
            hp.port = Integer.valueOf(tmp);
        }

        if (portOffsetRaw != null && !portOffsetRaw.isEmpty()) {
            String portOffsetString = replaceDollarExpression(portOffsetRaw, commandLine, "0");
            Integer portOffset = Integer.valueOf(portOffsetString);
            hp.port += portOffset;
            hp.withOffset = true;
        } else if (mode == AS7Mode.STANDALONE) {
            // On standalone servers, offset may also be set with a system property
            String value = commandLine.getSystemProperties().get(SOCKET_BINDING_PORT_OFFSET_SYSPROP);
            if (value != null) {
                int offset = Integer.valueOf(value);
                hp.port += offset;
            }
        }

        return hp;
    }

    /**
     * Try to obtain the management IP and port from the already parsed host.xml or standalone.xml
     *
     * @param commandLine Command line arguments of the process to
     *
     * @return an Object containing host and port
     */
    public HostPort getNativeHostPort(AS7CommandLine commandLine, AS7Mode mode) {
        // There are three ways to configure the http(s) management endpoint
        //
        // 1. Standalone servers favored style (socket-binding style)
        //     <management>
        //         <management-interfaces>
        //             <native-interface security-realm="ManagementRealm">
        //                 <socket-binding native="management"/>
        //             </native-interface>
        //         </management-interfaces>
        //     </management>
        //
        // 2. Host controllers style, unfavored standalone servers style (socket style)
        //     <management>
        //         <management-interfaces>
        //             <native-interface security-realm="ManagementRealm">
        //                 <socket interface="management" port="9999" />
        //             </native-interface>
        //         </management-interfaces>
        //     </management>
        //
        //
        // 3. Very old and deprecated style (early as7 style)
        //     <management>
        //         <management-interfaces>
        //             <native-interface security-realm="ManagementRealm" interface="management" port="9999" />
        //         </management-interfaces>
        //     </management>

        String portString;
        String interfaceExpression;
        String socketBindingName;
        String portOffsetRaw = null;
        boolean isSecure = false;
        // detect http interface
        if (findMatchingElements("//management/management-interfaces/native-interface/socket-binding").getLength() != 0) {
            // This is case 1
            socketBindingName = obtainXmlPropertyViaXPath("//management/management-interfaces/native-interface/socket-binding/@native");
            portString = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@port");
            String interfaceName = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@interface");
            String xpathExpression = "/server/socket-binding-group/@port-offset";
            portOffsetRaw = obtainXmlPropertyViaXPath(xpathExpression);

            interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                    + "']/loopback-address/@value");
            }
        } else if (findMatchingElements("//management/management-interfaces/native-interface/socket").getLength() != 0) {
            // This is case 2
            String socketInterface = obtainXmlPropertyViaXPath("//management/management-interfaces/native-interface/socket/@interface");
            interfaceExpression = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                    + "']/loopback-address/@value");
            }
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/native-interface/socket/@port");
        } else {
            // This is case 3
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/native-interface/@secure-port");
            if (!portString.isEmpty()) {
                isSecure = true;
            } else {
                portString = obtainXmlPropertyViaXPath("//management/management-interfaces/native-interface/@port");
            }
            String interfaceName = obtainXmlPropertyViaXPath("//management/management-interfaces/native-interface/@interface");
            interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                    + "']/loopback-address/@value");
            }
        }

        HostPort hp = new HostPort();

        if (!interfaceExpression.isEmpty()) {
            hp.host = replaceDollarExpression(interfaceExpression, commandLine, "127.0.0.1");
        } else {
            hp.host = "127.0.0.1"; // Fallback
        }

        hp.port = 0;
        if (portString != null && !portString.isEmpty()) {
            String tmp = replaceDollarExpression(portString, commandLine, String.valueOf(DEFAULT_NATIVE_PORT));
            hp.port = Integer.valueOf(tmp);
        }

        if (portOffsetRaw != null && !portOffsetRaw.isEmpty()) {
            String portOffsetString = replaceDollarExpression(portOffsetRaw, commandLine, "0");
            Integer portOffset = Integer.valueOf(portOffsetString);
            hp.port += portOffset;
            hp.withOffset = true;
        } else if (mode == AS7Mode.STANDALONE) {
            // On standalone servers, offset may also be set with a system property
            String value = commandLine.getSystemProperties().get(SOCKET_BINDING_PORT_OFFSET_SYSPROP);
            if (value != null) {
                int offset = Integer.valueOf(value);
                hp.port += offset;
            }
        }

        return hp;
    }

    private NodeList findMatchingElements(String expression) {
        try {
            XPathExpression xPathExpression = xpathFactory.newXPath().compile(expression);
            return (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Try to determine the host name - that is the name of a standalone server or a
     * host in domain mode by looking at the standalone.xml/host.xml files
     * @return server name
     */
    public String getHostName() {
        String hostName = this.document.getDocumentElement().getAttribute("name");
        return hostName;
    }

    /**
     * Try to obtain the domain controller's location from looking at host.xml
     * @return host and port of the domain controller
     */
    public HostPort getDomainControllerHostPort(AS7CommandLine commandLine) {
        // first check remote, as we can't distinguish between a missing local element or
        // an empty one, which is the default
        String remoteHost = obtainXmlPropertyViaXPath("/host/domain-controller/remote/@host");
        String portString = obtainXmlPropertyViaXPath("/host/domain-controller/remote/@port");

        HostPort hp;
        if (!remoteHost.isEmpty() && !portString.isEmpty()) {
            // remote domain controller
            hp = new HostPort(false);
            hp.host = replaceDollarExpression(remoteHost, commandLine, "localhost");
            portString = replaceDollarExpression(portString, commandLine, "9999");
            hp.port = Integer.parseInt(portString);
        } else {
            // local domain controller
            hp = new HostPort(true);
            hp.port = 9999;
        }

        return hp;
    }

    /**
     * 
     * @return true if $local authentication is the only authentication configured for realm associated to native management interface
     */
    public boolean isNativeLocalOnly() {
        String nativeRealm = getManagementSecurityRealm();
        if (nativeRealm != null) {
            XPath xpath = this.xpathFactory.newXPath();
            try {
                XPathExpression expr = xpath.compile("count(//management/security-realms/security-realm[@name='"
                    + nativeRealm + "']/authentication[count(local) = count(*)]) = 1");
                return (Boolean) expr.evaluate(this.document, XPathConstants.BOOLEAN);
            } catch (XPathExpressionException e) {
                log.error("Evaluation of XPath expression failed: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public String getManagementSecurityRealm() {
        String realm = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        return realm;
    }

    /**
     * read server SSL key-store information
     * @return null if not present
     */
    public TruststoreConfig getServerIdentityKeystore() {
        String mgmtRealm = getManagementSecurityRealm();
        if (mgmtRealm != null) {
            Node keyStoreNode = (Node) xpathExpression("//management/security-realms/security-realm[@name='"
                + mgmtRealm + "']/server-identities/ssl/keystore", XPathConstants.NODE);
            return TruststoreConfig.fromXmlNode(keyStoreNode);
        }
        return null;
    }

    /**
     * read trust-store information for 2-way authentication
     * @return null if not present
     */
    public TruststoreConfig getClientAuthenticationTruststore() {
        String mgmtRealm = getManagementSecurityRealm();
        if (mgmtRealm != null) {
            Node keyStoreNode = (Node) xpathExpression("//management/security-realms/security-realm[@name='"
                + mgmtRealm + "']/authentication/truststore", XPathConstants.NODE);
            return TruststoreConfig.fromXmlNode(keyStoreNode);
        }
        return null;
    }

    /**
     * read vault configuration
     * @return vault configuration (key,value of vault-options) or null if vault is not present
     */
    public Map<String, String> getVault() {
        
        Node vaultNode = (Node) xpathExpression("//vault", XPathConstants.NODE);
        if (vaultNode == null) {
            return null;
        }
        Map<String, String> vault = new LinkedHashMap<String, String>();
        NodeList vaultOptions = (NodeList) xpathExpression("//vault/vault-option", XPathConstants.NODESET);
        for (int i=0; i< vaultOptions.getLength(); i++) {
            Node option = vaultOptions.item(i);
            vault.put(option.getAttributes().getNamedItem("name").getNodeValue(),
                option.getAttributes().getNamedItem("value").getNodeValue());
        }
        return vault;
    }

    /**
     * @Deprecated use {@link HostConfiguration#getSecurityPropertyFile(ServerPluginConfiguration, String)} instead
     */
    public File getSecurityPropertyFile(File baseDir, AS7Mode mode, String realm) {
        String fileName = obtainXmlPropertyViaXPath("//security-realms/security-realm[@name='" + realm
            + "']/authentication/properties/@path");
        String relDir = obtainXmlPropertyViaXPath("//security-realms/security-realm[@name='" + realm
            + "']/authentication/properties/@relative-to");

        String dmode;
        if (mode == AS7Mode.STANDALONE)
            dmode = "server";
        else
            dmode = "domain";

        File configDir;
        if (relDir.equals("jboss." + dmode + ".config.dir")) {
            configDir = new File(baseDir, "configuration");
        } else {
            configDir = new File(relDir);
        }
        File securityPropertyFile = new File(configDir, fileName);

        return securityPropertyFile;
    }

    public File getSecurityPropertyFile(ServerPluginConfiguration configuration, String realm) {
        String fileName = obtainXmlPropertyViaXPath("//security-realms/security-realm[@name='" + realm
            + "']/authentication/properties/@path");
        String relDir = obtainXmlPropertyViaXPath("//security-realms/security-realm[@name='" + realm
            + "']/authentication/properties/@relative-to");

        if (relDir == null || relDir.isEmpty()) {
            return new File(fileName);
        }
        File relativeDir = configuration.getPath(relDir);
        if (relativeDir != null) {
            return new File(relativeDir, fileName);
        } else {
            log.warn("Cannot resolve security property file based on path=" + fileName + " relative-to=" + relDir);
            return new File(fileName);
        }
    }

    public String getDomainApiVersion() {
        // Look for the first child node of type element (<host> in domain mode or <server> in standalone mode)
        // We can't just call getFirstChild because first child could be a node of type comment
        for (Node childNode = document.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == ELEMENT_NODE) {
                String xmlns = childNode.getAttributes().getNamedItem("xmlns").getTextContent();
                return xmlns.substring(xmlns.lastIndexOf(':') + 1);
            }
        }
        return EMPTY_STRING;
    }

    /**
     * Run the passed xpathExpression on the prepopulated hostXml document and
     * return the target element or attribute as a String.
     * @param xpathExpression XPath Expression to evaluate
     * @return String value of the Element or Attribute the XPath was pointing to.
     *     Null in case the xpathExpression could not be evaluated.
     * @throws IllegalArgumentException if hostXml is null
     */
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
     * Check if the passed value has an expression in the form of ${var} or ${var:default},
     * try to resolve it. Resolution is done by looking at the command line to see if
     * there are -bmanagement or -Djboss.bind.address.management arguments present
     *
     * @param value a hostname or hostname expression
     * @param commandLine The command line from the process
     * @param lastResort fall back to this value if the value could not be found on the command line and
     *                   the expression did not specify a default value
     * @return resolved value
     */
    protected String replaceDollarExpression(String value, AS7CommandLine commandLine, String lastResort) {
        if (!value.contains("${")) {
            return value;
        }

        // remove ${ }
        value = value.substring(2, value.length() - 1);
        String fallback = lastResort;
        String expression;
        if (value.contains(":")) {
            int i = value.indexOf(":");
            expression = value.substring(0, i);
            fallback = ((i + 1) < value.length()) ? value.substring(i + 1) : "";
        } else {
            expression = value;
        }
        String resolvedValue = null;
        if (expression.equals(BIND_ADDRESS_MANAGEMENT_SYSPROP)) {
            // special case: mgmt address can be specified via either -bmanagement= or -Djboss.bind.address.management=
            resolvedValue = commandLine.getClassOption(BIND_ADDRESS_MANAGEMENT_OPTION);
        } else if (expression.equals(DOMAIN_MASTER_ADDRESS_SYSPROP)) {
            // special case: DC address can be specified via either --master-address= or -Djboss.domain.master.address=
            resolvedValue = commandLine.getClassOption(MASTER_ADDRESS_OPTION);
        } else if (expression.equals(DOMAIN_MASTER_PORT_SYSPROP)) {
            // special case: DC port can be specified via either --master-port= or -Djboss.domain.master.port=
            resolvedValue = commandLine.getClassOption(MASTER_PORT_OPTION);
        }

        if (resolvedValue == null) {
            resolvedValue = commandLine.getSystemProperties().get(expression);
        }
        if (resolvedValue == null) {
            resolvedValue = fallback;
        }

        return resolvedValue;
    }

}
