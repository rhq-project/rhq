/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.plugins.mock.jboss.operations;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.mock.jboss.scenario.OperationSuccess;

/**
 * Handles processing for control operations that are configured to emulate a successful execution.
 *
 * @author Jason Dobies
 */
public class SuccessHandler implements OperationHandler {
    // Attributes  --------------------------------------------

    private String message;
    private boolean echoParameters;

    // Constructors  --------------------------------------------

    public SuccessHandler(OperationSuccess resultPolicy) {
        this.message = resultPolicy.getMessage();
        this.echoParameters = resultPolicy.isEchoParameters();
    }

    // OperationHandler Implementation  --------------------------------------------

    public OperationResult handleOperation(Configuration configuration) {
        StringBuffer sb = new StringBuffer();
        sb.append(message);

        if (echoParameters) {
            sb.append("Parameters: ");
            for (Property p : configuration.getProperties()) {
                if (p instanceof PropertySimple) {
                    PropertySimple simple = (PropertySimple) p;
                    sb.append(p.getName()).append(" -> ").append(simple.getStringValue()).append("  ");
                }
            }
        }

        OperationResult results = new OperationResult();
        results.getComplexResults().put(new PropertySimple("results", sb.toString()));
        return results;
    }
}
