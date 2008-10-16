/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.operations;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.mock.jboss.scenario.OperationFailure;

/**
 * Handles the processing of a control operation that is configured to throw an exception.
 *
 * @author Jason Dobies
 */
public class FailureHandler implements OperationHandler {
    // Attributes  --------------------------------------------

    private String message;

    // Constructors  --------------------------------------------

    public FailureHandler(OperationFailure resultPolicy) {
        this.message = resultPolicy.getMessage();
    }

    // OperationHandler Implementation  --------------------------------------------

    public OperationResult handleOperation(Configuration configuration) throws Exception {
        throw new Exception(message);
    }
}
