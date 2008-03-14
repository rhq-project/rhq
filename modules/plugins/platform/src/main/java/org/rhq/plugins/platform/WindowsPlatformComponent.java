/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.platform;

import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.platform.win.WindowsSoftwareDelegate;
import org.rhq.plugins.platform.win.Win32EventLogDelegate;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

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
