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
        // If we are a single node cluster we have to abort since the new node will not be
        // able to gossip with the existing node. But if we are running multiple nodes, we
        // can since the new node will be able to bootstrap from them.

        if (clusterSnapshot.size() == 1) {
            return StepFailureStrategy.ABORT;
        }
        return StepFailureStrategy.CONTINUE;
    }
}
