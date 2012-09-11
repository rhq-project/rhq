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

import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convenience methods associated with JMS management.
 * 
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class JMSJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_MESSAGING = "messaging";
    public static final String HORNETQ_SERVER = "hornetq-server";
    public static final String JMS_QUEUE = "jms-queue";
    public static final String OP_ENABLE = "enable";

    public JMSJBossASClient(ModelControllerClient client) {
        super(client);
    }

    public boolean isQueueEnabled(String queueName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_MESSAGING, HORNETQ_SERVER, "default", JMS_QUEUE,
            queueName);
        ModelNode results = readResource(addr);
        boolean enabledFlag = false;
        if (results.hasDefined("enabled")) {
            ModelNode enabled = results.get("enabled");
            enabledFlag = enabled.asBoolean(false);
        }
        return enabledFlag;
    }

    public void enableQueue(String queueName) throws Exception {
        if (isQueueEnabled(queueName)) {
            return; // nothing to do - it's already enabled
        }

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_MESSAGING, HORNETQ_SERVER, "default", JMS_QUEUE,
            queueName);
        ModelNode request = createRequest(OP_ENABLE, addr);
        request.get(PERSISTENT).set(true);
        ModelNode results = execute(request);
        if (!isSuccess(results)) {
            throw new FailureException(results);
        }
        return; // everything is OK
    }

    /**
     * Checks to see if there is already a queue with the given name.
     *
     * @param queueName the name to check
     * @return true if there is a queue with the given name already in existence
     */
    public boolean isQueue(String queueName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_MESSAGING, HORNETQ_SERVER, "default");
        String haystack = JMS_QUEUE;
        return null != findNodeInList(addr, haystack, queueName);
    }

    /**
     * Returns a ModelNode that can be used to create a queue.
     * Callers are free to tweak the queue request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * @param name the queue name
     * @param entryNames the jndiNames, each is prefixed with 'java:/'.  Only supports one entry currently.
     *
     * @return the request that can be used to create the queue
     */
    public ModelNode createNewQueueRequest(String name, List<String> entryNames) {

        String dmrTemplate = "" //
            + "{" //
            + "\"entries\" => [\"%s\"] " //
            + "}";

        String dmr = String.format(dmrTemplate, entryNames.get(0));

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_MESSAGING, HORNETQ_SERVER, "default", JMS_QUEUE, name);
        final ModelNode request = ModelNode.fromString(dmr);
        request.get(OPERATION).set(ADD);
        request.get(ADDRESS).set(addr.getAddressNode());

        return request;
    }

}
