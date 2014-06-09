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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;

/**
 * Discover subsystems. We need to distinguish two cases denoted by the path
 * plugin config:
 * <ul>
 *     <li>Path is a single 'word': here the value denotes a key in the resource path
 *     of AS7, that identifies a child type see e.g. the Connectors below the JBossWeb
 *     service in the plugin descriptor. There can be multiple resources of the given
 *     type. In addition it is possible that a path entry in configuration shares multiple
 *     types that are separated by the pipe symbol.</li>
 *     <li>Path is a key-value pair (e.g. subsystem=web ). This denotes a singleton
 *     subsystem with a fixes path within AS7 (perhaps below another resource in the
 *     tree.</li>
 * </ul>
 *
 * This subclass adds logic for discovering different versions of the same logical resource,
 * by stripping version info out of the path, removing it from the resourceName and setting it
 * as the resourceVersion.
 * <p/>
 * The default version matching pattern is designed to match maven-like version stamping:
 * <code>name-version.ext</code> being the basic format.  The version must minimally contain
 * <code>major.minor</code> values.  See Maven documentation for more information.  The default
 * pattern is <code>"^(.*?)-([0-9]+\\.[0-9].*)(\\..*)$"</code>. The same pattern is applied to
 * all <code>Deployment</code> and <code>Subdeployment</code> artifacts.
 * <p/>
 * To override the default pattern the following environment variable can be defined:
 * <code>rhq.as7.VersionedSubsystemDiscovery.pattern=theDesiredRegexPattern</code>. The regex
 * *must* capture three groups as does the default. Group1=name, Group2=version, Group3=extension.
 * <p/>
 * To disable versioned discovery and maintain version strings in the deployment names, set
 * <code>rhq.as7.VersionedSubsystemDiscovery.pattern=disable</code>
 *
 * @author Jay Shaughnessy
 */
public class VersionedSubsystemDiscovery extends AbstractVersionedSubsystemDiscovery implements ResourceUpgradeFacet {

    private final Log log = LogFactory.getLog(this.getClass());

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        // Perform the standard discovery. This can return resources with versions in the name,
        // key and path.
        Set<DiscoveredResourceDetails> details = super.discoverResources(context);

        if (DISABLED) {
            return details;
        }

        // Now, post-process the discovery as needed.  We need to strip the versions from the
        // resource name and the resource key.  We want to leave them in the path plugin config
        // property, that value reflects the actual DMR used to query EAP.
        // The stripped versions are then used to set the resource version string.

        for (DiscoveredResourceDetails detail : details) {
            MATCHER.reset(detail.getResourceName());
            if (MATCHER.matches()) {
                // reset the resource name with the stripped value
                detail.setResourceName(MATCHER.group(1) + MATCHER.group(3));

                // The version string for a subdeployment must incorporate the parent deployment's version
                // so that we detect an overall version change if the parent is re-deployed.  Without this
                // the Subdeployment will not be properly updated if its version remains unchanged in the
                // updated Deployment.
                if (SUBDEPLOYMENT_TYPE.equals(context.getResourceType().getName())) {
                    String parentResourceVersion = context.getParentResourceContext().getVersion();
                    parentResourceVersion = (null == parentResourceVersion) ? "" : (parentResourceVersion + "/");
                    detail.setResourceVersion(parentResourceVersion + MATCHER.group(2));
                } else {
                    detail.setResourceVersion(MATCHER.group(2));
                }
            }

            StringBuilder sb = new StringBuilder();
            String comma = "";
            for (String segment : detail.getResourceKey().split(",")) {
                sb.append(comma);
                comma = ",";
                MATCHER.reset(segment);
                if (MATCHER.matches()) {
                    sb.append(MATCHER.group(1) + MATCHER.group(3));
                } else {
                    sb.append(segment);
                }
            }
            detail.setResourceKey(sb.toString());
        }

        return details;
    }

    // The Matching logic here is the same as above, but instead of setting the discovery details we
    // set new values in the upgrade report for name, version and key.
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

            // The version string for a subdeployment must incorporate the parent deployment's version
            // so that we detect an overall version change if the parent is re-deployed.  Without this
            // the Subdeployment will not be properly updated if its version remains unchanged in the
            // updated Deployment.
            if (SUBDEPLOYMENT_TYPE.equals(inventoriedResource.getResourceType().getName())) {
                String parentResourceVersion = inventoriedResource.getParentResourceContext().getVersion();
                parentResourceVersion = (null == parentResourceVersion) ? "" : (parentResourceVersion + "/");
                result.setNewVersion(parentResourceVersion + MATCHER.group(2));
            } else {
                result.setNewVersion(MATCHER.group(2));
            }
        }

        StringBuilder sb = new StringBuilder();
        String comma = "";
        boolean upgradeKey = false;
        for (String segment : inventoriedResource.getResourceKey().split(",")) {
            sb.append(comma);
            comma = ",";
            MATCHER.reset(segment);
            if (MATCHER.matches()) {
                upgradeKey = true;
                sb.append(MATCHER.group(1) + MATCHER.group(3));
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

        if (null != result && log.isDebugEnabled()) {
            log.debug("Requesting upgrade: " + result);
        }

        return result;
    }
}
