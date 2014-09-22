/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.plugins.cassandra;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class AntiEntropySessionsComponent extends MBeanResourceComponent<JMXComponent<?>> {

    @Override
    public AvailabilityType getAvailability() {
        AvailabilityType availability = super.getAvailability();

        //NOTE: Anti Entropy Sessions resource is up and discoverable
        //      only after running repair and until the next resource
        //      restart. However, keeping the resource in inventory
        //      long term provides good telemetry for the repair operation.
        //
        //      By replacing DOWN with MISSING, the users are empowered
        //      to decide what to do with the resource once DOWN.
        //      By default MISSING will be DOWN, but users can configure
        //      it further. Please see the MISSING documentation for more
        //      details.
        if (AvailabilityType.DOWN.equals(availability)) {

            availability = AvailabilityType.MISSING;
        }

        return availability;
    }
}
