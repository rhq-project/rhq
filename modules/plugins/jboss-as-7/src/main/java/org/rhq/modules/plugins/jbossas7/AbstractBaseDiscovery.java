/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.system.ProcessInfo;

/**
 * Abstract base class for some discovery related functionality - especially
 * in the area of processes and host.xml
 * @author Heiko W. Rupp
 */
public abstract class AbstractBaseDiscovery<T extends ResourceComponent> implements ResourceDiscoveryComponent<T> {
    static final String DORG_JBOSS_BOOT_LOG_FILE = "-Dorg.jboss.boot.log.file=";
    private static final String DJBOSS_SERVER_HOME_DIR = "-Djboss.home.dir";
    static final int DEFAULT_MGMT_PORT = 9990;
    protected Document hostXml;
    protected final Log log = LogFactory.getLog(this.getClass());


    /**
     * Read the host.xml or standalone.xml file depending on isDomainMode. If isDomainMode is true,
     * host.xml is read, otherwise standalone.xml.
     * The xml file content is stored in the variable hostXml for future use.
     * @param processInfo Process info to determine the base file location
     * @param isDomainMode Indiates if host.xml should be read (true) or standalone.xml (false)
     */
    protected void readStandaloneOrHostXml(ProcessInfo processInfo, boolean isDomainMode) {
        String hostXmlFile = getHostXmlFileLocation(processInfo,isDomainMode);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            hostXml = builder.parse(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
    }

    /**
     * Determine the server home (=base) directory by parsing the passed command line
     * @param commandLine command line arguments of the process
     * @return The home dir if found or empty string otherwise
     */
    String getHomeDirFromCommandLine(String[] commandLine) {
            for (String line: commandLine) {
                if (line.startsWith(DJBOSS_SERVER_HOME_DIR))
                    return line.substring(DJBOSS_SERVER_HOME_DIR.length()+1);
            }
            return "";
    }

    /**
     * Determine the location of the boot log file of the server by parsing the command line
     * @param commandLine command line arguments of the process
     * @return The log file location or empty string otherwise
     */
    String getLogFileFromCommandLine(String[] commandLine) {

        for (String line: commandLine) {
            if (line.startsWith(DORG_JBOSS_BOOT_LOG_FILE))
                return line.substring(DORG_JBOSS_BOOT_LOG_FILE.length());
        }
        return "";
    }

    /**
     * Try to obtain the management IP and port from the already parsed host.xml or standalone.xml
     * @return an Object containing host and port
     * @see #readStandaloneOrHostXml(org.rhq.core.system.ProcessInfo, boolean) on how to obtain the parsed xml
     */
    protected HostPort getManagementPortFromHostXml() {
        if (hostXml==null)
            throw new IllegalArgumentException("hostXml is null. You need to call 'readStandaloneOrHostXml' first.");
        Element host =  hostXml.getDocumentElement();
        NodeList interfaceParent = host.getElementsByTagName("management-interfaces");
        if (interfaceParent ==null || interfaceParent.getLength()==0) {
            log.warn("No <management-interfaces> found in host.xml");
            return new HostPort();
        }
        NodeList mgmtInterfaces = interfaceParent.item(0).getChildNodes();
        if (mgmtInterfaces==null || mgmtInterfaces.getLength()==0) {
            log.warn("No <*-interface> found in host.xml");
            return new HostPort();
        }
        for (int i = 0 ; i < mgmtInterfaces.getLength(); i++) {
            if (!(mgmtInterfaces.item(i) instanceof Element))
                continue;

            Element mgmtInterface = (Element) mgmtInterfaces.item(i);
            if (mgmtInterface.getNodeName().equals("http-interface")) {
                String tmp = mgmtInterface.getAttribute("port");
                int port = Integer.valueOf(tmp);
                HostPort hp = new HostPort();
                hp.isLocal=true;
                hp.port = port;

                String nIf = mgmtInterface.getAttribute("interface");
                String hostName = getInterface(nIf);
                hp.host = hostName;
                return hp;
            }
        }
        return new HostPort();
    }

    /**
     * Try to obtain the management interface IP from the host/standalone.xml files
     * @param nIf Interface to look for
     * @return IP address to use
     */
    private String getInterface(String nIf) {
        if (hostXml==null)
            throw new IllegalArgumentException("hostXml is null. You need to call 'readStandaloneOrHostXml' first.");

        Element host =  hostXml.getDocumentElement();
        NodeList interfaceParent = host.getElementsByTagName("interfaces");
        if (interfaceParent ==null || interfaceParent.getLength()==0) {
            log.warn("No <interfaces> found in host.xml");
            return null;
        }
        NodeList mgmtInterfaces = interfaceParent.item(0).getChildNodes();
        if (mgmtInterfaces==null || mgmtInterfaces.getLength()==0) {
            log.warn("No <*-interface> found in host.xml");
            return null;
        }
        for (int i = 0 ; i < mgmtInterfaces.getLength(); i++) {
            if (!(mgmtInterfaces.item(i) instanceof Element))
                continue;
            Element mgmtInterface = (Element) mgmtInterfaces.item(i);
            if (mgmtInterface.getNodeName().equals("interface")) {
                String name = mgmtInterface.getAttribute("name");
                if (!name.equals(nIf))
                    continue;

                NodeList nl = mgmtInterface.getChildNodes();
                if (nl!=null) {
                    for (int j = 0 ; j < nl.getLength(); j++) {
                        if (!(nl.item(j) instanceof Element))
                            continue;

                        String nodeName = nl.item(j).getNodeName();
                        if (nodeName.equals("any-ipv4-address"))
                            return "0.0.0.0";

                        String x = ((Element) nl.item(j)).getAttribute("value");
                        return x;

                        // TODO check for <any> and so on
                    }
                }
            }

        }
        return null;  // TODO: Customise this generated block
    }

    /**
     * Try to determine the host name - that is the name of a standalone server or a
     * host in domain mode by looking at the standalone/host.xml files
     * @return server name
     */
    protected String findHostName() {
        if (hostXml==null)
            throw new IllegalArgumentException("hostXml is null. You need to call 'readStandaloneOrHostXml' first.");

        String hostName = hostXml.getDocumentElement().getAttribute("name");
        return hostName;
    }

    /**
     * Try to obtain the domain controller's location from looking at host.xml
     * @return host and port of the domain controller
     */
    protected HostPort getDomainControllerFromHostXml() {
        if (hostXml==null)
            throw new IllegalArgumentException("hostXml is null. You need to call 'readStandaloneOrHostXml' first.");

        Element host =  hostXml.getDocumentElement();
        NodeList dcParent = host.getElementsByTagName("domain-controller");
        if (dcParent==null || dcParent.getLength()==0)
            return new HostPort(false);
        NodeList interfs = dcParent.item(0).getChildNodes();
        for (int i = 0; i < interfs.getLength(); i++) {
            if (!(interfs.item(i)instanceof Element))
                continue;

            Element interf = (Element) interfs.item(i);
            if (interf.getNodeName().equals("local"))
                return new HostPort();

            // not local, so get the remote
            HostPort hp = new HostPort(false);
            hp.host = interf.getAttribute("host");
            hp.port = Integer.parseInt(interf.getAttribute("port"));
            return hp;
        }

        return new HostPort(false);
    }

    /**
     * Get the location of the host definition file (host.xml in domain mode, standalone.xml
     * in standalone mode.
     * @param processInfo ProcessInfo structure containing the ENV variables
     * @param isDomain Are we looking for host.xml (=isDomain) or not
     * @return The path to the definition file.
     */
    protected String getHostXmlFileLocation(ProcessInfo processInfo, boolean isDomain) {

        String home = processInfo.getEnvironmentVariable("jboss.home.dir");
        if (home==null)
            home = getHomeDirFromCommandLine(processInfo.getCommandLine());
        StringBuilder builder = new StringBuilder(home);
        if (isDomain)
            builder.append("/domain");
        else
            builder.append("/standalone");
        builder.append("/configuration");
        if (isDomain)
            builder.append("/host.xml");
        else
            builder.append("/standalone.xml");
        return builder.toString();

    }

    /**
     * Helper class that holds information about the host,port tuple
     */
    protected static class HostPort {
        String host;
        int port;
        boolean isLocal = true;

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
            return "HostPort{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    ", isLocal=" + isLocal +
                    '}';
        }
    }
}
