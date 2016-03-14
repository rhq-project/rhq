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

package org.rhq.modules.plugins.wildfly10;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

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
public class VersionedRuntimeDiscovery extends AbstractVersionedSubsystemDiscovery {
    private static final Log LOG = LogFactory.getLog(VersionedRuntimeDiscovery.class);

    private static final Pattern FORWARD_SLASH_PATTERN = Pattern.compile("/");
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BaseComponent<?>> context)
        throws Exception {

        // Perform the standard discovery. This can return resources with versions in the name,
        // key and path.
        Set<DiscoveredResourceDetails> details = super.discoverResources(context);

        if (DISABLED || null == details || details.isEmpty()) {
            return details;
        }

        // Now, post-process the discovery as needed.  We need to strip the versions from the
        // resource name and the resource key.  We want to leave them in the path plugin config
        // property, that value reflects the actual DMR used to query EAP.
        // Note that the version string is always set to the parent's version for these
        // resources which are logically part of the umbrella deployment.

        // Work with a list because we may update the key, which is used in the DiscoveredResourceDetails.equals()
        ArrayList<DiscoveredResourceDetails> updatedDetails = new ArrayList<DiscoveredResourceDetails>(details);
        HashMap<String, Integer> keyCount = new HashMap<String, Integer>(updatedDetails.size());
        details.clear();

        Configuration config = context.getDefaultPluginConfiguration();
        String configPath = config.getSimpleValue("path", "");
        boolean isType = (configPath == null || configPath.isEmpty() || configPath.contains("="));

        for (DiscoveredResourceDetails detail : updatedDetails) {
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
            for (String segment : FORWARD_SLASH_PATTERN.split(detail.getResourceName())) {
                MATCHER.reset(segment);
                if (MATCHER.matches()) {
                    sb.append(slash);
                    sb.append(MATCHER.group(1)).append(MATCHER.group(3));
                } else {
                    sb.append(slash);
                    sb.append(segment);
                }
                slash = "/";
            }
            detail.setResourceName(sb.toString());

            sb = new StringBuilder();
            String comma = "";
            for (String outerSegment : COMMA_PATTERN.split(detail.getResourceKey())) {
                sb.append(comma);
                comma = ",";
                slash = "";
                for (String segment : FORWARD_SLASH_PATTERN.split(outerSegment)) {
                    sb.append(slash);
                    slash = "/";
                    MATCHER.reset(segment);
                    if (MATCHER.matches()) {
                        sb.append(MATCHER.group(1)).append(MATCHER.group(3));
                    } else {
                        sb.append(segment);
                    }
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
}
