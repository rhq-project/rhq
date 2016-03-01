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

package org.rhq.modules.plugins.wildfly10;

import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Component class for Threading within the runtime
 * @author Heiko W. Rupp
 */
public class ThreadingComponent extends BaseComponent<ThreadingComponent> {

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws  Exception {

        if (name.equals("get-all-thread-ids")) {
            Operation op = new ReadAttribute(getAddress(), "all-thread-ids");
            Result res = getASConnection().execute(op);
            OperationResult operationResult;
            if (res.isSuccess()) {
                List<Long> ids = (List<Long>) res.getResult();
                operationResult = new OperationResult(ids.toString());
            } else {
                operationResult = new OperationResult();
                String errorMessage = "Got no result back";
                if (!res.isSuccess()) {
                    errorMessage += ": " + res.getFailureDescription();
                }
                operationResult.setErrorMessage(errorMessage);
            }
            return operationResult;
        }

        return super.invokeOperation(name, parameters);
    }
}
