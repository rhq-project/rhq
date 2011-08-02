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
import java.util.List;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Common stuff for the Domain
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class ManagedASComponent extends BaseComponent {

    /**
     * Get the availability of the managed AS server. We can't just check if
     * a connection succeeds, as the check runs against the API/HostController
     * and the managed server may still be down even if the connection succeeds.
     * @return Availability of the managed AS instance.
     */
    @Override
    public AvailabilityType getAvailability() {

        if (context.getResourceType().getName().equals("JBossAS-Managed")) {
            List<PROPERTY_VALUE> address = new ArrayList<PROPERTY_VALUE>(2);
            String host = pluginConfiguration.getSimpleValue("domainHost","local");
            address.add(new PROPERTY_VALUE("host",host));
            address.add(new PROPERTY_VALUE("server-config",myServerName));
            Operation getStatus = new Operation("read-attribute",address,"name","status");
            Result result = null;
            try {
                result = connection.execute(getStatus);
            } catch (Exception e) {
                log.warn(e.getMessage());
                return AvailabilityType.DOWN;
            }
            if (!result.isSuccess())
                return AvailabilityType.DOWN;

            String msg = result.getResult().toString();
            if (msg.contains("STARTED"))
                return AvailabilityType.UP;
            else
                return AvailabilityType.DOWN;
        }

        return super.getAvailability();
    }
}
