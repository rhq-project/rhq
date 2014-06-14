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

import org.rhq.core.domain.configuration.Configuration;
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
 * Similar to {@link VersionedSubsystemDiscovery} but the logic is slightly different for the Runtime
 * resources under a <code>Deployment</code> or <code>Subdeployment</code>.
 *
 * @see VersionedSubsystemDiscovery
 *
 * @author Jay Shaughnessy
 */
public class VersionedRuntimeDiscovery extends AbstractVersionedSubsystemDiscovery implements ResourceUpgradeFacet {

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
        // Note that the version string is always set to the parent's version for these
        // resources which are logically part of the umbrella deployment.

        Configuration config = context.getDefaultPluginConfiguration();
        String configPath = config.getSimpleValue("path", "");
        boolean isType = (configPath == null || configPath.isEmpty() || configPath.contains("="));

        for (DiscoveredResourceDetails detail : details) {
            BaseComponent parentComponent = context.getParentResourceComponent();
            String parentPath = (null != parentComponent.getPath()) ? parentComponent.getPath() : "";
            detail.setResourceVersion((parentPath.isEmpty()) ? null : context.getParentResourceContext().getVersion());

            if (isType) {
                detail.setResourceKey((parentPath.isEmpty()) ? configPath : (parentComponent.key + "," + configPath));
                continue;
            }

            // Note that in addition to the comma-separated segments of the DMR address, certain runtime
            // resources (e.g. "Hibernate Persistence Unit") have a forwardSlash-separated segments of
            // their own.

            StringBuilder sb = new StringBuilder();
            String slash = "";
            for (String segment : detail.getResourceName().split("/")) {
                MATCHER.reset(segment);
                if (MATCHER.matches()) {
                    sb.append(slash);
                    sb.append(MATCHER.group(1) + MATCHER.group(3));
                } else {
                    sb.append(slash);
                    sb.append(segment);
                }
                slash = "/";
            }
            detail.setResourceName(sb.toString());

            sb = new StringBuilder();
            String comma = "";
            for (String outerSegment : detail.getResourceKey().split(",")) {
                sb.append(comma);
                comma = ",";
                slash = "";
                for (String segment : outerSegment.split("/")) {
                    sb.append(slash);
                    slash = "/";
                    MATCHER.reset(segment);
                    if (MATCHER.matches()) {
                        sb.append(MATCHER.group(1) + MATCHER.group(3));
                    } else {
                        sb.append(segment);
                    }
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

        // Note that in addition to the comma-separated segments of the DMR address, certain runtime
        // resources (e.g. "Hibernate Persistence Unit") have a forwardSlash-separated segments of
        // their own.

        StringBuilder sb = new StringBuilder();
        String slash = "";
        boolean upgradeName = false;
        for (String segment : inventoriedResource.getName().split("/")) {
            MATCHER.reset(segment);
            if (MATCHER.matches()) {
                upgradeName = true;
                sb.append(slash);
                sb.append(MATCHER.group(1) + MATCHER.group(3));
            } else {
                sb.append(slash);
                sb.append(segment);
            }
            slash = "/";
        }

        if (upgradeName) {
            if (null == result) {
                result = new ResourceUpgradeReport();
            }
            result.setForceGenericPropertyUpgrade(true); // It is critical the name get upgraded
            result.setNewName(sb.toString());
        }

        // Note that the version string is always set to the parent's version for these
        // resources which are logically part of the umbrella deployment.
        String parentVersion = inventoriedResource.getParentResourceContext().getVersion();
        String currentVersion = inventoriedResource.getVersion();
        if ((currentVersion != parentVersion)
            && ((null == currentVersion && null != parentVersion) || !currentVersion.equals(parentVersion))) {
            if (null == result) {
                result = new ResourceUpgradeReport();
            }
            result.setForceGenericPropertyUpgrade(true); // It is critical the version get upgraded
            result.setNewVersion(parentVersion);
        }

        sb = new StringBuilder();
        String comma = "";
        boolean upgradeKey = false;
        for (String outerSegment : inventoriedResource.getResourceKey().split(",")) {
            sb.append(comma);
            comma = ",";
            slash = "";
            for (String segment : outerSegment.split("/")) {
                sb.append(slash);
                slash = "/";
                MATCHER.reset(segment);
                if (MATCHER.matches()) {
                    upgradeKey = true;
                    sb.append(MATCHER.group(1) + MATCHER.group(3));
                } else {
                    sb.append(segment);
                }
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
