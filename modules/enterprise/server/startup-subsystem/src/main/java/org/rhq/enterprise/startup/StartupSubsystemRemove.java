/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.enterprise.startup;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handler responsible for removing the deployments.
 *
 * @author John Mazzitelli
 */
class StartupSubsystemRemove extends AbstractRemoveStepHandler {

    static final StartupSubsystemRemove INSTANCE = new StartupSubsystemRemove();

    private StartupSubsystemRemove() {
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model)
        throws OperationFailedException {

        // Add a step to remove the the deployments
        if (requiresRuntime(context)) { // only add the step if we are going to actually undeploy the ear
            PathAddress deploymentAddress = PathAddress.pathAddress(PathElement.pathElement(DEPLOYMENT,
                StartupExtension.DEPLOYMENT_APP_EAR));
            ModelNode op = Util.getEmptyOperation(REMOVE, deploymentAddress.toModelNode());
            ImmutableManagementResourceRegistration rootResourceRegistration = context.getRootResourceRegistration();
            OperationStepHandler handler = rootResourceRegistration.getOperationHandler(deploymentAddress, REMOVE);
            context.addStep(op, handler, OperationContext.Stage.MODEL);
        }
        super.performRemove(context, operation, model);
    }

}
