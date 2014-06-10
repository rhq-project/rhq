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

package org.rhq.enterprise.startup;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author John Mazzitelli
 */
class StartupSubsystemAdd extends AbstractAddStepHandler {

    private static final Logger LOG = Logger.getLogger(StartupSubsystemAdd.class);

    static final StartupSubsystemAdd INSTANCE = new StartupSubsystemAdd();

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource)
        throws OperationFailedException {

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor("", StartupCrippledDeploymentProcessor.PHASE,
                    StartupCrippledDeploymentProcessor.PRIORITY, new StartupCrippledDeploymentProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        try {
            if (requiresRuntime(context)) { // only add the step if we are going to actually deploy the ear
                PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,
                    StartupExtension.DEPLOYMENT_APP_EAR));
                ModelNode op = Util.getEmptyOperation(ADD, deploymentAddress.toModelNode());
                op.get(ENABLED).set(true);
                op.get(PERSISTENT).set(false); // prevents writing this deployment out to standalone.xml

                Module module = Module.forClass(getClass());

                URL url = module.getExportedResource(StartupExtension.DEPLOYMENT_APP_EAR);
                if (url == null) {
                    throw new FileNotFoundException("Could not find the EAR");
                }
                ModelNode contentItem = new ModelNode();

                boolean explodedDeployment = true; // this is here just to keep the code around that deploys if we are unexploded
                if (explodedDeployment) {
                    String urlString = new File(url.toURI()).getAbsolutePath();
                    if (!(new File(urlString).exists())) {
                        throw new FileNotFoundException("Missing the EAR at [" + urlString + "]");
                    }
                    contentItem.get(PATH).set(urlString);
                    contentItem.get(ARCHIVE).set(false);
                } else {
                    String urlString = url.toExternalForm();
                    contentItem.get(URL).set(urlString);
                }

                op.get(CONTENT).add(contentItem);

                ImmutableManagementResourceRegistration rootResourceRegistration;
                rootResourceRegistration = context.getRootResourceRegistration();
                OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress, ADD);

                context.addStep(op, handler, OperationContext.Stage.MODEL);
                return;
            }
        } catch (Exception e) {
            throw new OperationFailedException("The RHQ EAR failed to be deployed: " + e, e);
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // We overrode the code that calls this method
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

}
