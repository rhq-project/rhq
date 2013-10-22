/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.enterprise.server.measurement;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @Author Jay Shaughnessy
 * @author Noam Malki
 */

@Remote
public interface AvailabilityManagerRemote {

    /**
     * @param subject the subject
     * @param resourceId the resource
     * @param pc page control
     * @return not null
     * @Deprecated use {@link #findAvailabilityByCriteria(Subject, AvailabilityCriteria)}
     */
    @Deprecated
    public PageList<Availability> findAvailabilityForResource(Subject subject, int resourceId, PageControl pc);

    /**
     * Gets the last known Availability for the given resource - which includes whether it is currently up (i.e.
     * available) or down and the last time it was known to have changed to that state.
     *
     * @param subject the subject
     * @param resourceId the resource
     *
     * @return the full and current status of the resource
     */
    public Availability getCurrentAvailabilityForResource(Subject subject, int resourceId);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<Availability> findAvailabilityByCriteria(Subject subject, AvailabilityCriteria criteria);
}
