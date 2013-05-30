/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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
     *
     * @param commandLine Command line arguments of the process to
     *
     * @return an Object containing host and port
     */
    public HostPort getManagementHostPort(AS7CommandLine commandLine, AS7Mode mode) {
        String portString;
        String interfaceExpression;

        String socketBindingName;

        socketBindingName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket-binding/@http");
        String socketInterface = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@interface");
        String portOffsetRaw = null;

        if (!socketInterface.isEmpty()) {
            interfaceExpression = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("//interfaces/interface[@name='" + socketInterface
                                + "']/loopback-address/@value");
            }
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@port");
        } else if (socketBindingName.isEmpty()) {
            // old AS7.0, early 7.1 style
            portString = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@port");
            String interfaceName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@interface");
            interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                                + "']/loopback-address/@value");
            }
        } else {
            // later AS7.1 and EAP6 standalone.xml
            portString = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@port");
            String interfaceName = obtainXmlPropertyViaXPath("/server/socket-binding-group/socket-binding[@name='"
                + socketBindingName + "']/@interface");
            String socketBindingGroupName = "standard-sockets";
            // /server/socket-binding-group[@name='standard-sockets']/@port-offset
            String xpathExpression =
                    "/server/socket-binding-group[@name='" + socketBindingGroupName + "']/@port-offset";
            portOffsetRaw = obtainXmlPropertyViaXPath(xpathExpression);

            // TODO the next may also be expressed differently
            interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                    + "']/inet-address/@value");
            if (interfaceExpression.isEmpty()) {
                interfaceExpression = obtainXmlPropertyViaXPath("/server/interfaces/interface[@name='" + interfaceName
                                + "']/loopback-address/@value");
            }
        }
        HostPort hp = new HostPort();

        if (!interfaceExpression.isEmpty())
            hp.host = replaceDollarExpression(interfaceExpression, commandLine, "localhost");
        else
            hp.host = "localhost"; // Fallback

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
        }

        // TODO (ips): We shouldn't need the below code if the above code that reads the port offset from the config
        //             file is working correctly.
        if (!hp.withOffset && (mode == AS7Mode.STANDALONE)) {
            // Only standalone applies the port offset to the management ports.
            String value = commandLine.getSystemProperties().get(SOCKET_BINDING_PORT_OFFSET_SYSPROP);
            if (value != null) {
                int offset = Integer.valueOf(value);
                hp.port += offset;
            }
        }

        return hp;
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

    public String getManagementSecurityRealm() {
        String realm = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");
        return realm;
    }

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

    public String getDomainApiVersion() {

        String version = document.getFirstChild().getAttributes().getNamedItem("xmlns").getTextContent();

        version = version.substring(version.lastIndexOf(':')+1);
        return version;
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
        XPath xpath = this.xpathFactory.newXPath();
        try {
            XPathExpression expr = xpath.compile(xpathExpression);
            Object result = expr.evaluate(this.document, XPathConstants.STRING);
            return result.toString();
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
