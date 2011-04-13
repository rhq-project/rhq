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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ian Springer
 */
public abstract class AbstractCompoundAsyncOperation implements AsyncOperation,
        AsyncOperationCallback {

    private List<AsyncOperation> operations = new ArrayList<AsyncOperation>();
    private Map<AsyncOperation, Object[]> params = new LinkedHashMap<AsyncOperation, Object[]>();
    private AsyncOperationCallback<Map<AsyncOperation, Object>> callback;
    private Map<AsyncOperation, Object> successResults = new LinkedHashMap<AsyncOperation, Object>();
    private Map<AsyncOperation, Throwable> failureThrowables = new LinkedHashMap<AsyncOperation, Throwable>();

    public void addOperation(AsyncOperation operation) {
        if (this.callback != null) {
            throw new IllegalStateException("Child operations cannot be added once the compound operation has been executed.");
        }
        this.operations.add(operation);
    }

    @Override
    public void execute(AsyncOperationCallback callback, Object... params) {
        if (this.callback != null) {
            throw new IllegalStateException("This executor can only be executed once and has already been executed.");
        }
        this.callback = callback;

        List<AsyncOperation> operations1 = this.operations;
        for (int i = 0, operations1Size = operations1.size(); i < operations1Size; i++) {
            AsyncOperation operation = operations1.get(i);
            Object[] paramArray;
            if (i < this.params.size()) {
                Object param = params[i];
                paramArray = (param.getClass().isArray()) ? (Object[]) param : new Object[] { param };
            } else {
                // no params
                paramArray = new Object[0];
            }
            this.params.put(operation, paramArray);
        }
    }

    @Override
    public void onFailure(AsyncOperation operation, Throwable caught) {
        //noinspection ThrowableResultOfMethodCallIgnored
        this.failureThrowables.put(operation, caught);
        processCompletedOperation();
    }

    @Override
    public void onSuccess(AsyncOperation operation, Object result) {
        this.successResults.put(operation, result);
        processCompletedOperation();
    }

    public List<AsyncOperation> getOperations() {
        return operations;
    }

    public AsyncOperationCallback<Map<AsyncOperation, Object>> getCallback() {
        return callback;
    }

    public Map<AsyncOperation, Object> getSuccessResults() {
        return successResults;
    }

    public Map<AsyncOperation, Throwable> getFailureThrowables() {
        return failureThrowables;
    }

    public Object[] getParameters(AsyncOperation operation) {
        return this.params.get(operation);
    }

    protected boolean isComplete() {
        int completedOperationsCount = this.successResults.size() + this.failureThrowables.size();
        return (completedOperationsCount == this.operations.size());
    }

    protected boolean processCompletedOperation() {
        if (isComplete()) {
            if (this.failureThrowables.isEmpty()) {
                this.callback.onSuccess(this, this.successResults);
            } else {
                CompoundAsyncOperationException exception = new CompoundAsyncOperationException(this.successResults,
                        this.failureThrowables);
                this.callback.onFailure(this, exception);
            }
            return true;
        } else {
            return false;
        }
    }

}
