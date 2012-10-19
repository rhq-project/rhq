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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemoveIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.platform.content.RpmPackageDiscoveryDelegate;
import org.rhq.plugins.platform.content.yum.PluginContext;
import org.rhq.plugins.platform.content.yum.YumContext;
import org.rhq.plugins.platform.content.yum.YumProxy;
import org.rhq.plugins.platform.content.yum.YumServer;

public class LinuxPlatformComponent extends PosixPlatformComponent implements ContentFacet {
    // the prefix for all distro trait names
    private static final String DISTRO_TRAIT_NAME_PREFIX = "distro.";

    // trait metric names
    private static final String TRAIT_DISTRO_NAME = DISTRO_TRAIT_NAME_PREFIX + "name";
    private static final String TRAIT_DISTRO_VERSION = DISTRO_TRAIT_NAME_PREFIX + "version";

    private final Log log = LogFactory.getLog(LinuxPlatformComponent.class);

    private ContentContext contentContext;

    private YumServer yumServer = new YumServer();
    private YumProxy yumProxy = new YumProxy();

    private boolean enableContentDiscovery = false;
    private boolean enableInternalYumServer = false;


    @Override
    public void start(ResourceContext context) {
        super.start(context);

        Configuration pluginConfiguration = context.getPluginConfiguration();
        PropertySimple contentProp = pluginConfiguration.getSimple("enableContentDiscovery");
        if (contentProp != null) {
            Boolean bool = contentProp.getBooleanValue();
            this.enableContentDiscovery = (bool != null) ? bool.booleanValue() : false;
        } else {
            this.enableContentDiscovery = false;
        }

        PropertySimple yumProp = pluginConfiguration.getSimple("enableInternalYumServer");
        if (yumProp != null) {
            Boolean bool = yumProp.getBooleanValue();
            this.enableInternalYumServer = (bool != null) ? bool.booleanValue() : false;
        } else {
            this.enableInternalYumServer = false;
        }

        if (this.enableContentDiscovery) {
            RpmPackageDiscoveryDelegate.setSystemInfo(this.resourceContext.getSystemInformation());
            RpmPackageDiscoveryDelegate.checkExecutables();

            //DebPackageDiscoveryDelegate.setSystemInfo(this.resourceContext.getSystemInformation());
            //DebPackageDiscoveryDelegate.checkExecutables();
        }

        startWithContentContext(context.getContentContext());


        return;
    }

    @Override
    public void stop() {

        try {
            yumServer.halt();
        } catch (Exception e) {
            log.warn("Failed to shutdown the yum server", e);
        }

        super.stop();
    }

    private void startWithContentContext(ContentContext context) {
        if (this.enableInternalYumServer) {
            int port = yumPort();
            log.debug("yum port=[" + port + "]");

            this.contentContext = context;
            try {
                YumContext yumContext = new PluginContext(port, this.resourceContext, context);
                yumServer.start(yumContext);
                yumProxy.init(this.resourceContext);

            } catch (Exception e) {
                log.error("Start failed:", e);
            }
        } else {
            log.info("Internal yum server is disabled.");
        }
        return;
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        Set<ResourcePackageDetails> detailsSet = new HashSet<ResourcePackageDetails>();

        if (this.enableContentDiscovery) {
            if (type.getName().equals("rpm")) {
                try {
                    detailsSet = RpmPackageDiscoveryDelegate.discoverPackages(type);
                } catch (IOException e) {
                    log.error("Error while trying to discover RPMs", e);
                }
            }
        }

        return detailsSet;
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        return null;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        try {
            DeployPackagesResponse result = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
            List<String> pkgs = new ArrayList<String>();
            for (ResourcePackageDetails p : packages) {
                String pkgInfo = p.getName();
                String version = p.getVersion();
                if ((version != null) && (!"".equals(version))) {
                    pkgInfo = pkgInfo + "-" + version;
                }
                log.info("Adding <" + pkgInfo + "> to list of packages to deploy, original package object was: " + p);
                pkgs.add(pkgInfo);
                result
                    .addPackageResponse(new DeployIndividualPackageResponse(p.getKey(), ContentResponseResult.SUCCESS));
            }

            yumProxy.install(pkgs);
            return result;
        } catch (Exception e) {
            log.error("Install packages failed", e);
        }

        return new DeployPackagesResponse(ContentResponseResult.FAILURE);
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        try {
            RemovePackagesResponse result = new RemovePackagesResponse(ContentResponseResult.SUCCESS);
            List<String> pkgs = new ArrayList<String>();
            for (ResourcePackageDetails p : packages) {
                pkgs.add(p.getName());
                result
                    .addPackageResponse(new RemoveIndividualPackageResponse(p.getKey(), ContentResponseResult.SUCCESS));
            }

            yumProxy.remove(pkgs);
            return result;
        } catch (Exception e) {
            log.error("Remove packages failed", e);
        }

        return new RemovePackagesResponse(ContentResponseResult.FAILURE);
    }

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        return null;
    }

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metricRequests) {
        super.getValues(report, metricRequests);
        for (MeasurementScheduleRequest metricRequest : metricRequests) {
            if (metricRequest.getName().startsWith(DISTRO_TRAIT_NAME_PREFIX)) {
                report.addData(getDistroTrait(metricRequest));
            }
        }
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("cleanYumMetadataCache".equals(name)) {
            if (this.yumServer.isStarted()) {
                log.info("Cleaning yum metadata");
                yumServer.cleanMetadata();
                yumProxy.cleanMetadata();
                return new OperationResult();
            } else {
                throw new UnsupportedOperationException("Internal yum server is disabled, this operation is a no-op");
            }
        }

        return super.invokeOperation(name, parameters);
    }

    private MeasurementDataTrait getDistroTrait(MeasurementScheduleRequest metricRequest) {
        MeasurementDataTrait trait = new MeasurementDataTrait(metricRequest, "?");
        if (metricRequest.getName().equals(TRAIT_DISTRO_NAME)) {
            trait.setValue(LinuxDistroInfo.getInstance().getName());
        } else if (metricRequest.getName().equals(TRAIT_DISTRO_VERSION)) {
            trait.setValue(LinuxDistroInfo.getInstance().getVersion());
        } else {
            log.error("Being asked to collect an unknown Linux distro trait: " + metricRequest.getName());
        }

        return trait;
    }

    private int yumPort() {
        PropertySimple p = this.resourceContext.getPluginConfiguration().getSimple("yumPort");
        return ((p != null) ? p.getIntegerValue() : 9080);
    }
}