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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.event.log.LogFileEventResourceComponentHelper;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.ProcessInfo;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Abstract base discovery component for the two server types - "JBossAS7 Host Controller" and
 * "JBossAS7 Standalone Server".
 */
public abstract class BaseProcessDiscovery extends AbstractBaseDiscovery implements ResourceDiscoveryComponent, ManualAddFacet {

    private static final String SOCKET_BINDING_PORT_OFFSET_SYSPROP = "jboss.socket.binding.port-offset";

    private final Log log = LogFactory.getLog(this.getClass());

    // Auto-discover running AS7 instances.
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
        Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

        List<ProcessScanResult> scans = discoveryContext.getAutoDiscoveredProcesses();
        for (ProcessScanResult psr : scans) {
            try {
                DiscoveredResourceDetails details = buildResourceDetails(discoveryContext, psr);
                discoveredResources.add(details);
                log.info("Discovered new " + discoveryContext.getResourceType().getName() + " Resource with key ["
                        + details.getResourceKey() + "].");
            } catch (Exception e) {
                log.error("Discovery for a " + discoveryContext.getResourceType().getName()
                        + " Resource failed for process " + psr + ": " + e);
            }
        }

        return discoveredResources;
    }

    protected DiscoveredResourceDetails buildResourceDetails(ResourceDiscoveryContext discoveryContext, ProcessScanResult psr) {
        Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();
        // IF SE, then look at domain/configuration/host.xml <management interface="default" port="9990
        // for management port
        ProcessInfo process = psr.getProcessInfo();
        String[] commandLine = process.getCommandLine();
        String homeDir = getHomeDir(process).getPath();
        JBossProductType productType = JBossProductType.determineJBossProductType(new File(homeDir));
        String version = determineServerVersionFromHomeDir(homeDir);
        if (productType != JBossProductType.AS) {
            version = productType.SHORT_NAME + " " + version;
        }
        File baseDir = getBaseDir(process);
        String configName = baseDir.getName();
        String key = baseDir.getPath();
        readStandaloneOrHostXmlFromFile(getHostXmlFile(process).getPath()); // this sets this.hostXml
        HostPort hostPort = getHostPortFromHostXml();

        String name = buildDefaultResourceName(hostPort, configName, productType);
        String description = buildDefaultResourceDescription(hostPort, productType);

        pluginConfig.put(new PropertySimple("homeDir", homeDir));
        pluginConfig.put(new PropertySimple("baseDir", baseDir));
        pluginConfig.put(new PropertySimple("configDir", getConfigDir(process)));
        pluginConfig.put(new PropertySimple("startScript", getMode().getStartScript()));
        pluginConfig.put(new PropertySimple("domainHost", findHost(getHostXmlFile(process))));
        fillUserPassFromFile(pluginConfig, getMode(), homeDir);
        String logFile = getLogFile(process).getAbsolutePath();
        initLogEventSourcesConfigProp(logFile, pluginConfig);
        HostPort managementHostPort = getManagementPortFromHostXml(commandLine);
        pluginConfig.put(new PropertySimple("hostname", managementHostPort.host));
        pluginConfig.put(new PropertySimple("port", managementHostPort.port));
        pluginConfig.put(new PropertySimple("realm", getManagementSecurityRealmFromHostXml()));

        ProductInfo productInfo = new ProductInfo(managementHostPort.host, pluginConfig.getSimpleValue("user", null),
            pluginConfig.getSimpleValue("password", null), managementHostPort.port);
        productInfo = productInfo.getFromRemote();
        if (productInfo.fromRemote) {
            version = productInfo.productName + " " + productInfo.productVersion;
            name = productInfo.productName + " " + productInfo.serverName;
        }

        //            String javaClazz = psr.getProcessInfo().getName();

        /*
* We'll connect to the discovered VM on the local host, so set the jmx connection
* properties accordingly. This may only work on JDK6+, but then JDK5 is deprecated
* anyway.
*/
        //                config.put(new PropertySimple(JMXDiscoveryComponent.COMMAND_LINE_CONFIG_PROPERTY,
        //                        javaClazz));
        //                config.put(new PropertySimple(JMXDiscoveryComponent.CONNECTION_TYPE,
        //                        LocalVMTypeDescriptor.class.getName()));
        //
        //                // TODO vmid will change when the detected server is bounced - how do we follow this?
        //                config.put(new PropertySimple(JMXDiscoveryComponent.VMID_CONFIG_PROPERTY,psr.getProcessInfo().getPid()));

        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), key, name, version, description,
            pluginConfig, process);
    }

    protected File getBaseDir(ProcessInfo process) {
        String baseDirString = getSystemPropertyFromCommandLine(process.getCommandLine(),
                getBaseDirSystemPropertyName());
        File baseDir = (baseDirString != null) ? new File(FileUtils.getCanonicalPath(baseDirString)) :
                new File(getHomeDir(process), getDefaultBaseDirName());
        return baseDir;
    }

    protected File getConfigDir(ProcessInfo process) {
        String configDirString = getSystemPropertyFromCommandLine(process.getCommandLine(),
                getConfigDirSystemPropertyName());
        File configDir = (configDirString != null) ? new File(FileUtils.getCanonicalPath(configDirString)) :
                new File(getBaseDir(process), getDefaultConfigDirName());
        return configDir;
    }

    protected File getHostXmlFile(ProcessInfo process) {
        return new File(getConfigDir(process), getHostXmlFileName());
    }

    protected String getDefaultConfigDirName() {
        return "configuration";
    }

    protected abstract AS7Mode getMode();

    protected abstract File getLogFile(ProcessInfo process);

    protected abstract String getBaseDirSystemPropertyName();

    protected abstract String getConfigDirSystemPropertyName();

    protected abstract String getDefaultBaseDirName();

    protected abstract String getHostXmlFileName();

    protected abstract String buildDefaultResourceName(HostPort hostPort, String configName, JBossProductType productType);
    
    protected abstract String buildDefaultResourceDescription(HostPort hostPort, JBossProductType productType);
    
    protected HostPort checkForSocketBindingOffset(HostPort managementPort, String[] commandLine) {
        for (String line : commandLine) {
            if (line.contains(SOCKET_BINDING_PORT_OFFSET_SYSPROP)) {
                String tmp = line.substring(line.indexOf('=')+1);
                Integer offset = Integer.valueOf(tmp);
                managementPort.port+=offset;
                break;
            }
        }

        return managementPort;
    }

    // Manually add a (remote) AS7 instance.
    @Override
    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext context) throws InvalidPluginConfigurationException {

        String hostname = pluginConfiguration.getSimpleValue("hostname", null);
        String portS = pluginConfiguration.getSimpleValue("port", null);
        String user = pluginConfiguration.getSimpleValue("user", null);
        String pass = pluginConfiguration.getSimpleValue("password", null);

        if (hostname == null || portS == null) {
            throw new InvalidPluginConfigurationException("Host and port must not be null");
        }
        int port = Integer.valueOf(portS);

        ProductInfo productInfo = new ProductInfo(hostname, user, pass, port).getFromRemote();
        String productName = productInfo.getProductName();
        String productVersion = productInfo.getProductVersion();

        String resourceKey = hostname + ":" + portS + ":" + productName;

        String description;
        if (productName.contains("EAP")) {
            description = "Standalone" + JBossProductType.EAP.FULL_NAME + " server";
        } else if (productName.contains("EDG")) {
            description = "Standalone" + JBossProductType.EDG.FULL_NAME + " server";
        } else {
            description = context.getResourceType().getDescription();
        }

        pluginConfiguration.put(new PropertySimple("manuallyAdded", true));

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(context.getResourceType(), resourceKey,
            productName + " @ " + hostname + ":" + port, productVersion, description, pluginConfiguration, null);

        return detail;
    }

    private String getServerAttribute(ASConnection connection, String attributeName) {
        Operation op = new ReadAttribute(null, attributeName);
        Result res = connection.execute(op);
        if (!res.isSuccess()) {
            throw new InvalidPluginConfigurationException("Could not connect to remote server ["
                + res.getFailureDescription() + "]. Did you enable management?");
        }
        return (String) res.getResult();
    }

    private void fillUserPassFromFile(Configuration config, AS7Mode mode, String baseDir) {

        //        String configDir = baseDir + File.separator + mode + File.separator + "configuration";
        String realm = getManagementSecurityRealmFromHostXml();
        String fileName = getSecurityPropertyFileFromHostXml(baseDir, mode, realm);

        File file = new File(fileName);
        if (!file.exists() || !file.canRead()) {
            if (log.isDebugEnabled())
                log.debug("No console user properties file found at [" + file.getAbsolutePath()
                    + "] or file is not readable");
            return;
        }
        BufferedReader br = null;
        try {
            FileReader fileReader = new FileReader(file);
            br = new BufferedReader(fileReader);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#"))
                    continue;
                if (line.isEmpty())
                    continue;
                if (!line.contains("="))
                    continue;
                // found a candidate
                String user = line.substring(0, line.indexOf("="));

                config.put(new PropertySimple("user", user));
                //    String pass = line.substring(line.indexOf("=") + 1);
                //    config.put(new PropertySimple("password", pass));  // this is now hashed, so no point in supplying it
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    // empty
                }
        }
    }

    private String findHost(File hostXmlFile) {                
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        String hostName = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream is = new FileInputStream(hostXmlFile);
            try {
                Document document = builder.parse(is); // TODO keep this around
                hostName = document.getDocumentElement().getAttribute("name");
            } finally {
                is.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        if (hostName == null)
            hostName = "local"; // Fallback to the installation default
        return hostName;
    }

    /**
     * Obtain the running configuration from the command line if it was passed via --(server,domain,host)-config
     * @param commandLine Command line to look at
     * @param mode mode and thus command line switch to look for
     * @return the config or the default for the mode if no config was passed on the command line.
     */
    protected String getServerConfigFromCommandLine(String[] commandLine, AS7Mode mode) {
        String configArg = mode.getConfigArg();

        for (int index = 0; index < commandLine.length; index++) {
            if (commandLine[index].startsWith(configArg)) {
                if (index + 1 < commandLine.length) {
                    return commandLine[index + 1];
                } else {
                    break;
                }
            }
        }

        return mode.getDefaultXmlFile();
    }

    private void initLogEventSourcesConfigProp(String fileName, Configuration pluginConfiguration) {
        PropertyList logEventSources = pluginConfiguration
            .getList(LogFileEventResourceComponentHelper.LOG_EVENT_SOURCES_CONFIG_PROP);

        if (logEventSources == null)
            return;

        File serverLogFile = new File(fileName);

        if (serverLogFile.exists() && !serverLogFile.isDirectory()) {
            PropertyMap serverLogEventSource = new PropertyMap("logEventSource");
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.LOG_FILE_PATH, serverLogFile));
            serverLogEventSource.put(new PropertySimple(
                LogFileEventResourceComponentHelper.LogEventSourcePropertyNames.ENABLED, Boolean.FALSE));
            logEventSources.add(serverLogEventSource);
        }
    }

    private class ProductInfo {
        private String hostname;
        private String user;
        private String pass;
        private int port;
        private String productVersion;
        private String productName;
        private String releaseVersion;
        private String releaseCodeName;
        private boolean fromRemote = false;
        private String serverName;

        public ProductInfo(String hostname, String user, String pass, int port) {
            this.hostname = hostname;
            this.user = user;
            this.pass = pass;
            this.port = port;
        }

        public String getProductVersion() {
            return productVersion;
        }

        public String getProductName() {
            return productName;
        }

        public ProductInfo getFromRemote() {
            ASConnection connection = new ASConnection(hostname, port, user, pass);
            try {
                productVersion = getServerAttribute(connection, "product-version");
                productName = getServerAttribute(connection, "product-name");
                releaseVersion = getServerAttribute(connection, "release-version");
                releaseCodeName = getServerAttribute(connection, "release-codename");
                serverName = getServerAttribute(connection, "name");
                if (productVersion == null)
                    productVersion = releaseVersion;
                if (productName == null)
                    productName = "AS7";

                fromRemote = true;
            } catch (InvalidPluginConfigurationException e) {
                log.debug("Could not get the product info from [" + hostname + ":" + port
                    + "] - probably a connection failure");
            }
            return this;
        }

        @Override
        public String toString() {
            return "ProductInfo{" + "hostname='" + hostname + '\'' + ", port=" + port + ", productVersion='"
                + productVersion + '\'' + ", productName='" + productName + '\'' + ", releaseVersion='"
                + releaseVersion + '\'' + ", releaseCodeName='" + releaseCodeName + '\'' + ", fromRemote=" + fromRemote
                + '}';
        }
    }

}
