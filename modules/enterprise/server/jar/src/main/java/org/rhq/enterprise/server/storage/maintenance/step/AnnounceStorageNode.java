package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class AnnounceStorageNode extends ResourceOperationStepRunner {

    private static final Log log = LogFactory.getLog(AnnounceStorageNode.class);

    public AnnounceStorageNode() {
        super("announce");
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
