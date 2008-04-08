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

package org.rhq.enterprise.gui.legacy.action.resource.common.events;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.KeyConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceController;

/**
 * @author hrupp
 *
 */
public class EventsPortalAction extends ResourceController {

    private static final String EVENTS_TITLE = "resource.common.monitor.events.EventsHeader"; // see ApplicationResource.properties
    private static final String EVENTS_PORTAL = ".resource.common.events.Events";

    private static final String EDIT_RANGE_TITLE = "resource.common.monitor.visibility.MetricDisplayRangeTitle";
    private static final String EDIT_RANGE_PORLET = ".resource.common.monitor.visibility.MetricDisplayRange";

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.legacy.action.BaseDispatchAction#getKeyMethodMap()
     */
    @Override
    protected Properties getKeyMethodMap() {

        Properties map = new Properties();
        map.setProperty("events", "events");
        map.setProperty(KeyConstants.MODE_MON_EDIT_RANGE, "editRange");

        return map;
    }

    public ActionForward events(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        setResource(request);

        Portal portal = Portal.createPortal(EVENTS_TITLE, EVENTS_PORTAL);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward editRange(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(EDIT_RANGE_TITLE, EDIT_RANGE_PORLET);
        portal.setWorkflowPortal(true);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

}
