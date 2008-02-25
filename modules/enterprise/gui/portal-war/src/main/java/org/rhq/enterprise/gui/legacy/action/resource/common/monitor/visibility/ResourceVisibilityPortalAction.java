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
/*
 * Created on Aug 6, 2003
 *
 */
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceController;

/**
 * Base class for Resource Visibility Portal action. Put shared functionalility in this object.
 */
public abstract class ResourceVisibilityPortalAction extends ResourceController {
    public ActionForward currentHealth(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);
        return null;
    }

    public ActionForward resourceMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);
        return null;
    }

    public ActionForward performance(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);

        return null;
    }

    public ActionForward events(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);

        return null;
    }

}