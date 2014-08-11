package org.rhq.enterprise.server.storage.maintenance.step;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class BootstrapNode extends BaseStepRunner {

    private static final Log log = LogFactory.getLog(BootstrapNode.class);

    @Override
    public void execute(MaintenanceStep step) {
        log.info("Bootstrapping new node");

        Configuration configuration = step.getConfiguration();
        String targetAddress = configuration.getSimpleValue("targetAddress");
        PropertyMap params = (PropertyMap) configuration.get("parameters");

        Configuration operationParams = new Configuration();
        operationParams.put(params.get("cqlPort"));
        operationParams.put(params.get("gossipPort"));
        operationParams.put(params.get("addresses"));

        executeOperation(targetAddress, "prepareForBootstrap", operationParams);
    }
}
