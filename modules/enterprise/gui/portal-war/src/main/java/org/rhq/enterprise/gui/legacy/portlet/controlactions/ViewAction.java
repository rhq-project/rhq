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
package org.rhq.enterprise.gui.legacy.portlet.controlactions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.operation.composite.GroupOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.GroupOperationScheduleComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationScheduleComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.OperationPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        boolean displayLastCompleted = false;
        boolean displayNextScheduled = false;

        PageList<ResourceOperationLastCompletedComposite> lastCompletedResourceOps = new PageList<ResourceOperationLastCompletedComposite>();
        PageList<GroupOperationLastCompletedComposite> lastCompletedGroupOps = new PageList<GroupOperationLastCompletedComposite>();
        PageList<ResourceOperationScheduleComposite> nextScheduledResourceOps = new PageList<ResourceOperationScheduleComposite>();
        PageList<GroupOperationScheduleComposite> nextScheduledGroupOps = new PageList<GroupOperationScheduleComposite>();

        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            if (user == null) {
                // session timed out, return prematurely
                return null;
            }

            WebUserPreferences preferences = user.getWebPreferences();

            OperationPortletPreferences operationPreferences = preferences.getOperationPortletPreferences();

            displayLastCompleted = operationPreferences.useLastCompleted;
            displayNextScheduled = operationPreferences.useNextScheduled;

            OperationManagerLocal manager = LookupUtil.getOperationManager();

            if (operationPreferences.useLastCompleted) {
                PageControl pageControl = new PageControl(0, operationPreferences.lastCompleted);
                pageControl.initDefaultOrderingField("ro.createdTime", PageOrdering.DESC);
                lastCompletedResourceOps = manager.findRecentlyCompletedResourceOperations(user.getSubject(), null,
                    pageControl);

                pageControl = new PageControl(0, operationPreferences.lastCompleted);
                pageControl.initDefaultOrderingField("go.createdTime", PageOrdering.DESC);
                lastCompletedGroupOps = manager.findRecentlyCompletedGroupOperations(user.getSubject(), pageControl);
            }

            if (operationPreferences.useNextScheduled) {
                PageControl pageControl = new PageControl(0, operationPreferences.nextScheduled);
                nextScheduledResourceOps = manager.findCurrentlyScheduledResourceOperations(user.getSubject(),
                    pageControl);

                pageControl = new PageControl(0, operationPreferences.nextScheduled);
                nextScheduledGroupOps = manager.findCurrentlyScheduledGroupOperations(user.getSubject(), pageControl);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [ControlActions] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [ControlActions] experienced an error: " + e.getMessage());
            }
        } finally {
            context.putAttribute("displayLastCompleted", displayLastCompleted);
            context.putAttribute("displayNextScheduled", displayNextScheduled);
            context.putAttribute("lastCompletedResource", lastCompletedResourceOps);
            context.putAttribute("lastCompletedGroup", lastCompletedGroupOps);
            context.putAttribute("nextScheduledResource", nextScheduledResourceOps);
            context.putAttribute("nextScheduledGroup", nextScheduledGroupOps);
        }

        return null;
    }
}