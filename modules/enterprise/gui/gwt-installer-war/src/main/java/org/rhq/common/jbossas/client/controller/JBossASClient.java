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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;

/**
 * A client that can be used to talk to a JBossAS server via the DMR/ModelControllerClient API.
 *
 * @author John Mazzitelli
 */
public class JBossASClient {

    // protected to allow subclasses to have a logger, too, without explicitly declaring one themselves
    protected final Log log = LogFactory.getLog(this.getClass());

    public static final String BATCH = "composite";
    public static final String BATCH_STEPS = "steps";
    public static final String OPERATION = "operation";
    public static final String ADDRESS = "address";
    public static final String RESULT = "result";
    public static final String OUTCOME = "outcome";
    public static final String OUTCOME_SUCCESS = "success";
    public static final String SUBSYSTEM = "subsystem";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String READ_ATTRIBUTE = "read-attribute";
    public static final String READ_RESOURCE = "read-resource";
    public static final String WRITE_ATTRIBUTE = "write-attribute";
    public static final String ADD = "add";

    private ModelControllerClient client;

    public JBossASClient(ModelControllerClient client) {
        this.client = client;
    }

    /////////////////////////////////////////////////////////////////
    // Some static methods useful for convienence

    /**
     * Convienence method that allows you to create request that reads a single attribute
     * value to a resource.
     * 
     * @param attributeName the name of the attribute whose value is to be read
     * @param address identifies the resource
     * @return the request
     */
    public static ModelNode createReadAttributeRequest(String attributeName, Address address) {
        ModelNode op = createRequest(READ_ATTRIBUTE, address);
        op.get(NAME).set(attributeName);
        return op;
    }

    /**
     * Convienence method that allows you to create request that writes a single attribute's
     * string value to a resource.
     * 
     * @param attributeName the name of the attribute whose value is to be written
     * @param attributeValue the attribute value that is to be written
     * @param address identifies the resource
     * @return the request
     */
    public static ModelNode createWriteAttributeRequest(String attributeName, String attributeValue, Address address) {
        ModelNode op = createRequest(WRITE_ATTRIBUTE, address);
        op.get(NAME).set(attributeName);
        op.get(VALUE).set(attributeValue);
        return op;
    }

    /**
     * Convienence method that builds a partial operation request node.
     * 
     * @param operation the operation to be requested
     * @param address identifies the target resource
     * @return the partial operation request node - caller should fill this in further to complete the node
     */
    public static ModelNode createRequest(String operation, Address address) {
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
    public static ModelNode createBatchRequest(ModelNode... steps) {
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
    public static List<String> getResultListAsStrings(ModelNode operationResult) {
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
     * If the given node has results, those results are returned in a ModelNode.
     * Otherwise, an empty node is returned.
     * 
     * @param operationResult the node to examine
     * @return the results as a ModelNode
     */
    public static ModelNode getResults(ModelNode operationResult) {
        if (!operationResult.hasDefined(RESULT)) {
            return new ModelNode();
        }

        return operationResult.get(RESULT);
    }

    /**
     * Examines the given node's result list and if the item is found, returns true.
     * 
     * @param operationResult the node to examine
     * @param item the item to look for in the node's result list
     * @return true if the node has a result list and it contains the item; false otherwise
     */
    public static boolean listContains(ModelNode operationResult, String item) {
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
    public static boolean isSuccess(ModelNode operationResult) {
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
    public static String getFailureDescription(ModelNode operationResult) {
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

    /////////////////////////////////////////////////////////////////
    // Non-static methods that need the client

    public ModelControllerClient getModelControllerClient() {
        return client;
    }

    /**
     * Convienence method that executes the request.
     * 
     * @param request
     * @return results
     * @throws Exception
     */
    public ModelNode execute(ModelNode request) throws Exception {
        try {
            return getModelControllerClient().execute(request, OperationMessageHandler.logging);
        } catch (Exception e) {
            log.error("Failed to execute request", e);
            throw e;
        }
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
        ModelNode op = createReadAttributeRequest(attributeName, address);
        ModelNode results = execute(op);
        if (isSuccess(results)) {
            ModelNode version = getResults(results);
            String attributeValue = version.asString();
            return attributeValue;
        } else {
            throw new FailureException(results, "Failed to get attribute [" + attributeName + "] from [" + address
                + "]");
        }
    }
}
