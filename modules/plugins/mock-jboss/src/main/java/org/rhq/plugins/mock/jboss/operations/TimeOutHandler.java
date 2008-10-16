/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.operations;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.mock.jboss.scenario.OperationTimedOut;

/**
 * Handles processing for control operations that are configured to time out.
 *
 * @author Jason Dobies
 */
public class TimeOutHandler implements OperationHandler {
    // Attributes  --------------------------------------------

    private int timeToWait;

    // Constructors  --------------------------------------------

    public TimeOutHandler(OperationTimedOut resultPolicy) {
        this.timeToWait = resultPolicy.getTimeToWait();
    }

    // OperationHandler Implementation  --------------------------------------------

    public OperationResult handleOperation(Configuration configuration) throws Exception {
        try {
            Thread.sleep(timeToWait);
        } catch (InterruptedException e) {
            throw e;
        }

        // Unlikley this will be hit if the above is configured correctly in the scenario, the PC should time this out.
        return null;
    }
}
