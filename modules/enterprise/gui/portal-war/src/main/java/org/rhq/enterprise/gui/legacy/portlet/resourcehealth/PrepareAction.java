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
package org.rhq.enterprise.gui.legacy.portlet.resourcehealth;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

public class PrepareAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        PropertiesForm pForm = (PropertiesForm) form;
        PageControl pc = WebUtility.getPageControl(request);
        WebUser user = SessionUtils.getWebUser(request.getSession());

        //this guarantees that the session doesn't contain any resources it shouldn't
        String key = Constants.USERPREF_KEY_FAVORITE_RESOURCES;
        DashboardUtils.verifyResources(key, user);
        SessionUtils.removeList(request.getSession(), Constants.PENDING_RESOURCES_SES_ATTR); // what is this?
        pForm.setDisplayOnDash(true);

        boolean availability = Boolean.parseBoolean(user.getPreference(".dashContent.resourcehealth.availability"));
        boolean alerts = Boolean.parseBoolean(user.getPreference(".dashContent.resourcehealth.alerts"));
        pForm.setAvailability(availability);
        pForm.setAlerts(alerts);

        Integer[] resourceIds = DashboardUtils.preferencesAsResourceIds(key, user); // all the resources
        List<Resource> resources = DashboardUtils.preferencesAsResources(key, user, pc); // paged, not all resources

        request.setAttribute("resourceHealthList", resources);
        request.setAttribute("resourceHealthTotalSize", resourceIds.length);

        return null;
    }
}