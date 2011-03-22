/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.platform;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.platform.win.Win32EventLogDelegate;
import org.rhq.plugins.platform.win.WindowsSoftwareDelegate;

/**
 * @author Greg Hinkle
 */
public class WindowsPlatformComponent extends PlatformComponent implements ContentFacet {
    private final Log log = LogFactory.getLog(WindowsPlatformComponent.class);

    private Win32EventLogDelegate eventLogDelegate;
    private boolean enableContentDiscovery = false;

    public void start(ResourceContext context) {
        super.start(context);
        Configuration pluginConfiguration = context.getPluginConfiguration();
        if (pluginConfiguration.getSimple("eventTrackingEnabled").getBooleanValue()) {
            try {
                eventLogDelegate = new Win32EventLogDelegate(pluginConfiguration);
                eventLogDelegate.open();
                context.getEventContext().registerEventPoller(eventLogDelegate, 60);
            } catch (Throwable t) {
                log.error("Failed to start the event logger. Will not be able to capture Windows events", t);
                if (eventLogDelegate != null) {
                    try {
                        eventLogDelegate.close();
                    } catch (Throwable t2) {
                    } finally {
                        eventLogDelegate = null;
                    }
                }
            }
        }

        PropertySimple contentProp = pluginConfiguration.getSimple("enableContentDiscovery");
        if (contentProp != null) {
            Boolean bool = contentProp.getBooleanValue();
            this.enableContentDiscovery = (bool != null) ? bool.booleanValue() : false;
        } else {
            this.enableContentDiscovery = false;
        }
    }

    public void stop() {
        super.stop();
        if (eventLogDelegate != null) {
            resourceContext.getEventContext().unregisterEventPoller(eventLogDelegate.getEventType());
            eventLogDelegate.close();
        }
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        return null;
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        return null;
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        if (this.enableContentDiscovery) {
            return new WindowsSoftwareDelegate().discoverInstalledSoftware(type);
        } else {
            return new HashSet<ResourcePackageDetails>();
        }
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;
    }
}
