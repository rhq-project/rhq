/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.augeas.Augeas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.augeas.helper.AugeasUtility;
import org.rhq.plugins.hosts.helper.NonAugeasHostsConfigurationDelegate;

/**
 * The ResourceComponent for the "Hosts File" ResourceType.
 *
 * @author Ian Springer
 */
public class HostsComponent extends AugeasConfigurationComponent {
    private static final String IPV4_ADDRESS_REGEX = "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)";
    private static final Pattern IPV4_ADDRESS_PATTERN = Pattern.compile(IPV4_ADDRESS_REGEX);

    private static final String IPV6_ADDRESS_REGEX = "((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})))(%.+)?";
    private static final Pattern IPV6_ADDRESS_PATTERN = Pattern.compile(IPV6_ADDRESS_REGEX);

    private static final String DOMAIN_NAME_REGEX = "(([a-zA-Z\\d]|[a-zA-Z\\d][a-zA-Z\\d\\-]*[a-zA-Z\\d])\\.)*([A-Za-z]|[A-Za-z][A-Za-z\\d\\-]*[A-Za-z\\d])";
    private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile(DOMAIN_NAME_REGEX);

    private final Log log = LogFactory.getLog(this.getClass());

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration resourceConfig;
        if (isAugeasAvailable()) {
            resourceConfig = super.loadResourceConfiguration();
        } else {
            resourceConfig = new NonAugeasHostsConfigurationDelegate(this).loadResourceConfiguration();
            // This will add error messages to any PropertySimples with invalid values, so they can be displayed by the GUI.
            validateResourceConfiguration(new ConfigurationUpdateReport(resourceConfig));
        }
        return resourceConfig;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        if (isAugeasAvailable()) {
            super.updateResourceConfiguration(report);
        } else {
            if (!validateResourceConfiguration(report)) {
                log.debug("Validation of updated Resource configuration for " + getResourceDescription()
                    + " failed with the following errors: " + report.getErrorMessage());
                report.setStatus(ConfigurationUpdateStatus.FAILURE);
                return;
            }
            new NonAugeasHostsConfigurationDelegate(this).updateResourceConfiguration(report);
        }
    }

    @Override
    protected String getNodeInsertionPoint(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        if ("alias".equals(propSimple.getName())) {
            return String.format("%s/canonical", node.getParent().getPath());
        }
        return super.getNodeInsertionPoint(augeas, node, propDefSimple, propSimple);
    }

    @Override
    protected AugeasNode getNewListMemberNode(AugeasNode listNode, PropertyDefinitionMap listMemberPropDefMap,
        int listIndex) {
        return new AugeasNode(listNode, "0" + listIndex);
    }

    @Override
    protected boolean validateResourceConfiguration(ConfigurationUpdateReport report) {
        Configuration resourceConfig = report.getConfiguration();
        StringBuilder errorMessage = new StringBuilder();
        Set<String> ipv4CanonicalNames = new HashSet<String>();
        Set<String> ipv6CanonicalNames = new HashSet<String>();
        Set<String> ipv4DuplicateCanonicalNames = new HashSet<String>();
        Set<String> ipv6DuplicateCanonicalNames = new HashSet<String>();
        Set<String> invalidDomainNameCanonicalNames = new HashSet<String>();
        Set<String> invalidIpAddressCanonicalNames = new HashSet<String>();
        Set<String> invalidAliasCanonicalNames = new HashSet<String>();
        PropertyList entries = resourceConfig.getList(".");
        for (Property entryProp : entries.getList()) {
            PropertyMap entryPropMap = (PropertyMap) entryProp;
            PropertySimple canonicalProp = entryPropMap.getSimple("canonical");
            String canonical = canonicalProp.getStringValue();
            if (!validateDomainName(canonical)) {
                canonicalProp.setErrorMessage("Invalid domain name.");
                invalidDomainNameCanonicalNames.add(canonical);
            }
            PropertySimple ipAddrProp = entryPropMap.getSimple("ipaddr");
            String ipaddr = ipAddrProp.getStringValue();
            if (!validateIpAddress(ipaddr)) {
                ipAddrProp.setErrorMessage("Invalid IP address.");
                invalidIpAddressCanonicalNames.add(canonical);
            }
            if (ipaddr.indexOf(':') != -1) {
                if (!ipv6CanonicalNames.add(canonical)) {
                    ipv6DuplicateCanonicalNames.add(canonical);
                }
            } else {
                if (!ipv4CanonicalNames.add(canonical)) {
                    ipv4DuplicateCanonicalNames.add(canonical);
                }
            }
        }
        if (!invalidDomainNameCanonicalNames.isEmpty()) {
            errorMessage.append("The entries with the following canonical names have invalid canonical names: ")
                .append(invalidDomainNameCanonicalNames).append(".\n");
        }
        if (!invalidIpAddressCanonicalNames.isEmpty()) {
            errorMessage.append("The entries with the following canonical names have invalid IP addresses: ").append(
                invalidIpAddressCanonicalNames).append(".\n");
        }
        if (!invalidAliasCanonicalNames.isEmpty()) {
            errorMessage.append("The entries with the following canonical names have one or more invalid aliases: ")
                .append(invalidAliasCanonicalNames).append(".\n");
        }
        if (!ipv4DuplicateCanonicalNames.isEmpty()) {
            errorMessage.append("More than one IPv4 address is defined for the following canonical names: ").append(
                ipv4DuplicateCanonicalNames).append(".\n");
        }
        if (!ipv6DuplicateCanonicalNames.isEmpty()) {
            errorMessage.append("More than one IPv6 address is defined for the following canonical names: ").append(
                ipv6DuplicateCanonicalNames).append(".\n");
        }
        boolean isValid;
        if (errorMessage.length() != 0) {
            report.setErrorMessage(errorMessage.toString());
            isValid = false;
        } else {
            isValid = true;
        }
        return isValid;
    }

    protected AugeasNode getExistingChildNodeForListMemberPropertyMap(AugeasNode parentNode,
        PropertyDefinitionList propDefList, PropertyMap propMap) {
        // First find all child nodes with the same 'canonical' value as the PropertyMap.
        Augeas augeas = getAugeas();
        String canonicalFilter = parentNode.getPath() + "/*/canonical";
        String canonical = propMap.getSimple("canonical").getStringValue();
        List<String> canonicalPaths = AugeasUtility.matchFilter(augeas, canonicalFilter, canonical);
        if (canonicalPaths.isEmpty()) {
            return null;
        }

        // Now see if there's at least one node in this list with an 'ipaddr' value with the same IP address version as
        // the PropertyMap.
        String ipaddr = propMap.getSimple("ipaddr").getStringValue();
        int ipAddressVersion = (ipaddr.indexOf(':') == -1) ? 4 : 6;
        for (String canonicalPath : canonicalPaths) {
            AugeasNode canonicalNode = new AugeasNode(canonicalPath);
            AugeasNode childNode = canonicalNode.getParent();
            AugeasNode ipaddrNode = new AugeasNode(childNode, "ipaddr");
            String existingIpaddr = augeas.get(ipaddrNode.getPath());
            int existingIpAddressVersion = (existingIpaddr.indexOf(':') == -1) ? 4 : 6;
            if (existingIpAddressVersion == ipAddressVersion) {
                return childNode;
            }
        }
        return null;
    }

    private boolean validateIpAddress(String ipAddress) {
        Matcher ipv4Matcher = IPV4_ADDRESS_PATTERN.matcher(ipAddress);
        if (ipv4Matcher.matches()) {
            return true;
        } else {
            Matcher ipv6Matcher = IPV6_ADDRESS_PATTERN.matcher(ipAddress);
            return (ipv6Matcher.matches());
        }
    }

    private boolean validateDomainName(String domainName) {
        Matcher domainNameMatcher = DOMAIN_NAME_PATTERN.matcher(domainName);
        return (domainNameMatcher.matches());
    }
}
