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
package org.rhq.plugins.cobbler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.augeas.Augeas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import org.rhq.plugins.augeas.helper.AugeasUtility;
import org.rhq.plugins.platform.PlatformComponent;

/**
 * The ResourceComponent for the "Cobbler File" ResourceType.
 *
 * @author Ian Springer
 */
public class CobblerComponent extends AugeasConfigurationComponent<PlatformComponent> implements
    ResourceConfigurationFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    public void start(ResourceContext<PlatformComponent> resourceContext) throws InvalidPluginConfigurationException,
        Exception {
        super.start(resourceContext);
    }

    @Override
    protected void setupAugeasModules(Augeas augeas) {
        augeas.set("/augeas/load/CobblerSettings/lens", "CobblerSettings.lns");
        augeas.set("/augeas/load/CobblerSettings/incl[1]", "/etc/cobbler/settings");
        augeas.set("/augeas/load/CobblerModules/lens", "CobblerModules.lns");
        augeas.set("/augeas/load/CobblerModules/incl[1]", "/etc/cobbler/modules.conf");
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
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

    @Override
    public Set<RawConfiguration> loadRawConfigurations() {
        Set<RawConfiguration> configs = new HashSet<RawConfiguration>();
        RawConfiguration config = new RawConfiguration();
        return null;
    }

    @Override
    public Configuration loadStructuredConfiguration() {
        try {
            return loadResourceConfiguration();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public RawConfiguration mergeRawConfiguration(Configuration from, RawConfiguration to) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void mergeStructuredConfiguration(RawConfiguration from, Configuration to) {
        // TODO Auto-generated method stub

    }

    @Override
    public void persistRawConfiguration(RawConfiguration rawConfiguration) {
        // TODO Auto-generated method stub

    }

    @Override
    public void persistStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub

    }

    @Override
    public void validateRawConfiguration(RawConfiguration rawConfiguration) {
        // TODO Auto-generated method stub

    }

    @Override
    public void validateStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub

    }

}
