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
package org.rhq.enterprise.gui.admin.report;

import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourceInstallReportUIBean {


    private boolean groupByVersion;

    public List<ResourceInstallCount> getResourceInstallCounts() {

        return LookupUtil.getResourceManager().findResourceInstallCounts(EnterpriseFacesContextUtility.getSubject(), groupByVersion);

    }


    public boolean isGroupByVersion() {
        return groupByVersion;
    }

    public void setGroupByVersion(boolean groupByVersion) {
        this.groupByVersion = groupByVersion;
    }
}
