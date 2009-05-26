/*
 * Jopr Management Platform
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
package org.jboss.on.common.jbossas;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;

/**
 * This is a base class for the ContentFacet implementation delegates for JBoss AS plugins.
 * 
 * Much of the implementation is the same for both the AS 4 and AS5 and is implemented
 * here. Subclasses (used in the server component implementations) specialize for the
 * specific requirements of the different JBoss AS versions.
 *  
 * @author Lukas Krejci
 */
public abstract class AbstractJBossASContentFacetDelegate implements ContentFacet {

    private final Log log = LogFactory.getLog(AbstractJBossASContentFacetDelegate.class);
    
    public static final String PACKAGE_TYPE_PATCH = "cumulativePatch";
    public static final String PACKAGE_TYPE_LIBRARY = "library";

	private JBPMWorkflowManager workflowManager;
	
	protected AbstractJBossASContentFacetDelegate(JBPMWorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}
	
	/**
	 * This default implementation supports deploying a single patch. For that the packages set
	 * must contain a single package with the type name equal to {@link #PACKAGE_TYPE_PATCH}.
	 * <p>
	 * Packages of type {@link #PACKAGE_TYPE_LIBRARY} are unsupported, all other package types 
	 * are silently ignored.
	 * 
	 * @throws UnsupportedOperationException 
	 *             if the packages set contains a package of type {@link #PACKAGE_TYPE_LIBRARY}
	 * 
	 * @return a package response describing th results of the deployment.
	 */
	public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages,
			ContentServices contentServices) {
        ContentResponseResult overallResult = ContentResponseResult.SUCCESS;
        List<DeployIndividualPackageResponse> individualResponses = new ArrayList<DeployIndividualPackageResponse>(
            packages.size());

        for (ResourcePackageDetails pkg : packages) {
            log.info("Attempting to deploy package: " + pkg);

            String packageTypeName = pkg.getPackageTypeName();
            if (packageTypeName.equals(PACKAGE_TYPE_PATCH)) {

                if (packages.size() > 1) {
                    log.warn("Attempt to install more than one patch at a time, installation aborted.");

                    DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
                    response
                        .setOverallRequestErrorMessage("When deploying a patch, no other packages may be deployed at the same time.");
                    return response;
                }

                try {
                    DeployIndividualPackageResponse response = workflowManager.run(pkg);
                    individualResponses.add(response);

                    if (response.getResult() == ContentResponseResult.FAILURE) {
                        overallResult = ContentResponseResult.FAILURE;
                    }
                } catch (Throwable throwable) {
                    log.error("Error deploying package: " + pkg, throwable);

                    // Don't forget to provide an individual response for the failed package.
                    DeployIndividualPackageResponse response = new DeployIndividualPackageResponse(pkg.getKey(),
                        ContentResponseResult.FAILURE);
                    response.setErrorMessageFromThrowable(throwable);
                    individualResponses.add(response);

                    overallResult = ContentResponseResult.FAILURE;
                }
            } else if (packageTypeName.equals(PACKAGE_TYPE_LIBRARY)) {
                throw new UnsupportedOperationException("Deployment of new libraries is not supported by the plugin.");
            }
        }

        DeployPackagesResponse response = new DeployPackagesResponse(overallResult);
        response.getPackageResponses().addAll(individualResponses);

        return response;
	}

	/**
	 * The default implementation of this method merely throws an
	 * unsupported operation exception.
	 * 
	 * @throws UnsupportedOperationException
	 * 
	 * @see ContentFacet#discoverDeployedPackages(PackageType)
	 */
	public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
		throw new UnsupportedOperationException();
	}

	public List<DeployPackageStep> generateInstallationSteps(
			ResourcePackageDetails packageDetails) {
        log.info("Translating installation steps for package: " + packageDetails);

        List<DeployPackageStep> steps = null;

        String packageTypeName = packageDetails.getPackageTypeName();
        if (packageTypeName.equals(PACKAGE_TYPE_PATCH)) {
            try {
                steps = workflowManager.translateSteps(packageDetails);
            } catch (Exception e) {
                log.error("Error translating installation steps for package: " + packageDetails, e);
            }

            log.info("Translated number of steps: " + (steps != null ? steps.size() : null));

        }

        return steps;
	}

	/**
	 * The default implementation of this method merely throws an
	 * unsupported operation exception.
	 * 
	 * @throws UnsupportedOperationException
	 * 
	 * @see ContentFacet#removePackages(Set)
	 */
	public RemovePackagesResponse removePackages(
			Set<ResourcePackageDetails> packages) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The default implementation of this method merely throws an
	 * unsupported operation exception.
	 * 
	 * @throws UnsupportedOperationException
	 * 
	 * @see ContentFacet#retrievePackageBits(ResourcePackageDetails)
	 */
	public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return the workflow manager
	 */
	protected JBPMWorkflowManager getWorkflowManager() {
		return workflowManager;
	}
}
