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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.taskdefs.Sleep;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;

/**
 * Component class that deals with the Logging subsystem
 * @author Heiko W. Rupp
 */
public class LoggerComponent extends BaseComponent {

    private final Log log = LogFactory.getLog(LoggerComponent.class);

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        Operation op = new Operation(name,pathToAddress(getPath()));

        Map<String,Property> propertyMap = parameters.getAllProperties();
        for (Map.Entry<String,Property> entry : propertyMap.entrySet()) {
            PropertySimple ps = (PropertySimple) entry.getValue();
            op.addAdditionalProperty(entry.getKey(),ps.getStringValue());
        }


        ASConnection conn = getASConnection();
        ComplexResult result = conn.executeComplex(op);

        if (result.isSuccess()) {
            return new OperationResult("ok");
        }
        else {
            OperationResult failure = new OperationResult();
            failure.setErrorMessage(result.getFailureDescription().toString());
            return failure;
        }


    }
}
