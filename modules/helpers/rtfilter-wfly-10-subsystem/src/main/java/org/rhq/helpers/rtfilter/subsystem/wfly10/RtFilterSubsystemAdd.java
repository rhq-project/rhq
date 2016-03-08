/*
 * RHQ Management Platform
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
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
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.rhq.helpers.rtfilter.subsystem.wfly10;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;

import org.rhq.helpers.rtfilter.subsystem.wfly10.deployment.RtFilterDeploymentUnitProcessor;

/**
 * @author Thomas Segismont
 */
class RtFilterSubsystemAdd extends AbstractAddStepHandler {

    static final RtFilterSubsystemAdd INSTANCE = new RtFilterSubsystemAdd();

    private RtFilterSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource)
        throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(RtFilterExtension.SUBSYSTEM_NAME,
                    RtFilterDeploymentUnitProcessor.PHASE, RtFilterDeploymentUnitProcessor.PRIORITY,
                    new RtFilterDeploymentUnitProcessor());

            }
        }, OperationContext.Stage.RUNTIME);
    }

}
