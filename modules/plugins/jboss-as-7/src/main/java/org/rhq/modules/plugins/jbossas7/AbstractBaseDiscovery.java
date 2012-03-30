/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

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
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;

/**
 * Abstract base class for some discovery related functionality - especially
 * in the area of processes and host.xml
 * @author Heiko W. Rupp
 */
public abstract class AbstractBaseDiscovery<T extends ResourceComponent<?>> implements ResourceDiscoveryComponent<T> {

    private static final String HOME_DIR_SYSPROP = "jboss.home.dir";
    private static final String BIND_ADDRESS_MANAGEMENT_SYSPROP = "jboss.bind.address.management";

    private AS7CommandLineOption BIND_ADDRESS_MANAGEMENT_OPTION = new AS7CommandLineOption("bmanagement", null);
    
    static final int DEFAULT_MGMT_PORT = 9990;
    private static final String JBOSS_AS_PREFIX = "jboss-as-";
    static final String CALL_READ_STANDALONE_OR_HOST_XML_FIRST = "hostXml is null. You need to call 'readStandaloneOrHostXml' first.";
    private static final String SOCKET_BINDING_PORT_OFFSET_SYSPROP = "jboss.socket.binding.port-offset";
    protected Document hostXml;
    protected final Log log = LogFactory.getLog(this.getClass());
    private static final String JBOSS_EAP_PREFIX = "jboss-eap-";
    public static final String EAP = "EAP";
    public static final String JDG = "JDG";
    public static final String EAP_PREFIX = EAP + " ";
    public static final String JDG_PREFIX = JDG + " ";
    private XPathFactory factory;

    protected AbstractBaseDiscovery() {
        synchronized (this) {
            factory = XPathFactory.newInstance();
        }
    }

    protected void readStandaloneOrHostXmlFromFile(String hostXmlFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            try {
                hostXml = builder.parse(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Try to obtain the management IP and port from the already parsed host.xml or standalone.xml
     * @return an Object containing host and port
     * @see #readStandaloneOrHostXmlFromFile(String) for how to obtain the parsed xml
     * @param commandLine Command line arguments of the process to
     */
    protected HostPort getManagementHostPortFromHostXml(String[] commandLine) {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String portString;
        String interfaceExpression;

        String socketBindingName;

        socketBindingName = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket-binding/@http");
        String socketInterface = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/socket/@interface");
        String portOffset = null;

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
            portOffset = obtainXmlPropertyViaXPath(xpathExpression);

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

        if (portOffset!=null && !portOffset.isEmpty()) {
            String tmp = replaceDollarExpression(portOffset, commandLine, "0");
            Integer offset = Integer.valueOf(tmp);
            hp.port += offset;
            hp.withOffset=true;
        }
        return hp;
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
    private String replaceDollarExpression(String value, String[] commandLine, String lastResort) {
        if (!value.contains("${"))
            return value;

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
            resolvedValue = getOptionFromCommandLine(commandLine, BIND_ADDRESS_MANAGEMENT_OPTION);
        }
        if (resolvedValue == null) {
            resolvedValue = getSystemPropertyFromCommandLine(commandLine, expression);
        }
        if (resolvedValue == null) {
            resolvedValue = fallback;
        }

        return resolvedValue;
    }

    /**
     * Try to determine the host name - that is the name of a standalone server or a
     * host in domain mode by looking at the standalone.xml/host.xml files
     * @return server name
     */
    protected String findHostName() {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String hostName = hostXml.getDocumentElement().getAttribute("name");
        return hostName;
    }

    /**
     * Try to obtain the domain controller's location from looking at host.xml
     * @return host and port of the domain controller
     */
    protected HostPort getHostPortFromHostXml() {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        // first check remote, as we can't distinguish between a missing local element or
        // and empty one which is the default
        String remoteHost = obtainXmlPropertyViaXPath("/host/domain-controller/remote/@host");
        String portString = obtainXmlPropertyViaXPath("/host/domain-controller/remote/@port");

        HostPort hp;
        if (!remoteHost.isEmpty() && !portString.isEmpty()) {
            hp = new HostPort(false);
            hp.host = remoteHost;
            hp.port = Integer.parseInt(portString);
        } else {
            hp = new HostPort(true);
            hp.port = 9999;
        }

        return hp;

    }

    String getManagementSecurityRealmFromHostXml() {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        String realm = obtainXmlPropertyViaXPath("//management/management-interfaces/http-interface/@security-realm");

        return realm;
    }

    String getSecurityPropertyFileFromHostXml(File baseDir, AS7Mode mode, String realm) {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

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

        return securityPropertyFile.getPath();
    }

    protected File getHomeDir(ProcessInfo processInfo) {
        String home = getSystemPropertyFromCommandLine(processInfo.getCommandLine(), HOME_DIR_SYSPROP,
                processInfo.getEnvironmentVariable("JBOSS_HOME"));
        File homeDir = new File(home);
        if (!homeDir.isAbsolute()) {
            if (processInfo.getExecutable() == null) {
                throw new RuntimeException(HOME_DIR_SYSPROP + " for AS7 process " + processInfo
                        + " is a relative path, and the RHQ Agent process does not have permission to resolve it.");
            }
            String cwd = processInfo.getExecutable().getCwd();
            homeDir = new File(cwd, home);
        }
        
        return new File(FileUtils.getCanonicalPath(homeDir.getPath()));
    }

    protected String determineServerVersionFromHomeDir(String homeDir) {
        String version;
        String tmp = homeDir.substring(homeDir.lastIndexOf("/") + 1);
        if (tmp.startsWith(JBOSS_AS_PREFIX)) {
            version = tmp.substring(JBOSS_AS_PREFIX.length());
        } else if (tmp.startsWith(JBOSS_EAP_PREFIX)) {
            version = tmp.substring(JBOSS_EAP_PREFIX.length());
        } else {
            version = homeDir.substring(homeDir.lastIndexOf("-") + 1);
        }
        return version;
    }

    /**
     * Run the passed xpathExpression on the prepopulated hostXml document and
     * return the target element or attribute as a String.
     * @param xpathExpression XPath Expression to evaluate
     * @return String value of the Element or Attribute the XPath was pointing to.
     *     Null in case the xpathExpression could not be evaluated.
     * @throws IllegalArgumentException if hostXml is null
     *
     */
    protected String obtainXmlPropertyViaXPath(String xpathExpression) {
        if (hostXml == null)
            throw new IllegalArgumentException(CALL_READ_STANDALONE_OR_HOST_XML_FIRST);

        XPath xpath = factory.newXPath();
        try {
            XPathExpression expr = xpath.compile(xpathExpression);

            Object result = expr.evaluate(hostXml, XPathConstants.STRING);

            return result.toString();
        } catch (XPathExpressionException e) {
            log.error("Evaluation XPath expression failed: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    protected static String getSystemPropertyFromCommandLine(String[] commandLine, String systemPropertyName) {
        return getSystemPropertyFromCommandLine(commandLine, systemPropertyName, null);
    }

    @Nullable
    protected static String getSystemPropertyFromCommandLine(String[] commandLine, String systemPropertyName,
                                                             String defaultValue) {
        String prefix = "-D" + systemPropertyName;
        String prefixWithEqualsSign = prefix + "=";
        for (String arg : commandLine) {            
            if (arg.startsWith(prefixWithEqualsSign)) {
                return (prefixWithEqualsSign.length() < arg.length()) ?
                    arg.substring(prefixWithEqualsSign.length()) : "";
            } else if (arg.equals(prefix)) {
                return "";
            }
        }
        return defaultValue;
    }

    @Nullable
    protected static String getOptionFromCommandLine(String[] commandLine, AS7CommandLineOption option) {
        String shortOptionPrefix;
        String shortOption;
        if (option.getShortName() != null) {
            shortOption = "-" + option.getShortName();
            shortOptionPrefix = shortOption + "=";
        } else {
            shortOption = null;
            shortOptionPrefix = null;
        }
        String longOptionPrefix;
        if (option.getLongName() != null) {
            longOptionPrefix = "--" + option.getLongName() + "=";
        } else {
            longOptionPrefix = null;
        }
        for (int i = 0, commandLineLength = commandLine.length; i < commandLineLength; i++) {
            String arg = commandLine[i];
            if (option.getShortName() != null) {
                if (arg.startsWith(shortOptionPrefix)) {
                    return (shortOptionPrefix.length() < arg.length()) ? arg.substring(shortOptionPrefix.length()) : "";
                } else if (arg.equals(shortOption)) {
                    return (i != (commandLineLength - 1)) ? commandLine[i + 1] : "";
                }
            }
            if (option.getLongName() != null) {
                if (arg.startsWith(longOptionPrefix)) {
                    return (longOptionPrefix.length() < arg.length()) ? arg.substring(longOptionPrefix.length()) : "";
                }
            }
        }
        // If we reached here, the option wasn't on the command line.

        return null;
    }
    
    protected HostPort checkForSocketBindingOffset(HostPort managementPort, String[] commandLine) {
        String value = getSystemPropertyFromCommandLine(commandLine, SOCKET_BINDING_PORT_OFFSET_SYSPROP);
        if (value != null) {
            int offset = Integer.valueOf(value);
            managementPort.port += offset;
        }

        return managementPort;
    }

    /**
     * Helper class that holds information about the host,port tuple
     */
    protected static class HostPort {
        String host;
        int port;
        boolean isLocal = true;
        boolean withOffset = false;

        public HostPort() {
            host = "localhost";
            port = DEFAULT_MGMT_PORT;
            isLocal = true;
        }

        public HostPort(boolean local) {
            this();
            isLocal = local;
        }

        @Override
        public String toString() {
            return "HostPort{" + "host='" + host + '\'' + ", port=" + port + ", isLocal=" + isLocal + '}';
        }
    }

}
