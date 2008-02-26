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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionRedirect;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.KeyConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.resource.GroupController;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A <code>BaseDispatchAction</code> that sets up compatible group monitor portals.
 */
public class GroupVisibilityPortalAction extends GroupController {
    private static final String TITLE_CURRENT_HEALTH = "resource.group.monitor.visibility.CurrentHealthTitle";

    private static final String PORTLET_CURRENT_HEALTH = ".resource.group.monitor.visibility.CurrentHealth";

    private static final String TITLE_FAVORITE_METRICS = "resource.group.monitor.visibility.FavoriteMetricsTitle";

    private static final String PORTLET_FAVORITE_METRICS = ".resource.group.monitor.visibility.FavoriteMetrics";

    private static final String TITLE_GROUP_METRICS = "resource.group.monitor.visibility.GroupMetricsTitle";

    private static final String PORTLET_GROUP_METRICS = ".resource.group.monitor.visibility.GroupMetrics";

    private static final String TITLE_PERFORMANCE = "resource.group.monitor.visibility.PerformanceTitle";

    private static final String PORTLET_PERFORMANCE = ".resource.group.monitor.visibility.Performance";

    private static final String TITLE_EVENTS = "resource.common.monitor.visibility.EventsTitle";

    private static final String PORTLET_EVENTS = ".resource.common.monitor.visibility.Events";

    protected static final Log log = LogFactory.getLog(GroupVisibilityPortalAction.class.getName());

    /**
     * Process the specified HTTP request, and create the corresponding HTTP response (or forward to another web
     * component that will create it). Return an <code>ActionForward</code> instance describing where and how control
     * should be forwarded, or <code>null</code> if the response has already been completed.
     *
     * @param     mapping  The ActionMapping used to select this instance
     * @param     request  The HTTP request we are processing
     * @param     response The HTTP response we are creating
     * @param     form     The optional ActionForm bean for this request (if any)
     *
     * @return    Describes where and how control should be forwarded.
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        ActionForward fwd = super.execute(mapping, form, request, response);

        if (!LookupUtil.getSystemManager().isMonitoringEnabled()) {
            ActionRedirect redirect = new ActionRedirect(mapping.findForward("inventory-jsf"));
            redirect.addParameter("groupId", request.getParameter("groupId"));
            redirect.addParameter("category", request.getParameter("category"));
            redirect.setRedirect(false);
            fwd = redirect;
        }

        return fwd;
    }

    @Override
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty(KeyConstants.MODE_MON_CUR, "currentHealth");
        map.setProperty(KeyConstants.MODE_MON_RES_METS, "resourceMetrics");
        map.setProperty(KeyConstants.MODE_MON_PERF, "performance");
        map.setProperty(KeyConstants.MODE_MON_EVENT, "events");
        return map;
    }

    public ActionForward currentHealth(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);

        request.setAttribute(KeyConstants.DEPL_CHILD_MODE_ATTR, "resourceMetrics");
        Portal portal = Portal.createPortal(TITLE_CURRENT_HEALTH, PORTLET_CURRENT_HEALTH);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward resourceMetrics(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);

        Portal portal = Portal.createPortal(TITLE_GROUP_METRICS, PORTLET_GROUP_METRICS);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward performance(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);

        Portal portal = Portal.createPortal(TITLE_PERFORMANCE, PORTLET_PERFORMANCE);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    public ActionForward events(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setResourceGroup(request);

        super.setNavMapLocation(request, mapping, Constants.MONITOR_VISIBILITY_LOC);

        Portal portal = Portal.createPortal(TITLE_EVENTS, PORTLET_EVENTS);
        request.setAttribute(AttrConstants.PORTAL_KEY, portal);
        return null;
    }

    private void findResourceHealths(HttpServletRequest request) throws Exception {
        Subject subject = WebUtility.getSubject(request);
        Integer groupId = WebUtility.getRequiredIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM);
        ResourceGroup group = LookupUtil.getResourceGroupManager().getResourceGroupById(subject, groupId, null);

        PageControl pageControl = WebUtility.getPageControl(request);

        // implicit is the "real" membership of the group, explicit is only used to keep implicit membership correct
        PageList<ResourceWithAvailability> healths = LookupUtil.getResourceManager()
            .getImplicitResourceWithAvailabilityByResourceGroup(subject, group, pageControl);

        if (log.isTraceEnabled()) {
            log.trace("got " + healths.size() + " ResourceTypeDisplays getting group member's health");
        }

        request.setAttribute(AttrConstants.GROUP_MEMBER_HEALTH_SUMMARIES_ATTR, healths);
    }
}