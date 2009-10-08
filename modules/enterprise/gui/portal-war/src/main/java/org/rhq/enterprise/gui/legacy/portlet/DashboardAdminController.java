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
package org.rhq.enterprise.gui.legacy.portlet;

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;

/**
 * A <code>Controller</code> that sets up dashboard portlets
 */
public class DashboardAdminController extends BaseDispatchAction {
    protected static Log log = LogFactory.getLog(DashboardAdminController.class.getName());

    private static Properties keyMethodMap = new Properties();

    static {
        keyMethodMap.setProperty("savedQueries", "savedQueries");
        keyMethodMap.setProperty("resourceHealth", "resourceHealth");
        keyMethodMap.setProperty("recentlyApproved", "recentlyApproved");
        keyMethodMap.setProperty("criticalAlerts", "criticalAlerts");
        keyMethodMap.setProperty("summaryCounts", "summaryCounts");
        keyMethodMap.setProperty("autoDiscovery", "autoDiscovery");
        keyMethodMap.setProperty("controlActions", "controlActions");
        keyMethodMap.setProperty("changeLayout", "changeLayout");
        keyMethodMap.setProperty("problemResources", "problemResources");
        keyMethodMap.setProperty("softwareSummary", "softwareSummary");
        keyMethodMap.setProperty("rsrcHealthAddResources", "rsrcHealthAddResources");
        keyMethodMap.setProperty("crtAlertsAddResources", "crtAlertsAddResources");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward savedQueries(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.SQ", ".dashContent.admin.savedQueries");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward resourceHealth(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.RH", ".dashContent.admin.resourceHealth");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward recentlyApproved(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.RA", ".dashContent.admin.recentlyApproved");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward criticalAlerts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.A", ".dashContent.admin.criticalAlerts");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward summaryCounts(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.SC", ".dashContent.admin.summaryCounts");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward autoDiscovery(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.AD", ".dashContent.admin.autoDiscovery");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward controlActions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.CA", ".dashContent.admin.controlActions");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward rsrcHealthAddResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.RH.addResources",
            ".dashContent.admin.resourcehealth.addResources");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward crtAlertsAddResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.A.addResources",
            ".dashContent.admin.criticalAlerts.addResources");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward changeLayout(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.PL", ".dashContent.admin.changeLayout");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward problemResources(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.PR", ".dashContent.admin.problemResources");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward softwareSummary(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal("dash.settings.PageTitle.SoftwareSummary",
            ".dashContent.admin.softwareSummary");

        portal.setDialog(true);

        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }
}