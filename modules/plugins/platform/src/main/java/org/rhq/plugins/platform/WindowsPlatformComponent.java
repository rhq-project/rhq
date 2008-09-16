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

import java.io.InputStream;
import java.util.List;
import java.util.Set;

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
    private Win32EventLogDelegate eventLogDelegate;

    public void start(ResourceContext context) {
        super.start(context);
        if (context.getPluginConfiguration().getSimple("eventTrackingEnabled").getBooleanValue()) {
            eventLogDelegate = new Win32EventLogDelegate(context.getPluginConfiguration());
            eventLogDelegate.open();
            context.getEventContext().registerEventPoller(eventLogDelegate, 60);
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
        return new WindowsSoftwareDelegate().discoverInstalledSoftware(type);
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;
    }
}
