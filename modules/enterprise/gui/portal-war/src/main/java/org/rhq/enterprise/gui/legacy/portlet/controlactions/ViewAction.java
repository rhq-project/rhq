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
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();

        OperationPortletPreferences operationPreferences = preferences.getOperationPortletPreferences();

        context.putAttribute("displayLastCompleted", operationPreferences.useLastCompleted);
        context.putAttribute("displayNextScheduled", operationPreferences.useNextScheduled);

        try {
            OperationManagerLocal manager = LookupUtil.getOperationManager();

            if (operationPreferences.useLastCompleted) {
                PageList<ResourceOperationLastCompletedComposite> rlist;
                PageControl pageControl = new PageControl(0, operationPreferences.lastCompleted);
                pageControl.initDefaultOrderingField("ro.createdTime", PageOrdering.DESC);
                rlist = manager.getRecentlyCompletedResourceOperations(user.getSubject(), pageControl);
                context.putAttribute("lastCompletedResource", rlist);

                PageList<GroupOperationLastCompletedComposite> glist;
                pageControl = new PageControl(0, operationPreferences.lastCompleted);
                pageControl.initDefaultOrderingField("go.createdTime", PageOrdering.DESC);
                glist = manager.getRecentlyCompletedGroupOperations(user.getSubject(), pageControl);
                context.putAttribute("lastCompletedGroup", glist);
            }

            if (operationPreferences.useNextScheduled) {
                PageList<ResourceOperationScheduleComposite> rlist;
                PageControl pageControl = new PageControl(0, operationPreferences.nextScheduled);
                rlist = manager.getCurrentlyScheduledResourceOperations(user.getSubject(), pageControl);
                context.putAttribute("nextScheduledResource", rlist);

                PageList<GroupOperationScheduleComposite> glist;
                pageControl = new PageControl(0, operationPreferences.nextScheduled);
                glist = manager.getCurrentlyScheduledGroupOperations(user.getSubject(), pageControl);
                context.putAttribute("nextScheduledGroup", glist);
            }
        } catch (Exception e) {
            LogFactory.getLog(ViewAction.class).error("Failed to get operations for portlet", e);
            PageControl pc = PageControl.getSingleRowInstance();
            context.putAttribute("lastCompletedResource", new PageList<Object>(pc));
            context.putAttribute("lastCompletedGroup", new PageList<Object>(pc));
            context.putAttribute("nextScheduledResource", new PageList<Object>(pc));
            context.putAttribute("nextScheduledGroup", new PageList<Object>(pc));
        }

        return null;
    }
}