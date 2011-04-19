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

}
