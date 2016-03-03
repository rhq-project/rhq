/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.modules.plugins.wildfly10.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.modules.plugins.wildfly10.ASConnection;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * helper class to set deployment metadata on AS7 server (an uploaded content on server is required before using this class)
 * @author lzoubek@redhat.com
 *
 */
public class Deployer {

    private static final Log LOG = LogFactory.getLog(Deployer.class);

    private final String deploymentName;
    private final String runtimeName;
    private final String hash;
    private final ASConnection connection;
    private final List<Operation> beforeDeploySteps = new ArrayList<Operation>();
    private final List<Operation> afterDeploySteps = new ArrayList<Operation>();
    private int opTimeout = 300;
    private String newResourceKey;

    public Deployer(String deploymentName, String runtimeName, String hash, ASConnection connection) {
        this.deploymentName = deploymentName;
        this.runtimeName = runtimeName;
        this.hash = hash;
        this.connection = connection;
    }

    /**
     * set deployment operation timeout (default is 300s)
     * @param timeout new timeout
     * @return this
     */
    public Deployer operationTimeout(int timeout) {
        this.opTimeout = timeout;
        return this;
    }

    public Deployer addBeforeDeployStep(Operation step) {
        beforeDeploySteps.add(step);
        return this;
    }

    public Deployer addAfterDeployStep(Operation step) {
        afterDeploySteps.add(step);
        return this;
    }

    /**
     * gets resource key of new deployment resource (first {@link #deployToServer()} or {@link #deployToServerGroup(String)} must be called)
     * @return
     */
    public String getNewResourceKey() {
        return newResourceKey;
    }

    /**
     * does the deployment
     * @param doDeploy set to true if deployment should be enabled immediately - this must be set to false in case of DomainDeployment
     * @return deployment operation result
     */
    public Result deployToServer(boolean doDeploy) {
        CompositeOperation cop = new CompositeOperation();
        for (Operation step : beforeDeploySteps) {
            cop.addStep(step);
        }
        Operation step1 = addDeployment();
        cop.addStep(step1);
        // if standalone, then :deploy the deployment anyway
        if (doDeploy) {
            Operation step2 = new Operation("deploy", step1.getAddress());
            cop.addStep(step2);
        }
        for (Operation step : afterDeploySteps) {
            cop.addStep(step);
        }
        // otherwise only add the deployment
        newResourceKey = step1.getAddress().getPath();

        if (ASConnection.verbose) {
            LOG.info("Deploy operation: " + cop);
        }
        return connection.execute(cop, opTimeout);
    }

    /**
     * deploys to server group with given resourceKey
     * @param serverGroupResourceKey resourceKey of group
     * @return deployment operation result
     */
    public Result deployToServerGroup(String serverGroupResourceKey) {
        return deployToServerGroup(new Address(serverGroupResourceKey));
    }

    /**
     * deploys to server group with given address
     * @param serverGroupAddress serverGroup address
     * @return deployment operation result
     */
    public Result deployToServerGroup(Address serverGroupAddress) {
        CompositeOperation cop = new CompositeOperation();
        cop.addStep(addDeployment());
        serverGroupAddress.add("deployment", deploymentName);
        cop.addStep(new Operation("add", serverGroupAddress));
        cop.addStep(new Operation("deploy", serverGroupAddress));
        newResourceKey = serverGroupAddress.getPath();
        if (ASConnection.verbose) {
            LOG.info("Deploy operation: " + cop);
        }
        return connection.execute(cop, opTimeout);
    }

    /**
     * create an operation step which adds deployment resource on AS7 server
     * @return new step
     */
    private Operation addDeployment() {
        Operation step1 = new Operation("add", "deployment", deploymentName);

        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        content.add(contentValues);
        step1.addAdditionalProperty("content", content);

        step1.addAdditionalProperty("name", deploymentName);
        step1.addAdditionalProperty("runtime-name", runtimeName);
        return step1;
    }
}
