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
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.AlertsPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.DisambiguatedResourceListUtil;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class PrepareAction extends TilesAction {
    
    private static final IntExtractor<Resource> RESOURCE_ID_EXTRACTOR = new IntExtractor<Resource>() {
        
        public int extract(Resource object) {
            return object.getId();
        }
    }; 
    
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(PrepareAction.class);
        PropertiesForm pForm = (PropertiesForm) form;
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getWebPreferences();

        //this guarantees that the session dosen't contain any resources it shouldn't
        SessionUtils.removeList(request.getSession(), Constants.PENDING_RESOURCES_SES_ATTR);

        //set all the form properties
        pForm.setDisplayOnDash(true);

        AlertsPortletPreferences alertPrefs = preferences.getAlertsPortletPreferences();
        pForm.setAlertsPortletPreferences(alertPrefs);

        PageControl pageControl = WebUtility.getPageControl(request);

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        PageList<Resource> resources = resourceManager.findResourceByIds(user.getSubject(), alertPrefs.asArray(), false,
            pageControl);

        PageList<DisambiguatedResourceListUtil.Record<Resource>> disambiguatedResources = 
            DisambiguatedResourceListUtil.buildResourceList(resourceManager.disambiguate(resources, true, RESOURCE_ID_EXTRACTOR),
                resources.getTotalSize(), resources.getPageControl(), false);
        
        request.setAttribute("criticalAlertsList", disambiguatedResources);
        request.setAttribute("criticalAlertsTotalSize", disambiguatedResources.getTotalSize());

        return null;
    }
}