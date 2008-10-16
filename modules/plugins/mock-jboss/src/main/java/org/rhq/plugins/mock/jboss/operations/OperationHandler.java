/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.jboss.on.plugins.mock.jboss.operations;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Base interface for all control operation handlers. Each implmenting class is responsible for emulating a different
 * result to the execution of a control operation, such as throwing an exception or timing out. There will be one
 * instance of this class for each resource/operation name pairing and will be used to generate the response for each
 * operation call on that resource.
 *
 * @author Jason Dobies
 */
public interface OperationHandler
{
   /**
    * Takes appropriate action when an operation on the resource is requested.
    *
    * @param configuration parameters to the operation.
    * @return response message as appropriate for the specified implementaiton.
    * @throws Exception if the particular implementation emulates an exception case.
    * @throws InterruptedException if the operation was canceled or was timed out by external forces
    */
   OperationResult handleOperation(Configuration configuration)
      throws Exception, InterruptedException;

}
