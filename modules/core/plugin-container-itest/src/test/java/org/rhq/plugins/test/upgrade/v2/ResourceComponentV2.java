/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.plugins.test.upgrade.v2;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * @author Lukas Krejci
 */
public class ResourceComponentV2 implements ResourceComponent<ResourceComponent<?>>, ContentFacet, OperationFacet {

    private ResourceContext<ResourceComponent<?>> context;

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException,
        Exception {
        this.context = context;

        new ResourceContextStress(context, "start").stress();
    }

    @Override
    public void stop() {
        new ResourceContextStress(context, "stop").stress();
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        return new OperationResult();
    }

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        // TODO Auto-generated method stub
        return null;
    }
}
