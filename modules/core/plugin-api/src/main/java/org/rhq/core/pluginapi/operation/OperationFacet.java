 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.pluginapi.operation;

import org.rhq.core.domain.configuration.Configuration;

/**
 * This facet must be implemented by resource components that want to expose one or more operations.
 */
public interface OperationFacet {
    /**
     * Invoke the operation with the specified name and returns a result set consisting of configuration propeties.
     *
     * <p>Plugin developers must write implementations of this method with the awareness that operations are <i>
     * cancelable</i> and should prepare for such a possiblity in the code. For example, a user may request that an
     * operation be canceled, or an operation may be taking to long and it should be canceled due to a timeout
     * expiration. When an operation invocation is canceled, the thread that is invoking this method will be
     * {@link Thread#interrupt() interrupted}. The plugin writer has a choice to make - either write this method
     * implementation such that it ignores cancelation requests (in effect continue doing what it is doing and return
     * normally) or handle the cancelation request and stop doing what it is doing and throw an
     * {@link InterruptedException} to indicate the operation was aborted. Note that if an implementation does not throw
     * an {@link InterruptedException}, the plugin container will assume the operation did not cancel itself. If, even
     * after an interrupt, the implementation returns normally, the plugin container will assume the operation was
     * successful and will be marked as such.</p>
     *
     * <p>Plugin writers are encouraged to check the current thread's {@link Thread#isInterrupted()} method periodically
     * to catch cancellation requests, as well as catching and processing {@link InterruptedException} appropriately.
     * Plugin writers must also ensure that they do not leave the managed resource in an inconsistent state after being
     * canceled or timed out.</p>
     *
     * <p>If a plugin determines that the operation invocation failed for whatever reason, this method must throw an
     * <code>Exception</code>, which indicates a failure. If this method returns normally with any
     * {@link OperationResult} (even if it is <code>null</code>), the operation invocation will assumed to have been a
     * success.</p>
     *
     * @param  name       the name of the operation
     * @param  parameters the parameters passed to the operation; even for operations with no parameters, an empty
     *                    config will be passed in by the plugin container
     *
     * @return the result of the invocation
     *
     * @throws InterruptedException if this operation was canceled
     * @throws Exception            if failed to invoke the operation on the resource
     */
    OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception;
}