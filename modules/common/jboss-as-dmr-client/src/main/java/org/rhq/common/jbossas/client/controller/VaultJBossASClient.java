/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * Provides convenience methods associated with Vault management.
 *
 * @author Stefan Negrea
 */
public class VaultJBossASClient extends JBossASClient {

    public static final String CORE_SERVICE = "core-service";
    public static final String VAULT = "vault";

    public VaultJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if there is already a vault with the given name.
     *
     * @return true if the vault is already configured
     */
    public boolean isVault() throws Exception {
        Address addr = Address.root().add(CORE_SERVICE, VAULT);
        final ModelNode queryNode = createRequest(READ_RESOURCE, addr);
        final ModelNode results = execute(queryNode);
        if (isSuccess(results)) {
            return true;
        }

        return false;
    }

    /**
     * Returns a ModelNode that can be used to create the vault.
     * Callers are free to tweak the queue request that is returned,
     * if they so choose, before asking the client to execute the request.
     *
     * @param className class name for the custom vault
     *
     * @return the request that can be used to create the vault
     */
    public ModelNode createNewVaultRequest(String className) {
        String dmrTemplate = "" //
            + "{" //
            + "\"code\" => \"%s\""
            + "}";

        String dmr = String.format(dmrTemplate, className);

        Address addr = Address.root().add(CORE_SERVICE, VAULT);
        final ModelNode request = ModelNode.fromString(dmr);
        request.get(OPERATION).set(ADD);
        request.get(ADDRESS).set(addr.getAddressNode());

        return request;
    }
}
