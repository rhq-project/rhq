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

    /**
     * This will build the platform's resource details, where its name and key are by default
     * the plugin container's name.  Subclasses of this platform discovery component
     * are free to override this behavior if they wish to make the platform name different,
     * but under normal circumstances, subclasses will not want to alter the key value.
     * 
     * @see ResourceDiscoveryComponent#discoverResources(ResourceDiscoveryContext)
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext context) {
        if (!isPlatformSupported(context)) {
            return Collections.EMPTY_SET;
        }

        SystemInfo systemInfo = context.getSystemInformation();

        // platform resources will use the plugin container name as its key.
        // we are guaranteed this string is unique across all agents/plugin containers.
        // (it is usually the hostname, but is not guaranteed to be)
        String key = determineResourceKey(context);

        // build the resource name based on the plugin container name and possibly hostname
        String name = determineResourceName(context);

        // use the platform type description as the description for this resource
        String description = context.getResourceType().getDescription();

        String version;
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
     * This will determine what the new platform's resource key should be. This default
     * implementation first tries to use the plugin container's name which is guaranteed
     * to be unique across all agents/plugin containers.  If, for some odd reason, it is
     * <code>null</code>, the platform's hostname is used.  This is less than optimal
     * but the plugin container's name will never be <code>null</code> under virtually
     * all use-cases (except perhaps in test scenarios).
     * 
     * @param context the discovery context used to get the plugin container name or host name if its needed
     * 
     * @return the new platform's resource key
     */
    protected String determineResourceKey(ResourceDiscoveryContext context) {
        String name = context.getPluginContainerName();

        if (name == null) {
            name = getHostname(context.getSystemInformation());
        }

        return name;
    }

    /**
     * This will determine what the new platform's resource name should be. This default
     * implementation first tries to use the plugin container's name which is guaranteed
     * to be unique across all agents/plugin containers.  If, for some odd reason, it is
     * <code>null</code>, the platform's hostname is used. If the plugin container's name
     * is not <code>null</code> but it is not the hostname of the platform (as detected
     * by {@link #getHostname(SystemInfo)}), the name will consist of the platform's
     * hostname appeneded with the plugin container's name.
     * 
     * @param context the discovery context used to get the plugin container name or host name if its needed
     * 
     * @return the new platform's resource key
     */
    protected String determineResourceName(ResourceDiscoveryContext context) {
        String pcName = context.getPluginContainerName();
        String hostName = getHostname(context.getSystemInformation());
        String resourceName = hostName;

        if ((pcName != null) && (!pcName.equals(hostName))) {
            resourceName = hostName + " (" + pcName + ")";
        }

        return resourceName;
    }

    /**
     * Tries to determine this platform's hostname, using the (possibly native)
     * system info API.  If, for whatever reason, this method cannot determine the
     * hostname, a generic "Unnamed Platform" String will be returned (this will
     * rarely, if ever, happen under normal situations).
     * 
     * @param systemInformation
     * 
     * @return the platform's hostname (will never be <code>null</code>)
     */
    protected String getHostname(SystemInfo systemInformation) {
        String name;

        // ask the system info class to get us the hostname - possibly via the native library
        try {
            name = systemInformation.getHostname();
        } catch (Exception e) {
            name = null;
        }

        // under certain conditions, we might not be able to natively get the hostname so try to get it via Java
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
        return name;
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