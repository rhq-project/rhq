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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Component class for host- and domain controller
 * @author Heiko W. Rupp
 */
public class HostControllerComponent extends BaseServerComponent implements OperationFacet {

    private final Log log = LogFactory.getLog(HostControllerComponent.class);

    @Override
    public OperationResult invokeOperation(String name,
                                           Configuration parameters) throws InterruptedException, Exception {

        if (name.equals("start")) {
            return startServer(AS7Mode.DOMAIN);
        } else if (name.equals("restart")) {
            return restartServer(parameters, AS7Mode.DOMAIN);

        } else if (name.equals("shutdown")) {
            // This is a bit trickier, as it needs to be executed on the level on /host=xx
            String domainHost = pluginConfiguration.getSimpleValue("domainHost","");
            if (domainHost.isEmpty()) {
                OperationResult result = new OperationResult();
                result.setErrorMessage("No domain host found - can not continue");
                return result;
            }
            Operation op = new Operation("shutdown","host",domainHost);
            Result res = getASConnection().execute(op);

            postProcessResult(name,res);
        }

        // Defer other stuff to the base component for now
        return super.invokeOperation(name, parameters);
    }
}
