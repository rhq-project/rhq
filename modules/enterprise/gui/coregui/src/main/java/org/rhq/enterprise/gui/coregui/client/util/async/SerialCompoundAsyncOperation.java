/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.util.async;

/**
 * A compound {@link AsyncOperation async operation}, which executes a set of child async operations serially. Its
 * {@link #execute(AsyncOperationCallback, Object...)} method invokes its callback either when all child operations have
 * completed (i.e. invoked their own callbacks), or when a child operation fails and abortOnFailure is set to true.
 *
 * @author Ian Springer
 */
public class SerialCompoundAsyncOperation extends AbstractCompoundAsyncOperation {

    private boolean abortOnFailure;
    private int executionIndex= -1;

    public void setAbortOnFailure(boolean abortOnFailure) {
        this.abortOnFailure = abortOnFailure;
    }

    @Override
    public void execute(AsyncOperationCallback callback, Object... params) {
        super.execute(callback, params);
        
        if (getOperations().isEmpty()) {
            processCompletedOperation();
        } else {
            // Execute the first child operation. processCompletedOperation() will kick off the rest of the child
            // operations.
            executeNextOperation();
        }
    }

    @Override
    protected boolean isComplete() {
        return (super.isComplete() || (this.abortOnFailure && !getFailureThrowables().isEmpty()));
    }

    @Override
    protected boolean processCompletedOperation() {
        boolean isLastCompletedOperation = super.processCompletedOperation();
        if (!isLastCompletedOperation) {
            if (this.executionIndex < (getOperations().size() - 1)) {
                // Execute the next operation.
                executeNextOperation();
            }
        }
        return (isLastCompletedOperation);
    }

    private void executeNextOperation() {
        AsyncOperation operation = getOperations().get(++this.executionIndex);
        Object[] paramArray = getParameters(operation);
        operation.execute(this, paramArray);
    }

}
