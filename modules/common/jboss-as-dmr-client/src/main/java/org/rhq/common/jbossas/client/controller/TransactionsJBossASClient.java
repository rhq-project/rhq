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
 * Provides management of the transactions subsystem.
 *
 * @author John Mazzitelli
 */
public class TransactionsJBossASClient extends JBossASClient {

    public static final String TRANSACTIONS = "transactions";

    public TransactionsJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Sets the default transaction timeout.
     * @param timeoutSecs the new default transaction timeout, in seconds.
     * @throws Exception
     */
    public void setDefaultTransactionTimeout(int timeoutSecs) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, TRANSACTIONS);
        final ModelNode req = createWriteAttributeRequest("default-timeout", String.valueOf(timeoutSecs), address);
        final ModelNode response = execute(req);

        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }

    /**
     * Sets the HA node identifier for transactions
     *
     * @param nodeId NodeId conforming to JTM specs
     * @throws Exception
     */
    public void setTransactionNodeId(String nodeId) throws Exception {
        final Address address = Address.root().add(SUBSYSTEM, TRANSACTIONS);
        final ModelNode req = createWriteAttributeRequest("node-identifier", nodeId, address);
        final ModelNode response = execute(req);

        if (!isSuccess(response)) {
            throw new FailureException(response);
        }
        return;
    }
}
