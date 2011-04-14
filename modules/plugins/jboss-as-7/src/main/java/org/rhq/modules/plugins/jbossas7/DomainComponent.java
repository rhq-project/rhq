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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
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
    public AvailabilityType getAvailability() {

        if (context.getResourceType().getName().equals("JBossAS-Managed")) {
            List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>(2);
            String host = conf.getSimpleValue("domainHost","local");
            address.add(new PROPERTY_VALUE("host",host));
            address.add(new PROPERTY_VALUE("server-config",myServerName));
            Operation getStatus = new Operation("read-attribute",address,"name","status");
            JsonNode result = null;
            try {
                result = connection.executeRaw(getStatus);
            } catch (Exception e) {
                log.warn(e.getMessage());
                return AvailabilityType.DOWN;
            }
            if (ASConnection.isErrorReply(result))
                return AvailabilityType.DOWN;

            String msg = ASConnection.getSuccessDescription(result);
            if (msg.contains("STARTED"))
                return AvailabilityType.UP;
            else
                return AvailabilityType.DOWN;
        }

        return super.getAvailability();    // TODO: Customise this generated block
    }

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

        List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>();

        if (what.equals("server-group")) {
            String groupName = parameters.getSimpleValue("name",null);
            String profile = parameters.getSimpleValue("profile","default");

            address.add(new PROPERTY_VALUE("server-group",groupName));

            operation = new Operation(op,address,"profile",profile);
        } else if (what.equals("server")) {

            if (context.getResourceType().getName().equals("JBossAS-Managed")) {
                String host = conf.getSimpleValue("domainHost","local");
                address.add(new PROPERTY_VALUE("host",host));
                address.add(new PROPERTY_VALUE("server-config",myServerName));
                operation = new Operation(op,address);
            }
            else if (context.getResourceType().getName().equals("Host")) {
                address.addAll(pathToAddress(getPath()));
                String serverName = parameters.getSimpleValue("name",null);
                address.add(new PROPERTY_VALUE("server-config",serverName));
                Map<String,Object> props = new HashMap<String, Object>();
                String serverGroup = parameters.getSimpleValue("group",null);
                props.put("group",serverGroup);
                if (op.equals("add")) {
                    props.put("name",serverName);
                    boolean autoStart = parameters.getSimple("auto-start").getBooleanValue();
                    props.put("auto-start",autoStart);
                    // TODO put more properties in
                }

                operation = new Operation(op,address,props);
            }
        } else if (what.equals("destination")) {
            address.addAll(pathToAddress(getPath()));
            String newName = parameters.getSimpleValue("name","");
//            String type = parameters.getSimpleValue("type","Queue").toLowerCase();
//            address.add(new PROPERTY_VALUE(type,newName));
            String queueName = parameters.getSimpleValue("queue-address","");
            Map<String,Object> props = new HashMap<String, Object>();
            props.put("queue-address",queueName);
            operation = new Operation(op,address);
        }

        OperationResult operationResult = new OperationResult();
        if (operation!=null) {
            JsonNode result = connection.executeRaw(operation);

            if (ASConnection.isErrorReply(result)) {
                operationResult.setErrorMessage(ASConnection.getFailureDescription(result));
            }
            else {
                operationResult.setSimpleResult(ASConnection.getSuccessDescription(result));
            }
        }
        else {
            operationResult.setErrorMessage("No valid operation was given");
        }
        // TODO throw an exception if the operation failed?
        return operationResult;
    }
}
