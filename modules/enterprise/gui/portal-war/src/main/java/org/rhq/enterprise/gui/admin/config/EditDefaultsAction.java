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

import java.util.ArrayList;
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
import org.rhq.core.domain.resource.composite.ResourceTypeTemplateCountComposite;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Populate the list of types to configure metric defaults
 */
public class EditDefaultsAction extends BaseDispatchAction {

    ResourceTypeManagerLocal typeMgr = LookupUtil.getResourceTypeManager();
    Map<Integer, ResourceTypeTemplateCountComposite> compositeMap;

    public ActionForward getMonitorDefaults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse resp) throws Exception {

        Subject subject = WebUtility.getSubject(request);

        String viewMode = WebUtility.getOptionalRequestParameter(request, "viewMode", null);

        // Next two are needed to sort the result by name - could be done in the DB
        List<ResourceType> platformTypes = null;
        List<ResourceType> serverTypes = null;

        if ("existing".equals(viewMode)) {
            platformTypes = typeMgr.getUtilizedResourceTypesByCategory(subject, ResourceCategory.PLATFORM, null);
            serverTypes = typeMgr.getUtilizedResourceTypesByCategory(subject, ResourceCategory.SERVER, null);
        } else {
            platformTypes = typeMgr.getAllResourceTypesByCategory(subject, ResourceCategory.PLATFORM);
            serverTypes = typeMgr.getAllResourceTypesByCategory(subject, ResourceCategory.SERVER);
        }

        /*
         * platform-services do not have duplicates in the ResourceType table, therefore we only need
         * to get a single set of them if there is even one platform that should be displayed on the UI
         */
        Map<Integer, SortedSet<ResourceType>> platformServices = new HashMap<Integer, SortedSet<ResourceType>>();
        if (platformTypes.size() > 0) {
            // if you deployed the perfplugin, the list of services will be 0 - if we see 0 services come back,
            // just ask for the next platform as a courtesy to developers who have perfplugin platform deployed
            for (ResourceType platformTypeEntry : platformTypes) {
                platformServices = typeMgr.getChildResourceTypesForResourceTypes(Arrays.asList(platformTypeEntry));
                if (platformServices.size() > 0) {
                    break; // we got them, no need to keep going
                }
            }
        }

        /*
         * on the other hand, we want to get the entire map of sets for descendants of server types 
         */
        Map<Integer, SortedSet<ResourceType>> services = typeMgr.getChildResourceTypesForResourceTypes(serverTypes);

        initCompositeMap();
        request.setAttribute(AttrConstants.ALL_PLATFORM_TYPES_ATTR, convertToComposite(platformTypes));
        request.setAttribute(AttrConstants.PLATFORM_SERVICES_ATTR, convertToComposite(platformServices));
        request.setAttribute(AttrConstants.ALL_SERVER_TYPES_ATTR, convertToComposite(serverTypes));
        request.setAttribute(AttrConstants.SERVICES_ATTR, convertToComposite(services));

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

    private List<ResourceTypeTemplateCountComposite> convertToComposite(List<ResourceType> intermediates) {
        List<ResourceTypeTemplateCountComposite> results = new ArrayList<ResourceTypeTemplateCountComposite>();
        for (ResourceType next : intermediates) {
            results.add(compositeMap.get(next.getId()));
        }

        return results;
    }

    private Map<Integer, SortedSet<ResourceTypeTemplateCountComposite>> convertToComposite(
        Map<Integer, SortedSet<ResourceType>> intermediates) {
        Map<Integer, SortedSet<ResourceTypeTemplateCountComposite>> results = new HashMap<Integer, SortedSet<ResourceTypeTemplateCountComposite>>();
        for (Integer key : intermediates.keySet()) {
            SortedSet<ResourceTypeTemplateCountComposite> composites = new TreeSet<ResourceTypeTemplateCountComposite>();
            results.put(key, composites);

            SortedSet<ResourceType> types = intermediates.get(key);
            for (ResourceType next : types) {
                composites.add(compositeMap.get(next.getId()));
            }
        }

        return results;
    }

    private void initCompositeMap() {
        compositeMap = typeMgr.getTemplateCountCompositeMap();
    }
}