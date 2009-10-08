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
package org.rhq.enterprise.gui.legacy.action.resource.group.monitor;

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.config.ResourceConfigPortalAction;

/**
 * This action prepares the portal for configuring group monitoring.
 */
public class GroupConfigPortalAction extends ResourceConfigPortalAction {
    private static final String CONFIG_METRICS_PORTAL = ".resource.group.monitor.config.ConfigMetrics";
    private static final String CONFIG_METRICS_TITLE = "resource.group.monitor.visibility.config.ConfigureVisibility.Title";

    private static final String ADD_METRICS_PORTAL = ".resource.group.monitor.config.AddMetrics";
    private static final String ADD_METRICS_TITLE = "resource.group.monitor.visibility.config.AddMetrics.Title";

    private static final String EDIT_AVAILABILITY_PORTAL = ".resource.group.monitor.config.EditAvailability";
    private static final String EDIT_AVAILABILITY_TITLE = "resource.group.monitor.visibility.config.ConfigureVisibility.EditGroupAvail.Title";

    /* (non javadoc)
     * @see org.rhq.enterprise.gui.legacy.action.BaseDispatchAction#getKeyMethodMap()
     */
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(Constants.MODE_ADD_METRICS, "addMetrics");
        map.setProperty(Constants.MODE_CONFIGURE, "configMetrics");
        map.setProperty(Constants.MODE_EDIT, "editAvailability");
        map.setProperty(Constants.MODE_LIST, "configMetrics");
        return map;
    }

    /**
     * mode=configure || mode=view
     */
    public ActionForward configMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);

        super.configMetrics(mapping, form, request, response);

        Portal portal = Portal.createPortal(CONFIG_METRICS_TITLE, CONFIG_METRICS_PORTAL);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * mode=addMetrics
     */
    public ActionForward addMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(ADD_METRICS_TITLE, ADD_METRICS_PORTAL);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

    /**
     * mode=edit
     */
    public ActionForward editAvailability(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);
        Portal portal = Portal.createPortal(EDIT_AVAILABILITY_TITLE, EDIT_AVAILABILITY_PORTAL);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }
}