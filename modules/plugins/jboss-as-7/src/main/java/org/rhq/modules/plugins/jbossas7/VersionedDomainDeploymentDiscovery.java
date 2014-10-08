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

package org.rhq.modules.plugins.jbossas7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

/**
 * Discover domain deployments. This is analogous to {@link VersionedSubsystemDiscovery} but addresses only
 * domain deployments. See {@link VersionedSubsystemDiscovery} for more on controlling/overriding the version handling.
 *
 * @author Jay Shaughnessy
 */
public class VersionedDomainDeploymentDiscovery extends AbstractVersionedDomainDeploymentDiscovery implements
    ResourceUpgradeFacet {

    private static final Log LOG = LogFactory.getLog(VersionedDomainDeploymentDiscovery.class);

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        // Perform the standard discovery. This can return deployments with versions in the name,
        // key and path.
        Set<DiscoveredResourceDetails> details = super.discoverResources(context);

        if (DISABLED || null == details || details.isEmpty()) {
            return details;
        }

        // Now, post-process the discovery as needed.  We need to strip the versions from the
        // resource name and the resource key.  We want to leave them in the path plugin config
        // property, that value reflects the actual DMR used to query EAP.
        // The stripped versions are then used to set the resource version string.

        // Work with a list because we may update the key, which is used in the DiscoveredResourceDetails.equals()
        ArrayList<DiscoveredResourceDetails> updatedDetails = new ArrayList<DiscoveredResourceDetails>(details);
        HashMap<String, Integer> keyCount = new HashMap<String, Integer>(updatedDetails.size());
        details.clear();

        // domain deployments are not actually deployed, they are basically just staged for deployment, so
        // they are fairly simple, they don't have subdeployments, etc...
        for (DiscoveredResourceDetails detail : updatedDetails) {
            MATCHER.reset(detail.getResourceName());
            if (MATCHER.matches()) {
                // reset the resource name with the stripped value
                detail.setResourceName(MATCHER.group(1) + MATCHER.group(3));
                detail.setResourceVersion(MATCHER.group(2));
            }

            StringBuilder sb = new StringBuilder();
            String comma = "";
            for (String segment : COMMA_PATTERN.split(detail.getResourceKey())) {
                sb.append(comma);
                comma = ",";
                MATCHER.reset(segment);
                if (MATCHER.matches()) {
                    sb.append(MATCHER.group(1)).append(MATCHER.group(3));
                } else {
                    sb.append(segment);
                }
            }
            detail.setResourceKey(sb.toString());
            Integer count = keyCount.get(detail.getResourceKey());
            keyCount.put(detail.getResourceKey(), (null == count ? 1 : ++count));
        }

        // Now, make sure that after we've stripped the versions that we don't end up with multiple discoveries
        // for the same key, this is an indication that there are multiple versions of the same Deployment deployed.
        // In this case we remove the duplicates and issue a warning so the user can hopefully rectify the situation.
        for (Map.Entry<String, Integer> entry : keyCount.entrySet()) {
            if (entry.getValue() > 1) {
                LOG.warn("Discovered multiple resources with resource key [" + entry.getKey()
                    + "].  This is not allowed and they will be removed from discovery.  This is typically caused by "
                    + "having multiple versions of the same Deployment deployed.  To solve the problem either remove "
                    + "all but one version of the problem deployment or disable versioned deployment handling by "
                    + "setting -Drhq.as7.VersionedSubsystemDiscovery.pattern=disable for the agent.");
                for (Iterator<DiscoveredResourceDetails> i = updatedDetails.iterator(); i.hasNext();) {
                    DiscoveredResourceDetails detail = i.next();
                    if (detail.getResourceKey().equals(entry.getKey())) {
                        i.remove();
                    }
                }
            }
        }

        details.addAll(updatedDetails);
        return details;
    }

    // The Matching logic here is the same as above, but instead of setting the discovery details we
    // set new values in the upgrade report for name, version and key.  Note that if multiple resources
    // upgrade to the same resource key it will be caught and fail downstream.
    @Override
    public ResourceUpgradeReport upgrade(ResourceUpgradeContext inventoriedResource) {
        ResourceUpgradeReport result = null;

        if (DISABLED) {
            return result;
        }

        MATCHER.reset(inventoriedResource.getName());
        if (MATCHER.matches()) {
            result = new ResourceUpgradeReport();
            result.setForceGenericPropertyUpgrade(true); // It is critical the name and version get upgraded

            // reset the resource name with the stripped value
            result.setNewName(MATCHER.group(1) + MATCHER.group(3));
            result.setNewVersion(MATCHER.group(2));
        }

        StringBuilder sb = new StringBuilder();
        String comma = "";
        boolean upgradeKey = false;
        for (String segment : COMMA_PATTERN.split(inventoriedResource.getResourceKey())) {
            sb.append(comma);
            comma = ",";
            MATCHER.reset(segment);
            if (MATCHER.matches()) {
                upgradeKey = true;
                sb.append(MATCHER.group(1)).append(MATCHER.group(3));
            } else {
                sb.append(segment);
            }
        }
        if (upgradeKey) {
            if (null == result) {
                result = new ResourceUpgradeReport();
            }
            result.setNewResourceKey(sb.toString());
        }

        if (null != result && LOG.isDebugEnabled()) {
            LOG.debug("Requesting upgrade: " + result);
        }

        return result;
    }
}
