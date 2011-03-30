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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;

/**
 * Common stuff for the Domain
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class DomainComponent extends BaseComponent implements OperationFacet{

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        if (!name.contains(":")) {
            OperationResult badName = new OperationResult("Operation name did not contain a ':'");
            badName.setErrorMessage("Operation name did not contain a ':'");
            return badName;
        }

        int colonPos = name.indexOf(':');
        String what = name.substring(0, colonPos);
        String op = name.substring(colonPos+1);
        Operation operation=null;

        if (what.equals("server-group")) {
            String groupName = parameters.getSimpleValue("name",null);
            String profile = parameters.getSimpleValue("profile","default");

            List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>(1);
            address.add(new PROPERTY_VALUE("server-group",groupName));

            operation = new Operation(op,address,"profile",profile);
        } else if (what.equals("server")) {
            List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>();

            String host = conf.getSimpleValue("domainHost","local");

            address.add(new PROPERTY_VALUE("host",host));
            address.add(new PROPERTY_VALUE("server-config",myServerName));

            operation = new Operation(op,address);
        }

        OperationResult operationResult = new OperationResult();
        if (operation!=null) {
            JsonNode result = connection.execute(operation);

            if (connection.isErrorReply(result)) {
                operationResult.setErrorMessage(connection.getFailureDescription(result));
            }
            else {
                operationResult.setSimpleResult(connection.getSuccessDescription(result));
            }
        }
        else {
            operationResult.setErrorMessage("No valid operation was given");
        }
        // TODO throw an exception if the operation failed?
        return operationResult;
    }
}
