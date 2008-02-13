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
package org.rhq.plugins.platform;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.SystemInfo;

/**
 * Discovers the platform resource. This will discover one and only one resource. This is an abstract superclass to all
 * the natively supported platform discovery components and the fallback Java-only discovery component.
 *
 * @author John Mazzitelli
 */
public abstract class PlatformDiscoveryComponent implements ResourceDiscoveryComponent {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        if (!isPlatformSupported(context)) {
            return Collections.EMPTY_SET;
        }

        SystemInfo systemInfo = context.getSystemInformation();

        String key;
        String name;
        String version;
        String description = context.getResourceType().getDescription();

        // platform resources are a special case - the plugin container will
        // assign its platform resource's key so it can do so uniquely
        // without using something like "hostname" which might change in the future
        // and if the platform key changes in the future, bad things will happen
        key = "*plugin container will assign this*";

        // ask the system info class to get us the hostname - possibly via the native library
        try {
            name = systemInfo.getHostname();
        } catch (Exception e) {
            name = null;
        }

        // under certain conditions, might not be able to natively get the hostname so try to get it via Java
        if (name == null) {
            try {
                name = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                name = null;
            }

            // we fought the good fight but we just can't get this machine's hostname, give a generic platform name
            if (name == null) {
                name = "Unnamed Platform";
            }
        }

        try {
            version = systemInfo.getOperatingSystemName() + " " + systemInfo.getOperatingSystemVersion();
        } catch (Exception e) {
            version = "?";
        }

        DiscoveredResourceDetails discoveredResource = new DiscoveredResourceDetails(context.getResourceType(), key,
            name, version, description, null, null);

        HashSet<DiscoveredResourceDetails> results = new HashSet<DiscoveredResourceDetails>();
        results.add(discoveredResource);
        return results;
    }

    /**
     * Subclasses will implement this method to tell this class whether or not it can discover the platform of the
     * resource type found in the given context. If this returns <code>false</code>, this subclass discovery component
     * instance does not recognize the platform.
     *
     * <p>It is very important that all subclasses must return <code>false</code> <i>except one</i> for any given
     * platform. That is to say, if running on "RHEL 5", only the subclass that supports RHEL 5 must return <code>
     * true</code>, all others must return <code>false</code>. If running on "Windows XP", only the suclass that
     * supports Windows XP must return <code>true</code>. The only exception to this rule is the
     * {@link JavaPlatformDiscoveryComponent fallback Java platform discovery component}, which will always return
     * <code>true</code> because the fallback Java platform is always supported.</p>
     *
     * @param  context
     *
     * @return <code>true</code> if this instance can support the platform this plugin is running on; <code>false</code>
     *         otherwise
     */
    protected abstract boolean isPlatformSupported(ResourceDiscoveryContext context);
}