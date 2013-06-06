/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.startup.deployment;

import static org.rhq.enterprise.startup.StartupExtension.DEPLOYMENT_APP_EAR;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;

/**
 * A DUP which detects RHQ EAR deployment.
 *
 * @author Thomas Segismont
 */
public class RhqInitializationProcessor implements DeploymentUnitProcessor {

    private static final Logger LOG = Logger.getLogger(RhqInitializationProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getParent() == null && DEPLOYMENT_APP_EAR.equals(deploymentUnit.getName())
            && DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            LOG.info("Found " + DEPLOYMENT_APP_EAR + " deployment");
            RhqDeploymentMarker.mark(deploymentUnit);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
