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

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convenience methods associated with deployments.
 * 
 * @author John Mazzitelli
 */
public class DeploymentJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_DEPLOYMENT = "deployment";
    public static final String ENABLED = "enabled";

    public DeploymentJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if there is already a deployment with the given name.
     *
     * @param name the deployment name to check
     * @return true if there is a deployment with the given name already in existence
     */
    public boolean isDeployment(String name) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM_DEPLOYMENT, name);
        return null != readResource(addr);
    }

    public boolean isDeploymentEnabled(String name) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM_DEPLOYMENT, name);
        ModelNode results = readResource(addr);
        if (results == null) {
            throw new IllegalArgumentException("There is no deployment with the name: " + name);
        }
        boolean enabledFlag = false;
        if (results.hasDefined(ENABLED)) {
            ModelNode enabled = results.get(ENABLED);
            enabledFlag = enabled.asBoolean(false);
        }
        return enabledFlag;
    }

    public void enableDeployment(String name) throws Exception {
        enableDisableDeployment(name, true);
    }

    public void disableDeployment(String name) throws Exception {
        enableDisableDeployment(name, false);
    }

    private void enableDisableDeployment(String name, boolean enable) throws Exception {
        if (isDeploymentEnabled(name) == enable) {
            return; // nothing to do - its already in the state we want
        }

        Address addr = Address.root().add(SUBSYSTEM_DEPLOYMENT, name);
        ModelNode request = createWriteAttributeRequest(ENABLED, Boolean.toString(enable), addr);
        ModelNode results = execute(request);
        if (!isSuccess(results)) {
            throw new FailureException(results);
        }
        return; // everything is OK
    }
}
