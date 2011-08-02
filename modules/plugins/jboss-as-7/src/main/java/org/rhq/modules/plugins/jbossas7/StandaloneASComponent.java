/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for standalone AS7 servers
 * @author Heiko W. Rupp
 */
public class StandaloneASComponent extends BaseServerComponent implements OperationFacet {

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws Exception {

        if (name.equals("start")) {
            return startServer(AS7Mode.STANDALONE);
        } else if (name.equals("restart")) {
            return restartServer(parameters, AS7Mode.STANDALONE);

        }

        // reload, shutdown go to the remote server
        Operation op = new Operation(name,new Address());
        Result res = getASConnection().execute(op);

        OperationResult operationResult = postProcessResult(name, res);
        return operationResult;
    }

}
