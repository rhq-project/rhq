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
package org.rhq.enterprise.gui.legacy.portlet.criticalalerts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class PrepareAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(PrepareAction.class);
        PropertiesForm pForm = (PropertiesForm) form;
        WebUser user = SessionUtils.getWebUser(request.getSession());
        String key = ".dashContent.criticalalerts.resources";

        //this guarantees that the session dosen't contain any resources it shouldn't
        SessionUtils.removeList(request.getSession(), Constants.PENDING_RESOURCES_SES_ATTR);

        //set all the form properties
        pForm.setDisplayOnDash(true);

        String numberOfAlerts = user.getPreference(".dashContent.criticalalerts.numberOfAlerts");
        String past = user.getPreference(".dashContent.criticalalerts.past");
        String prioritity = user.getPreference(".dashContent.criticalalerts.priority");
        String selectedOrAll = user.getPreference(".dashContent.criticalalerts.selectedOrAll");

        DashboardUtils.verifyResources(key, user);

        pForm.setNumberOfAlerts(Integer.valueOf(numberOfAlerts));
        pForm.setPast(past);
        pForm.setPriority(prioritity);
        pForm.setSelectedOrAll(selectedOrAll);

        Integer[] resourcesIds = DashboardUtils.preferencesAsResourceIds(key, user);

        PageControl pageControl = WebUtility.getPageControl(request);

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        PageList<Resource> resources = resourceManager.getResourceByIds(user.getSubject(), resourcesIds, false,
            pageControl);

        request.setAttribute("criticalAlertsList", resources);
        request.setAttribute("criticalAlertsTotalSize", resources.getTotalSize());

        return null;
    }
}