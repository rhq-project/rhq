package org.rhq.enterprise.startup;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;

public class StartupCrippledDeploymentProcessor implements DeploymentUnitProcessor {

    public static final Phase PHASE = Phase.INSTALL;
    public static final int PRIORITY = 0x1;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // get the main parent deployment unit of the thing being deployed
        DeploymentUnit du = phaseContext.getDeploymentUnit();
        DeploymentUnit parent = du.getParent();
        while (parent != null) {
            du = parent;
            parent = parent.getParent();
        }

        // make sure the main deployment unit is the one we want
        String name = du.getName();
        if (!"rhq.ear".equals(name)) {
            throw new DeploymentUnitProcessingException("Cannot deploy custom applications in this app server");
        }

        return; // let the real deployers now get a chance to really deploy it.
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        return;
    }
}
