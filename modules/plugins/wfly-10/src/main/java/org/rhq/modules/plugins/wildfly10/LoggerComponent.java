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
package org.rhq.modules.plugins.wildfly10;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.wildfly10.json.ComplexResult;
import org.rhq.modules.plugins.wildfly10.json.Operation;

/**
 * Component class that deals with the Logging subsystem
 * @author Heiko W. Rupp
 */
public class LoggerComponent extends BaseComponent {

    private final Log log = LogFactory.getLog(LoggerComponent.class);

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        Operation op = new Operation(name,address);

        Map<String,Property> propertyMap = parameters.getAllProperties();
        for (Map.Entry<String,Property> entry : propertyMap.entrySet()) {
            if (entry.getValue() instanceof PropertySimple) {
                PropertySimple ps = (PropertySimple) entry.getValue();
                op.addAdditionalProperty(entry.getKey(),ps.getStringValue());
            } else if (entry.getValue() instanceof PropertyList) {
                PropertyList pl = (PropertyList) entry.getValue();
                List<Property> props = pl.getList();
                List<String> objects = new ArrayList<String>(props.size());
                for (Property p : props) {
                    PropertySimple ps = (PropertySimple) p;
                    objects.add(ps.getStringValue());
                }
                op.addAdditionalProperty(entry.getKey(),objects);
            }
        }


        ASConnection conn = getASConnection();
        ComplexResult result = conn.executeComplex(op);

        if (result.isSuccess()) {
            return new OperationResult("ok");
        }
        else {
            OperationResult failure = new OperationResult();
            failure.setErrorMessage(result.getFailureDescription());
            return failure;
        }


    }
}
