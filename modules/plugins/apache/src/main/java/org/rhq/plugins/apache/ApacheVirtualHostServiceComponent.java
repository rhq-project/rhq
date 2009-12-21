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
import java.util.Collections;
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
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.util.ResponseTimeConfiguration;
import org.rhq.core.pluginapi.util.ResponseTimeLogParser;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
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
    ConfigurationFacet, DeleteResourceFacet {
    private final Log log = LogFactory.getLog(this.getClass());

    public static final String URL_CONFIG_PROP = "url";
    public static final String MAIN_SERVER_RESOURCE_KEY = "MainServer";

    public static final String RESPONSE_TIME_LOG_FILE_CONFIG_PROP = ResponseTimeConfiguration.RESPONSE_TIME_LOG_FILE_CONFIG_PROP;
    public static final String RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP = ResponseTimeConfiguration.RESPONSE_TIME_URL_EXCLUDES_CONFIG_PROP;
    public static final String RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP = ResponseTimeConfiguration.RESPONSE_TIME_URL_TRANSFORMS_CONFIG_PROP;

    private static final String RESPONSE_TIME_METRIC = "ResponseTime";
    /** Multiply by 1/1000 to convert logged response times, which are in microseconds, to milliseconds. */
    private static final double RESPONSE_TIME_LOG_TIME_MULTIPLIER = 0.001;

    private ResourceContext<ApacheServerComponent> resourceContext;
    private URL url;
    private ResponseTimeLogParser logParser;

    private ConfigurationTimestamp lastConfigurationTimeStamp = new ConfigurationTimestamp();
    private int snmpWwwServiceIndex = -1;

    public void start(ResourceContext<ApacheServerComponent> resourceContext) throws Exception {
        this.resourceContext = resourceContext;
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String url = pluginConfig.getSimple(URL_CONFIG_PROP).getStringValue();
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
        return WWWUtils.isAvailable(this.url) ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        AugeasTree tree = getServerConfigurationTree();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType()
            .getResourceConfigurationDefinition();

        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        return mapping.updateConfiguration(getNode(tree), resourceConfigDef);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        AugeasTree tree=null;
        try {
        tree = getServerConfigurationTree();
        ConfigurationDefinition resourceConfigDef = resourceContext.getResourceType().getResourceConfigurationDefinition();
        ApacheAugeasMapping mapping = new ApacheAugeasMapping(tree);
        AugeasNode virtHostNode = getNode(tree);
        mapping.updateAugeas(virtHostNode,report.getConfiguration(), resourceConfigDef);
        tree.save();
        
        report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        log.info("Apache configuration was updated");
        }catch(Exception e){
                if (tree!=null)
                    log.error("Augeas failed to save configuration "+tree.summarizeAugeasError());
                else
                    log.error("Augeas failed to save configuration",e);
           report.setStatus(ConfigurationUpdateStatus.FAILURE);		
        }
    }

    public void deleteResource() throws Exception {
        // TODO Auto-generated method stub

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
        if (pipeIdx >= 0) {
            serverName = resourceKey.substring(0, pipeIdx);
        }

        String[] addrs = resourceKey.substring(pipeIdx + 1).split(" ");
        List<AugeasNode> nodes = tree.matchRelative(tree.getRootNode(), "<VirtualHost");
        List<AugeasNode> virtualHosts = new ArrayList<AugeasNode>();
        boolean updated;

        for (AugeasNode node : nodes) {
            List<AugeasNode> serverNameNodes = tree.matchRelative(node, "ServerName/param");
            String tempServerName = null;

            if (!(serverNameNodes.isEmpty())) {
                tempServerName = serverNameNodes.get(0).getValue();
            }

            if (tempServerName == null && serverName == null) {
                virtualHosts.add(node);
            } else if (tempServerName != null && serverName != null)
                if (tempServerName.equals(serverName)) {
                    updated = false;
                    List<AugeasNode> params = node.getChildByLabel("param");
                    for (AugeasNode nd : params) {
                        updated = false;
                        for (String adr : addrs) {
                            if (adr.equals(nd.getValue()))
                                updated = true;
                        }
                        if (!updated)
                            break;

                    }

                    if (updated) {
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

    /**
     * @return the index of the virtual host that identifies it in SNMP
     * @throws Exception on SNMP error
     */
    private int getWwwServiceIndex() throws Exception {
        ConfigurationTimestamp currentTimestamp = resourceContext.getParentResourceComponent().getConfigurationTimestamp();
        if (!lastConfigurationTimeStamp.equals(currentTimestamp)) {
            snmpWwwServiceIndex = -1;
            
            //configuration has changed. re-read the service index of this virtual host

            //we have to scan the SNMP to find the entry corresponding to this vhost.
            SNMPSession snmpSession = resourceContext.getParentResourceComponent().getSNMPSession();

            List<SNMPValue> names;
            List<SNMPValue> ports;

            names = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_NAME);
            ports = snmpSession.getColumn(SNMPConstants.COLUMN_VHOST_PORT);
            Iterator<SNMPValue> namesIterator = names.iterator();
            Iterator<SNMPValue> portsIterator = ports.iterator();

            //figure out the servername and addresses of this virtual host
            //from the resource key.
            String vhostServerName = null;
            String[] vhostAddressStrings = null;
            String key = resourceContext.getResourceKey();
            int pipeIdx = key.indexOf('|');
            if (pipeIdx >= 0) {
                vhostServerName = key.substring(0, pipeIdx);
            }
            vhostAddressStrings = key.substring(pipeIdx + 1).split(" ");

            AugeasTree configTree = resourceContext.getParentResourceComponent().getAugeasTree();

            //convert the vhost addresses into fully qualified ip/port addresses
            List<HttpdAddressUtility.Address> vhostAddresses = new ArrayList<HttpdAddressUtility.Address>(
                vhostAddressStrings.length);

            if (vhostAddressStrings.length == 1 && MAIN_SERVER_RESOURCE_KEY.equals(vhostAddressStrings[0])) {
                vhostAddresses = Collections.singletonList(HttpdAddressUtility.getMainServerSampleAddress(configTree));
            } else {               
                for (int i = 0; i < vhostAddressStrings.length; ++i) {
                    vhostAddresses.add(HttpdAddressUtility.getVirtualHostSampleAddress(configTree, vhostAddressStrings[i],
                        vhostServerName));
                }
            }
            
            while (namesIterator.hasNext()) {
                SNMPValue nameValue = namesIterator.next();
                SNMPValue portValue = portsIterator.next();

                String snmpHost = nameValue.toString();
                String fullPort = portValue.toString();

                int snmpPort = Integer.parseInt(fullPort.substring(fullPort.lastIndexOf(".") + 1));
                if (snmpPort == 0) snmpPort = 80;
                
                if (containsAddress(vhostAddresses, new HttpdAddressUtility.Address(snmpHost, snmpPort))) {
                    String nameOID = nameValue.getOID();
                    snmpWwwServiceIndex = Integer.parseInt(nameOID.substring(nameOID.lastIndexOf(".") + 1));
                    
                    break;
                }
            }
        }
        lastConfigurationTimeStamp = currentTimestamp;
        return snmpWwwServiceIndex;
    }

    private boolean containsAddress(List<HttpdAddressUtility.Address> addresses, HttpdAddressUtility.Address addressToCheck) throws UnknownHostException {
        if (addresses.contains(addressToCheck)) {
            return true;
        }
        
        //try to get the IP of the address to check
        InetAddress[] ipAddresses = InetAddress.getAllByName(addressToCheck.host);
        
        for(InetAddress ip : ipAddresses) {
            HttpdAddressUtility.Address newCheck = new HttpdAddressUtility.Address(ip.getHostAddress(), addressToCheck.port);
            
            if (addresses.contains(newCheck)) {
                return true;
            }
        }
        
        return false;
    }
}