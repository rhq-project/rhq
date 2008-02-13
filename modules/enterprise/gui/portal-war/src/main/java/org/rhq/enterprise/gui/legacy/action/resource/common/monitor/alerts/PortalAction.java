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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts;

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.event.alert.Alert;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.Portlet;
import org.rhq.enterprise.gui.legacy.action.resource.ResourceController;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A dispatcher for the alerts portal.
 */
public class PortalAction extends ResourceController {
    protected static Log log = LogFactory.getLog(PortalAction.class);

    @Override
    protected Properties getKeyMethodMap() {
        log.trace("Building method map ...");
        Properties map = new Properties();

        map.put(Constants.MODE_VIEW, "listAlerts");
        map.put(Constants.MODE_LIST, "listAlerts");
        map.put("viewAlert", "viewAlert");

        return map;
    }

    private void setTitle(HttpServletRequest request, Portal portal, String titleName) throws Exception {
        Resource resource = RequestUtils.getResource(request);

        if (resource != null) {
            // this is an alert against a single resource
            ResourceType resourceType = resource.getResourceType();
            ResourceCategory resourceCategory = resourceType.getCategory();
            String categoryName = resourceCategory.name().toLowerCase();

            titleName = StringUtil.replace(titleName, "platform", categoryName);
        } else if (RequestUtils.getResourceGroup(request) != null) {
            // we don't have different titles for groups
            titleName = StringUtil.replace(titleName, "platform", "group");
        } else {
            // i guess this is an auto-group?
            titleName = StringUtil.replace(titleName, "platform.", "");
        }

        portal.setName(titleName);
    }

    public ActionForward viewAlert(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);

        Portal portal = Portal.createPortal();

        setTitle(request, portal, "alert.current.platform.detail.Title");
        portal.setDialog(true);
        portal.addPortlet(new Portlet(".events.alert.view"), 1);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        // Get alert definition name
        Integer alertId = new Integer(request.getParameter("a"));

        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        Alert alert = alertManager.getById(alertId);
        AlertDefinition alertDefinition = alert.getAlertDefinition();

        request.setAttribute(Constants.TITLE_PARAM2_ATTR, alertDefinition.getName());

        return null;
    }

    public ActionForward listAlerts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResource(request);

        super.setNavMapLocation(request, mapping, Constants.ALERT_LOC);

        // clean out the return path
        SessionUtils.resetReturnPath(request.getSession());

        // set the return path
        try {
            setReturnPath(request, mapping);
        } catch (ParameterNotFoundException pnfe) {
            if (log.isDebugEnabled()) {
                log.debug("", pnfe);
            }
        }

        Portal portal = Portal.createPortal();
        setTitle(request, portal, "alerts.alert.platform.AlertList.Title");
        portal.setDialog(false);
        portal.addPortlet(new Portlet(".events.alert.list"), 1);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }
}

// EOF
