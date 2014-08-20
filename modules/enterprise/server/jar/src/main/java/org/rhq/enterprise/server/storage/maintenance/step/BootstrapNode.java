package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;

/**
 * @author John Sanda
 */
public class BootstrapNode extends ResourceOperationStepRunner {

    private static final Log log = LogFactory.getLog(BootstrapNode.class);

    public BootstrapNode() {
        super("prepareForBootstrap");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
