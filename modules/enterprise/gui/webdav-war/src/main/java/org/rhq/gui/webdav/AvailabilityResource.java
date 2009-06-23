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

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.PropFindableResource;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.enterprise.server.measurement.AvailabilityManagerBean;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class AvailabilityResource extends BasicResource implements PropFindableResource {


    private Resource resource;
    private Availability availability;

    public AvailabilityResource(Resource resource) {
        this.resource = resource;
        AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
        this.availability = availabilityManager.getCurrentAvailabilityForResource(LookupUtil.getSubjectManager().getOverlord(), resource.getId());
    }

    public String getUniqueId() {
        return "avail_" + availability.getId();
    }

    public String getName() {
        return "availability_is_" + availability.getAvailabilityType().getName();
    }

    public Date getModifiedDate() {
        return availability.getStartTime();
    }

    public String checkRedirect(Request request) {
        return null;  
    }

    public Date getCreateDate() {
        return availability.getStartTime();
    }
}
