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

import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.wildfly10.helper.JdrReportRunner;
import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Component for the JDR subsystem
 * @author Stefan Negrea
 */
public class JDRComponent extends BaseComponent<ResourceComponent<?>> implements OperationFacet {

    @SuppressWarnings("unchecked")
    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException,
        Exception {
        OperationResult operationResult = new OperationResult();
        Address address = new Address(path);
        Operation operation = new Operation(name, address);
        Result asResult = getASConnection().execute(operation, false, JdrReportRunner.JDR_OPERATION_TIMEOUT);

        if (!asResult.isSuccess()) {
            operationResult.setErrorMessage(asResult.getFailureDescription());
            return operationResult;
        }

        Configuration operationComplexResult = operationResult.getComplexResults();
        Map<String, Object> asResultContext = (Map<String, Object>) asResult.getResult();

        for (Map.Entry<String, Object> resultEntry : asResultContext.entrySet()) {
            PropertySimple property = new PropertySimple(resultEntry.getKey(), resultEntry.getValue().toString());
            operationComplexResult.put(property);
        }

        return operationResult;
    }
}
