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
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.platform.content.RpmPackageDiscoveryDelegate;
import org.rhq.plugins.platform.content.yum.PluginContext;
import org.rhq.plugins.platform.content.yum.YumContext;
import org.rhq.plugins.platform.content.yum.YumProxy;
import org.rhq.plugins.platform.content.yum.YumServer;

public class LinuxPlatformComponent extends PlatformComponent implements ContentFacet {
    // Constants  --------------------------------------------

    // the prefix for all distro trait names
    private static final String DISTRO_TRAIT_NAME_PREFIX = "distro.";

    // trait metric names
    private static final String TRAIT_DISTRO_NAME = DISTRO_TRAIT_NAME_PREFIX + "name";
    private static final String TRAIT_DISTRO_VERSION = DISTRO_TRAIT_NAME_PREFIX + "version";

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(LinuxPlatformComponent.class);

    private ContentContext contentContext;

    private YumServer yumServer = new YumServer();
    private YumProxy yumProxy = new YumProxy();

    @Override
    public void start(ResourceContext context) {
        super.start(context);
        RpmPackageDiscoveryDelegate.setSystemInfo(this.resourceContext.getSystemInformation());
        RpmPackageDiscoveryDelegate.checkExecutables();
        /*
         *    DebPackageDiscoveryDelegate.setSystemInfo(this.resourceContext.getSystemInformation());
         * DebPackageDiscoveryDelegate.checkExecutables();
         */
    }

    @Override
    public void stop() {
        yumServer.halt();
        super.stop();
    }

    public void startContentFacet(ContentContext context) {
        contentContext = context;
        try {
            YumContext yumContext = new PluginContext(yumPort(), resourceContext, context);
            yumServer.start(yumContext);
            yumProxy.init(resourceContext);
        } catch (Exception e) {
            log.error("Start failed:", e);
        }
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        Set<ResourcePackageDetails> detailsSet = new HashSet<ResourcePackageDetails>();
        if (type.getName().equals("rpm")) {
            try {
                detailsSet = RpmPackageDiscoveryDelegate.discoverPackages(type);
            } catch (IOException e) {
                log.error("Error while trying to discover RPMs", e);
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
                pkgs.add(p.getName());
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

    /*
     * public Set<InstalledPackageDetails> getInstalledPackages(PackageType type) { Set<InstalledPackageDetails>
     * detailsSet = new HashSet<InstalledPackageDetails>(); if (type.getName().equals("rpm")) {   try   {
     * detailsSet = RpmPackageDiscoveryDelegate.discoverPackages(type);   }   catch (IOException e)   {
     * log.error("Error while trying to discover RPMs", e);   } } else if (type.getName().equals("deb")) {
     */
    /*
     *       try      {         detailsSet = DebPackageDiscoveryDelegate.discoverPackages(type);      }      catch
     * (IOException e)      {         log.error("Error while trying to discover DEBs", e);      }
     */
    /*
     *    }   return detailsSet;
     *
     * }
     */

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metricRequests) {
        super.getValues(report, metricRequests);
        for (MeasurementScheduleRequest metricRequest : metricRequests) {
            if (metricRequest.getName().startsWith(DISTRO_TRAIT_NAME_PREFIX)) {
                report.addData(getDistroTrait(metricRequest));
            }
        }
    }

    public void startOperationFacet(OperationContext context) {
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("cleanYumMetadataCache".equals(name)) {
            log.info("Cleaning yum metadata");
            yumServer.cleanMetadata();
            yumProxy.cleanMetadata();
            return new OperationResult();
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
        PropertySimple p = resourceContext.getPluginConfiguration().getSimple("yumPort");
        return ((p != null) ? p.getIntegerValue() : 9080);
    }
}