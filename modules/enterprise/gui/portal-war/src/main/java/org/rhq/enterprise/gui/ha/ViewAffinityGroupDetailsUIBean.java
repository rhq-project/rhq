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
package org.rhq.enterprise.gui.ha;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cluster.AffinityGroup;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.cluster.AffinityGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAffinityGroupDetailsUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewAffinityGroupDetailsUIBean";

    private AffinityGroupManagerLocal affinityGroupManager = LookupUtil.getAffinityGroupManager();
    private AffinityGroup affinityGroup;

    public ViewAffinityGroupDetailsUIBean() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        int affinityGroupId = FacesContextUtility.getRequiredRequestParameter("affinityGroupId", Integer.class);
        affinityGroup = affinityGroupManager.getById(subject, affinityGroupId);
    }

    public AffinityGroup getAffinityGroup() {
        return affinityGroup;
    }

    public String edit() {
        return "edit";
    }

    public String save() {
        try {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            affinityGroupManager.update(subject, getAffinityGroup());
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "The affinity group has been updated.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error: " + e.getMessage());
            return "edit"; // stay in edit mode on failure
        }

        return "success";
    }

    public String cancel() {
        return "success";
    }

}