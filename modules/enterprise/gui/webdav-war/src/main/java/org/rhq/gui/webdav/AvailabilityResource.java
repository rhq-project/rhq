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
package org.rhq.gui.webdav;

import java.util.Date;

import com.bradmcevoy.http.PropFindableResource;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The property to indicate if the resource is up or down.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class AvailabilityResource extends BasicResource implements PropFindableResource {
    private Availability availability;

    public AvailabilityResource(Subject subject, Resource managedResource) {
        super(subject, managedResource);
    }

    public String getUniqueId() {
        return "avail_" + getAvailability().getId();
    }

    public String getName() {
        return "availability_is_" + getAvailability().getAvailabilityType().getName();
    }

    /**
     * The modified date is that when the current availability took effect.
     * This is the same as {@link #getCreateDate()}.
     */
    public Date getModifiedDate() {
        return getAvailability().getStartTime();
    }

    /**
     * The created date is that when the current availability took effect.
     * This is the same as {@link #getModifiedDate()}.
     */
    public Date getCreateDate() {
        return getModifiedDate();
    }

    private Availability getAvailability() {
        if (this.availability == null) {
            AvailabilityManagerLocal am = LookupUtil.getAvailabilityManager();
            this.availability = am.getCurrentAvailabilityForResource(getSubject(), getManagedResource().getId());
        }
        return this.availability;
    }
}
