package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class AnnounceStorageNode extends BaseStepRunner implements MaintenanceStepRunner {

    private static final Log log = LogFactory.getLog(AnnounceStorageNode.class);

    @Override
    public void execute(MaintenanceStep step) {
//        Map<String, String> argsMap = convertArgs(step.getArgs());
//        Configuration args = new Configuration();
//        args.put(new PropertySimple("address", argsMap.get("address")));
//
//        log.info("Announcing new node " + argsMap.get("address") + " to " + step.getStorageNode().getAddress());
//
//        executeOperation(step.getStorageNode(), "announce", args);

        log.info("Announcing new node");
    }

}
