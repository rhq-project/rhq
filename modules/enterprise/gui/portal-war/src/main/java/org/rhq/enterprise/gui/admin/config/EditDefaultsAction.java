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
package org.rhq.enterprise.gui.admin.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Populate the list of types to configure metric defaults
 */
public class EditDefaultsAction extends BaseDispatchAction {
    public ActionForward getMonitorDefaults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse resp) throws Exception {

        ResourceTypeManagerLocal typeMgr = LookupUtil.getResourceTypeManager();

        Subject subject = RequestUtils.getSubject(request);

        String viewMode = WebUtility.getOptionalRequestParameter(request, "viewMode", null);

        List<ResourceType> platformTypesTmp;
        List<ResourceType> serverTypesTmp;

        // Next two are needed to sort the result by name - could be done in the DB
        SortedSet<ResourceType> platformTypes = new TreeSet<ResourceType>();
        SortedSet<ResourceType> serverTypes = new TreeSet<ResourceType>();

        if ("existing".equals(viewMode)) {
            platformTypesTmp = typeMgr.getUtilizedResourceTypesByCategory(subject, ResourceCategory.PLATFORM, null);
            serverTypesTmp = typeMgr.getUtilizedResourceTypesByCategory(subject, ResourceCategory.SERVER, null);
        } else {
            platformTypesTmp = typeMgr.getAllResourceTypesByCategory(subject, ResourceCategory.PLATFORM);
            serverTypesTmp = typeMgr.getAllResourceTypesByCategory(subject, ResourceCategory.SERVER);
        }

        /*
         * platform-services do not have duplicates in the ResourceType table, therefore we only need
         * to get a single set of them if there is even one platform that should be displayed on the UI
         */
        Map<Integer, SortedSet<ResourceType>> platformServices = new HashMap<Integer, SortedSet<ResourceType>>();
        if (platformTypesTmp.size() > 0) {
            platformServices = typeMgr.getChildResourceTypesForResourceTypes(Arrays.asList(platformTypesTmp.get(0)));
        }

        /*
         * on the other hand, we want to get the entire map of sets for descendants of server types 
         */
        Map<Integer, SortedSet<ResourceType>> services = typeMgr.getChildResourceTypesForResourceTypes(serverTypesTmp);

        platformTypes.addAll(platformTypesTmp);
        serverTypes.addAll(serverTypesTmp);
        request.setAttribute(AttrConstants.ALL_PLATFORM_TYPES_ATTR, platformTypes);
        request.setAttribute(AttrConstants.ALL_SERVER_TYPES_ATTR, serverTypes);
        request.setAttribute(AttrConstants.SERVICES_ATTR, services);
        request.setAttribute(AttrConstants.PLATFORM_SERVICES_ATTR, platformServices);

        request.setAttribute("viewMode", viewMode);

        Portal p = Portal.createPortal("admin.home.ResourceTemplates", ".admin.config.EditMonitorConfig");
        request.setAttribute(AttrConstants.PORTAL_KEY, p);
        return null;
    }

    @Override
    protected Properties getKeyMethodMap() {
        Properties map = new Properties();
        map.setProperty("monitor", "getMonitorDefaults");
        return map;
    }
}