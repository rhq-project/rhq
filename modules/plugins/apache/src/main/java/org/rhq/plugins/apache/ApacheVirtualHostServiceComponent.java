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
package org.rhq.plugins.apache;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.util.AugeasNodeSearch;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;
import org.rhq.plugins.apache.util.ConfigurationTimestamp;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;
import org.rhq.plugins.www.util.WWWUtils;

/**
 * @author Ian Springer
 * @author Lukas Krejci
 */
public class ApacheVirtualHostServiceComponent implements ResourceComponent<ApacheServerComponent>, MeasurementFacet,
    ConfigurationFacet, DeleteResourceFacet, CreateChildResourceFacet {
    private static final Log log = LogFactory.getLog(ApacheVirtualHostServiceComponent.class);

    public static final String URL_CONFIG_PROP = "url";
    public static final String MAIN_SERVER_RESOURCE_KEY = "MainServer";

    public static final String RESPONSE_TIME_LOG_FILE_CONFIG_PROP = ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP;
    public static final String RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP = ResponseTimeConfiguration.RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP;
    public static final String RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP = ResponseTimeConfiguration.RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP;

    public static final String SERVER_NAME_CONFIG_PROP = "ServerName";
    
    private static final String RESPONSE_TIME_METRIC = "ResponseTime";
    /** Multiply by 1/1000 to convert logged response times, which are in microseconds, to milliseconds. */
    private static final double RESPONSE_TIME_LOG_TIME_MULTIPLIER = 0.001;

    private ResourceContext<ApacheServerComponent> resourceContext;
    private URL url;
    private ResponseTimeLogParser logParser;

    private ConfigurationTimestamp lastConfigurationTimeStamp = new ConfigurationTimestamp();
    private int snmpWwwServiceIndex = -1;

    public static final String RESOURCE_TYPE_NAME = "Apache Virtual Host";
    
    public void start(ResourceContext<ApacheServerComponent> resourceContext) throws Exception {
        this.resourceContext = resourceContext;
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String url = pluginConfig.getSimple(URL_CONFIG_PROP).getStringValue();
        if (url != null) {
            try {
                this.url = new URL(url);
                if (this.url.getPort() == 0) {
                    throw new InvalidPluginConfigurationException(
                        "The 'url' connection property is invalid - 0 is not a valid port; please change the value to the "
                            + "port this virtual host is listening on. NOTE: If the 'url' property was set this way "
                            + "after autodiscovery, you most likely did not include the port in the ServerName directive for "
                            + "this virtual host in httpd.conf.");
                }
            } catch (MalformedURLException e) {
                throw new Exception("Value of '" + URL_CONFIG_PROP + "' connection property ('" + url
                    + "') is not a valid URL.");
            }
        }
        
        ResponseTimeConfiguration responseTimeConfig = new ResponseTimeConfiguration(pluginConfig);
        File logFile = responseTimeConfig.getLogFile();
        if (logFile != null) {
            this.logParser = new ResponseTimeLogParser(logFile, RESPONSE_TIME_LOG_TIME_MULTIPLIER);
            this.logParser.setExcludes(responseTimeConfig.getExcludes());
            this.logParser.setTransforms(responseTimeConfig.getTransforms());
        }
    }

    public void stop() {
        this.resourceContext = null;
        this.url = null;
    }

    public AvailabilityType getAvailability() {
        return (this.url != null && WWWUtils.isAvailable(this.url)) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        ApacheServerComponent parent = resourceContext.getParentResourceComponent();
        if (!parent.isAugeasEnabled())
           throw new Exception(ApacheServerComponent.CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
        
        AugeasTree tree = getServerConfigurationTree();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        return mapping.updateConfiguration(getNode(tree), resourceConfigDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        AugeasTree tree = null;
        try {
            tree = getServerConfigurationTree();
            ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
                .getResourceConfigurationDefinition();
            ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
            AugeasNode virtHostNode = getNode(tree);
            mapping.updateAugeas(virtHostNode, report.getConfiguration(), resourceConfigDef);
            tree.save();

            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
            log.info("Apache configuration was updated");
            
            finishConfigurationUpdate(report);
        } catch (Exception e) {
            if (tree != null)
                log.error("Augeas failed to save configuration " + tree.summarizeAugeasError());
            else
                log.error("Augeas failed to save configuration", e);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
        }
    }

    public void deleteResource() throws Exception {
        ApacheServerComponent parent = resourceContext.getParentResourceComponent();
        if (!parent.isAugeasEnabled())
           throw new Exception(ApacheServerComponent.CONFIGURATION_NOT_SUPPORTED_ERROR_MESSAGE);
        
        if (MAIN_SERVER_RESOURCE_KEY.equals(resourceContext.getResourceKey())) {
            throw new IllegalArgumentException("Cannot delete the virtual host representing the main server configuration.");
        }
        
        AugeasTree tree = getServerConfigurationTree();
        
        try {
            AugeasNode myNode = getNode(getServerConfigurationTree());
    
            tree.removeNode(myNode, true);
            tree.save();
            
            deleteEmptyFile(tree, myNode);
            conditionalRestart();
        } catch (IllegalStateException e) {
            //this means we couldn't find the augeas node for this vhost.
            //that error can be safely ignored in this situation.
        }
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) throws Exception {
        int primaryIndex = getWwwServiceIndex();

        //bail out quickly if there's no SNMP support
        if (primaryIndex < 0)
            return;

        log.debug("Collecting metrics for VirtualHost service #" + primaryIndex + "...");
        SNMPSession snmpSession = this.resourceContext.getParentResourceComponent().getSNMPSession();

        if (!snmpSession.ping()) {
            log.debug("Failed to connect to SNMP agent at " + snmpSession + " - aborting metric collection...");
            return;
        }

        for (MeasurementScheduleRequest schedule : schedules) {
            String metricName = schedule.getName();
            if (metricName.equals(RESPONSE_TIME_METRIC)) {
                if (this.logParser != null) {
                    try {
                        CallTimeData callTimeData = new CallTimeData(schedule);
                        this.logParser.parseLog(callTimeData);
                        report.addData(callTimeData);
                    } catch (Exception e) {
                        log.error("Failed to retrieve HTTP call-time data.", e);
                    }
                } else {
                    log.error("The '" + RESPONSE_TIME_METRIC + "' metric is enabled for resource '"
                        + this.resourceContext.getResourceKey() + "', but no value is defined for the '"
                        + RESPONSE_TIME_LOG_FILE_CONFIG_PROP + "' connection property.");
                    // TODO: Communicate this error back to the server for display in the GUI.
                }
            } else {
                // Assume anything else is an SNMP metric.
                try {
                    collectSnmpMetric(report, primaryIndex, snmpSession, schedule);
                } catch (SNMPException e) {
                    log.error("An error occurred while attempting to collect an SNMP metric.", e);
                }
            }
        }

        log.info("Collected " + report.getDataCount() + " metrics for VirtualHost "
            + this.resourceContext.getResourceKey() + ".");
    }

    public CreateResourceReport createResource(CreateResourceReport report) {
        if (!isAugeasEnabled()){
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage("Resources can be created only when augeas is enabled.");
            return report;
        }
        ResourceType resourceType = report.getResourceType();
        
        if (resourceType.equals(getDirectoryResourceType())) {
            Configuration resourceConfiguration = report.getResourceConfiguration();
            Configuration pluginConfiguration = report.getPluginConfiguration();
        
            String directoryName = report.getUserSpecifiedResourceName();
            
            //fill in the plugin configuration
            
            //get the directive index
            AugeasTree tree = getServerConfigurationTree();
            AugeasNode myNode = getNode(tree);
            List<AugeasNode> directories = myNode.getChildByLabel("<Directory");
            int seq = 1;
            /*
             * myNode will be parent node of the new Directory node.
             * We need to create a new node for directory node which will contain child nodes.
             * To create a node we can call method from AugeasTree which will create a node. In this method is 
             * parameter sequence, if we will leave this parameter empty and there will be more nodes with 
             * the same label, new node will be created but the method createNode will return node with index 0 resp 1.
             * If that will happen we can not update the node anymore because we are updating wrong node.
             * To avoid this situation we need to know what is the last sequence nr. of virtual host's child (directory) nodes.
             * We can not just count child nodes with the same label because some of the child nodes
             * could be stored in another file. So that in httpd configurationstructure they are child nodes of virtual host,
             *  but in augeas configuration structure they can be child nodes of node Include[];. 
             */
           
            for (AugeasNode n : directories) {
                String param = n.getFullPath();
                int end = param.lastIndexOf(File.separatorChar);
                if (end != -1)
                  if (myNode.getFullPath().equals(param.substring(0,end)))
                      seq++;
                }
            
            //pluginConfiguration.put(new PropertySimple(ApacheDirectoryComponent.DIRECTIVE_INDEX_PROP, seq));
            //we don't support this yet... need to figure out how...
            pluginConfiguration.put(new PropertySimple(ApacheDirectoryComponent.REGEXP_PROP, false));
            String dirNameToSet = AugeasNodeValueUtil.escape(directoryName);
                        
            //now actually create the data in augeas
            try {
                ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
                AugeasNode directoryNode = tree.createNode(myNode, "<Directory", null, seq);
                tree.createNode(directoryNode, "param", dirNameToSet, 0);
                mapping.updateAugeas(directoryNode, resourceConfiguration, resourceType.getResourceConfigurationDefinition());
                tree.save();
                
                
                tree = getServerConfigurationTree();
                String key = AugeasNodeSearch.getNodeKey(myNode, directoryNode);
                report.setResourceKey(key); 
                report.setResourceName(directoryName);
    
                report.setStatus(CreateResourceStatus.SUCCESS);
                
                resourceContext.getParentResourceComponent().finishChildResourceCreate(report);
            } catch (Exception e) {
                report.setException(e);
                report.setStatus(CreateResourceStatus.FAILURE);
            }
        } else {
            report.setErrorMessage("Unable to create resources of type " + resourceType.getName());
            report.setStatus(CreateResourceStatus.FAILURE);
        }
        return report;
    }
    
    public AugeasTree getServerConfigurationTree() {
        return resourceContext.getParentResourceComponent().getAugeasTree();
    }

    /**
     * Returns a node corresponding to this component in the Augeas tree.
     * 
     * @param tree
     * @return
     * @throws IllegalStateException if none or more than one nodes found
     */
    public AugeasNode getNode(AugeasTree tree) {
        String resourceKey = resourceContext.getResourceKey();

        if (ApacheVirtualHostServiceComponent.MAIN_SERVER_RESOURCE_KEY.equals(resourceKey)) {
            return tree.getRootNode();
        }

        String serverName = null;
        int pipeIdx = resourceKey.indexOf('|');
        //the resource key always contains the '|' so we're only checking for non-empty
        //server names
        if (pipeIdx > 0) {
            serverName = resourceKey.substring(0, pipeIdx);
        }

        String[] addrs = resourceKey.substring(pipeIdx + 1).split(" ");
        List<AugeasNode> nodes = tree.matchRelative(tree.getRootNode(), "<VirtualHost");
        List<AugeasNode> virtualHosts = new ArrayList<AugeasNode>();

        boolean matching = false;

        for (AugeasNode node : nodes) {
            matching = false;
            List<AugeasNode> serverNameNodes = tree.matchRelative(node, "ServerName/param");
            String tempServerName = null;

            if (!(serverNameNodes.isEmpty())) {
                tempServerName = serverNameNodes.get(0).getValue();
            }
            if (tempServerName == null & serverName == null) {
                matching = true;
            }
            
            if (tempServerName != null & serverName != null) {
                if (tempServerName.equals(serverName)) {
                    matching = true;
                }
            }
            
            if (matching) {
                List<AugeasNode> params = node.getChildByLabel("param");
                for (AugeasNode nd : params) {
                    matching = false;
                    for (String adr : addrs) {
                        if (adr.equals(nd.getValue())) {
                            matching = true;
                        }
                    }
                    if (!matching) {
                        break;
                    }
                }

                if (matching) {
                    virtualHosts.add(node);
                }
            }
        }
       
        if (virtualHosts.size() == 0) {
            throw new IllegalStateException("Could not find virtual host configuration in augeas for virtual host: "
                + resourceKey);
        }

        if (virtualHosts.size() > 1) {
            throw new IllegalStateException("Found more than 1 virtual host configuration in augeas for virtual host: "
                + resourceKey);
        }

        return virtualHosts.get(0);
    }

    /**
     * @see ApacheServerComponent#finishConfigurationUpdate(ConfigurationUpdateReport)
     */
    public void finishConfigurationUpdate(ConfigurationUpdateReport report) {
        resourceContext.getParentResourceComponent().finishConfigurationUpdate(report);
    }
    
    /**
     * @see ApacheServerComponent#conditionalRestart()
     * 
     * @throws Exception
     */
    public void conditionalRestart() throws Exception {
        resourceContext.getParentResourceComponent().conditionalRestart();
    }
    
    public void deleteEmptyFile(AugeasTree tree, AugeasNode deletedNode) {
        resourceContext.getParentResourceComponent().deleteEmptyFile(tree, deletedNode);
    }
    
    private void collectSnmpMetric(MeasurementReport report, int primaryIndex, SNMPSession snmpSession,
        MeasurementScheduleRequest schedule) throws SNMPException {
        SNMPValue snmpValue = null;
        String metricName = schedule.getName();
        int dotIndex = metricName.indexOf('.');
        String mibName;
        if (dotIndex == -1) {
            // it's a service metric (e.g. "wwwServiceName") or a summary metric (e.g. "wwwSummaryInRequests")
            mibName = metricName;
            List<SNMPValue> snmpValues = snmpSession.getColumn(mibName);

            // NOTE: We assume SNMPValue's are returned in index-order.
            snmpValue = snmpValues.get(primaryIndex - 1);
        } else {
            // it's a request or response metric (e.g. "wwwRequestInRequests.GET" or "wwwResponseOutResponses.200")
            mibName = metricName.substring(0, dotIndex);
            String mibSecondaryIndex = metricName.substring(dotIndex + 1);
            String oid;
            try {
                Integer.parseInt(mibSecondaryIndex);
                oid = mibSecondaryIndex;
            } catch (NumberFormatException e) {
                // OID must be encoded as a string (e.g. 3.71.69.84 == "GET") - decode it
                oid = convertStringToOid(mibSecondaryIndex);
            }

            boolean found = false;
            Map<String, SNMPValue> table = snmpSession.getTable(mibName, primaryIndex);
            if (table != null) {
                snmpValue = table.get(oid);
                if (snmpValue != null) {
                    found = true;
                }
            }

            if (!found) {
                log.error("Entry '" + oid + "' not found for " + mibName + "[" + primaryIndex + "].");
                log.error("Table:\n" + table);
                return;
            }
        }

        log.debug("Collected SNMP metric [" + metricName + "], value = " + snmpValue);

        boolean valueIsTimestamp = false;
        ApacheServerComponent.addSnmpMetricValueToReport(report, schedule, snmpValue, valueIsTimestamp);
    }

    private String convertStringToOid(String string) {
        String oid;
        StringBuilder strBuf = new StringBuilder();
        strBuf.append(string.length()); // first digit in OID is the length of the string
        for (int i = 0; i < string.length(); i++) {
            // remaining digits are the integer values of each of the characters in the string
            strBuf.append('.').append((byte) string.charAt(i));
        }

        oid = strBuf.toString();
        return oid;
    }

    public static int getMatchingWwwServiceIndex(ApacheServerComponent parent, String resourceKey, List<SNMPValue> names, List<SNMPValue> ports) {
        int ret = -1;
        Iterator<SNMPValue> namesIterator = names.iterator();
        Iterator<SNMPValue> portsIterator = ports.iterator();

        //figure out the servername and addresses of this virtual host
        //from the resource key.
        String vhostServerName = null;
        String[] vhostAddressStrings = null;
        int pipeIdx = resourceKey.indexOf('|');
        if (pipeIdx >= 0) {
            vhostServerName = resourceKey.substring(0, pipeIdx);
        }
        vhostAddressStrings = resourceKey.substring(pipeIdx + 1).split(" ");

        ApacheDirectiveTree tree = parent.loadParser(); 
        
        //convert the vhost addresses into fully qualified ip/port addresses
        List<HttpdAddressUtility.Address> vhostAddresses = new ArrayList<HttpdAddressUtility.Address>(
            vhostAddressStrings.length);

        if (vhostAddressStrings.length == 1 && MAIN_SERVER_RESOURCE_KEY.equals(vhostAddressStrings[0])) {
            HttpdAddressUtility.Address serverAddr = parent.getAddressUtility().getMainServerSampleAddress(tree, null, 0);
            if (serverAddr != null) {
                vhostAddresses.add(serverAddr);
            }
        } else {
            for (int i = 0; i < vhostAddressStrings.length; ++i) {
                HttpdAddressUtility.Address vhostAddr = parent.getAddressUtility().getVirtualHostSampleAddress(tree, vhostAddressStrings[i],
                    vhostServerName, true);
                if (vhostAddr != null) {
                    vhostAddresses.add(vhostAddr);
                }
            }
        }

        //finding the snmp index that corresponds to the address(es) of the vhost isn't that simple
        //because the snmp module in apache always resolves the IPs to hostnames.
        //on the other hand, the resource key tries to be more accurate about what a 
        //vhost can actually be represented as. A vhost is represented by at most 1 hostname (i.e. ServerName)
        //and possibly multiple IP addresses.
        SNMPValue bestMatch = null;
        int bestMatchRate = 0;
        
        while (namesIterator.hasNext()) {
            SNMPValue nameValue = namesIterator.next();
            SNMPValue portValue = portsIterator.next();

            String snmpHost = nameValue.toString();
            String fullPort = portValue.toString();

            int snmpPort = Integer.parseInt(fullPort.substring(fullPort.lastIndexOf(".") + 1));
            
            HttpdAddressUtility.Address snmpAddress = new HttpdAddressUtility.Address(snmpHost, snmpPort);
        
            int matchRate = matchRate(vhostAddresses, snmpAddress);
            if (matchRate > bestMatchRate) {
                bestMatch = nameValue;
                bestMatchRate = matchRate;
            }
        }
        
        if (bestMatch != null) {
            String nameOID = bestMatch.getOID();
            ret = Integer.parseInt(nameOID.substring(nameOID.lastIndexOf(".") + 1));
        } else {
            log.warn("Unable to match the Virtual Host [" + resourceKey + "] with any of the SNMP advertised vhosts: " + names + ". It won't be possible to monitor the Virtual Host.");
        }
        return ret;
    }
    
    /**
     * @return the index of the virtual host that identifies it in SNMP
     * @throws Exception on SNMP error
     */
    private int getWwwServiceIndex() throws Exception {
        ConfigurationTimestamp currentTimestamp = resourceContext.getParentResourceComponent().getConfigurationTimestamp();
        if (!lastConfigurationTimeStamp.equals(currentTimestamp)) {
            snmpWwwServiceIndex = -1;
            //don't go through this configuration again even if we fail further below.. we'd fail again.
            lastConfigurationTimeStamp = currentTimestamp;
            
            //configuration has changed. re-read the service index of this virtual host

            //we have to scan the SNMP to find the entry corresponding to this vhost.
            SNMPSession snmpSession = resourceContext.getParentResourceComponent().getSNMPSession();

            List<SNMPValue> names;
            List<SNMPValue> ports;

            names = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_NAME);
            ports = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_PORT);
            
            snmpWwwServiceIndex = getMatchingWwwServiceIndex(resourceContext.getParentResourceComponent(), resourceContext.getResourceKey(), names, ports);
        }
        return snmpWwwServiceIndex;
    }

    public static int matchRate(List<HttpdAddressUtility.Address> addresses, HttpdAddressUtility.Address addressToCheck) {
        for(HttpdAddressUtility.Address a : addresses) {
            if (HttpdAddressUtility.isAddressConforming(addressToCheck, a.host, a.port, true)) {
                return 3;
            }
        }
        
        //try to get the IP of the address to check
        InetAddress[] ipAddresses;
        try {
            ipAddresses = InetAddress.getAllByName(addressToCheck.host);
            for(InetAddress ip : ipAddresses) {
                HttpdAddressUtility.Address newCheck = new HttpdAddressUtility.Address(ip.getHostAddress(), addressToCheck.port);
                
                for(HttpdAddressUtility.Address a : addresses) {
                    if (HttpdAddressUtility.isAddressConforming(newCheck, a.host, a.port, true)) {
                        return 2;
                    }
                }
            }            
        } catch (UnknownHostException e) {
            log.debug("Unknown host encountered in the httpd configuration: " + addressToCheck.host);
            return 0;
        }
        
        //this stupid 80 = 0 rule is to conform with snmp module
        //the problem is that snmp module represents both 80 and * port defs as 0, 
        //so whatever we do, we might mismatch the vhost. But there's no working around that
        //but to modify the snmp module itself.
        
        int addressPort = addressToCheck.port;
        if (addressPort == 80) {
            addressPort = 0;
        }
        
        //ok, try the hardest...
        for(HttpdAddressUtility.Address listAddress: addresses) {
            int listPort = listAddress.port;
            if (listPort == 80) {
                listPort = 0;
            }
            
            InetAddress[] listAddresses;
            try {
                listAddresses = InetAddress.getAllByName(listAddress.host);
            } catch (UnknownHostException e) {
                log.debug("Unknown host encountered in the httpd configuration: " + listAddress.host);
                return 0;
            }
            
            for (InetAddress listInetAddr : listAddresses) {
                for (InetAddress ip : ipAddresses) {
                    if (ip.equals(listInetAddr) && addressPort == listPort) {
                        return 1;
                    }
                }
            }
        }
        
        return 0;
    }
    
    private ResourceType getDirectoryResourceType() {
        return resourceContext.getResourceType().getChildResourceTypes().iterator().next();
    }
    
    public ApacheDirectiveTree loadParser() throws Exception{
        return resourceContext.getParentResourceComponent().loadParser();
    }
    
    public boolean isAugeasEnabled(){
        ApacheServerComponent parent = resourceContext.getParentResourceComponent();
        return parent.isAugeasEnabled();          
    }
}