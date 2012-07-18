/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * A client that can be used to talk to a JBossAS server via the DMR/ModelControllerClient API.
 *
 * @author John Mazzitelli
 */
public class JBossASClient {

    private static final String BATCH = "composite";
    private static final String BATCH_STEPS = "steps";
    private static final String OPERATION = "operation";
    private static final String ADDRESS = "address";
    private static final String RESULT = "result";
    private static final String OUTCOME = "outcome";
    private static final String OUTCOME_SUCCESS = "success";
    private static final String FAILURE_DESCRIPTION = "failure-description";
    private static final String NAME = "name";
    private static final String READ_ATTRIBUTE = "read-attribute";

    private ModelControllerClient client;
    
    public JBossASClient(ModelControllerClient client) {
        this.client = client;
    }

    public ModelControllerClient getModelControllerClient() {
        return client;
    }

    /**
     * Convienence method that builds a partial operation request node.
     * 
     * @param operation the operation to be requested
     * @param address identifies the target resource
     * @return the partial operation request node - caller should fill this in further to complete the node
     */
    public ModelNode createRequest(String operation, Address address) {
        final ModelNode request = new ModelNode();
        request.get(OPERATION).set(operation);
        request.get(ADDRESS).set(address.getAddressNode());
        return request;
    }

    /**
     * Creates a batch of operations that can be atomically invoked.
     * 
     * @param steps the different operation steps of the batch
     * 
     * @return the batch operation node
     */
    public ModelNode createBatchRequest(ModelNode... steps) {
        final ModelNode composite = new ModelNode();
        composite.get(OPERATION).set(BATCH);
        composite.get(ADDRESS).setEmptyList();
        final ModelNode stepsNode = composite.get(BATCH_STEPS);
        for (ModelNode step : steps) {
            stepsNode.add(step);
        }
        return composite;
    }

    /**
     * If the given node has a result list, that list will be returned
     * with the values as Strings. Otherwise, an empty list is returned.
     * 
     * @param operationResult the node to examine
     * @return the result list as Strings if there is a list, empty otherwise
     */
    public List<String> getResultList(ModelNode operationResult) {
        if (!operationResult.hasDefined(RESULT)) {
            return Collections.emptyList();
        }

        List<ModelNode> nodeList = operationResult.get(RESULT).asList();
        if (nodeList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<String>(nodeList.size());
        for (ModelNode node : nodeList) {
            list.add(node.asString());
        }

        return list;
    }

    /**
     * Examines the given node's result list and if the item is found, returns true.
     * 
     * @param operationResult the node to examine
     * @param item the item to look for in the node's result list
     * @return true if the node has a result list and it contains the item; false otherwise
     */
    public boolean listContains(ModelNode operationResult, String item) {
        if (!operationResult.hasDefined(RESULT)) {
            return false;
        }

        List<ModelNode> nodeList = operationResult.get(RESULT).asList();
        if (nodeList.isEmpty()) {
            return false;
        }

        for (ModelNode node : nodeList) {
            if (node.asString().equals(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if the operation was a success; <code>false</code> otherwise.
     * 
     * @param operationResult the operation result to test
     * @return the success or failure flag of the result
     */
    public boolean isSuccess(ModelNode operationResult) {
        if (operationResult != null) {
            return operationResult.hasDefined(OUTCOME)
                && operationResult.get(OUTCOME).asString().equals(OUTCOME_SUCCESS);
        }
        return false;
    }

    /**
     * If the operation result was a failure, this returns the failure description if there is one.
     * A generic failure message will be returned if the operation was a failure but has no failure
     * description. A <code>null</code> is returned if the operation was a success.
     * 
     * @param operationResult the operation whose failure description is to be returned
     * @return the failure description of <code>null</code> if the operation was a success
     */
    public String getFailureDescription(ModelNode operationResult) {
        if (isSuccess(operationResult)) {
            return null;
        }
        if (operationResult != null) {
            ModelNode descr = operationResult.get(FAILURE_DESCRIPTION);
            if (descr != null) {
                return descr.asString();
            }
        }
        return "Unknown failure";
    }

    /**
     * Convienence method that allows you to obtain a single attribute's string value from
     * a resource.
     * 
     * @param attributeName the attribute whose value is to be returned
     * @param address identifies the resource
     * @return the attribute value
     * 
     * @throws Exception if failed to obtain the attribute value
     */
    public String getStringAttribute(String attributeName, Address address) throws Exception {
        ModelNode op = createRequest(READ_ATTRIBUTE, address);
        op.get(NAME).set(attributeName);
        ModelNode results = getModelControllerClient().execute(op);
        if (isSuccess(results)) {
            ModelNode version = results.get(RESULT);
            String attributeValue = version.asString();
            return attributeValue;
        } else {
            throw new RuntimeException(getFailureDescription(results));
        }
    }
}
