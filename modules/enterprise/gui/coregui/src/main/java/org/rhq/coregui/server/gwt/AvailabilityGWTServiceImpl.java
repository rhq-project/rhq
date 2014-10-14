/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.server.gwt;

import java.util.List;

import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.util.Instant;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.AvailabilityGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */
public class AvailabilityGWTServiceImpl extends AbstractGWTServiceImpl implements AvailabilityGWTService {

    private static final long serialVersionUID = 1L;

    private AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();

    @Override
    public PageList<Availability> findAvailabilityByCriteria(AvailabilityCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(availabilityManager.findAvailabilityByCriteria(getSessionSubject(), criteria),
                "AvailabilityService.findAvailabilityByCriteria");
        } catch (Exception t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public List<Availability> getAvailabilitiesForResource(int resourceId, Instant start, Instant end)
        throws RuntimeException {
        try {
            Long now = System.currentTimeMillis();
            long endInMillis = end.toDate().getTime();
            if (now < end.toDate().getTime()) {
                // we can't foretell the future (this may be caused by different timezone on client's)
                endInMillis = now + 1;
            }
            if (start.toDate().getTime() < endInMillis) {
                return SerialUtility.prepare(availabilityManager.getAvailabilitiesForResource(getSessionSubject(),
                    resourceId, start.toDate().getTime(), endInMillis),
                    "AvailabilityService.getAvailabilitiesForResource");
            } else {
                throw new IllegalStateException(
                    "End time before start time. Check the timezone settins if it is the same as on the server-side.");
            }
        } catch (Exception t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public List<ResourceGroupAvailability> getAvailabilitiesForResourceGroup(int groupId, Instant start, Instant end)
        throws RuntimeException {
        try {
            Long now = System.currentTimeMillis();
            long endInMillis = end.toDate().getTime();
            if (now < end.toDate().getTime()) {
                // we can't foretell the future (this may be caused by different timezone on client's)
                endInMillis = now + 1;
            }
            if (start.toDate().getTime() < endInMillis) {
                return SerialUtility.prepare(availabilityManager.getAvailabilitiesForResourceGroup(getSessionSubject(),
                    groupId, start.toDate().getTime(), endInMillis),
                    "AvailabilityService.getAvailabilitiesForResourceGroup");
            } else {
                throw new IllegalStateException(
                    "End time before start time. Check the timezone settins if it is the same as on the server-side.");
            }
        } catch (Exception t) {
            throw getExceptionToThrowToClient(t);
        }
    }

}
