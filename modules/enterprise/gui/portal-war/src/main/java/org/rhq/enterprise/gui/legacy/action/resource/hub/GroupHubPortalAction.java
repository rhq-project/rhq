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
package org.rhq.enterprise.gui.legacy.action.resource.hub;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.InventorySummary;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.HubUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> that sets up the resource hub page (ResourceHub.jsp).
 *
 * @author Ian Springer
 */
public class GroupHubPortalAction extends BaseAction {
    private static final String ALL_PLATFORMS_KEY = "resource.hub.filter.AllPlatformTypes";
    private static final String ALL_SERVERS_KEY = "resource.hub.filter.AllServerTypes";
    private static final String ALL_SERVICES_KEY = "resource.hub.filter.AllServiceTypes";

    public static final int SELECTOR_GROUP_COMPAT = 1;
    public static final int SELECTOR_GROUP_ADHOC = 2;

    protected Log log = LogFactory.getLog(GroupHubPortalAction.class);

    /**
     * Set up the Resource Hub portal.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        GroupHubForm hubForm = (GroupHubForm) form;
        PageControl pageControl = WebUtility.getPageControl(request);
        Subject subject = RequestUtils.getSubject(request);

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // Setup whether we're displaying list view or chart view.
        HubUtils.initView(hubForm, user);

        // Find resources specified by category and potentially type.
        // Collect query params and replace invalid ones with defaults.
        String groupName = getGroupName(request, hubForm);
        String resourceTypeName = HubConstants.ALL_RESOURCE_TYPES;
        ResourceCategory resourceCategory = null;
        if (hubForm.getResourceType() != null) {
            /* resourceCategory and resourceType are mutually exclusive
             *
             * the user will either: 1) filter on a single type, 2) filter on a category (a.k.a "ALL ( Platforms | Server
             * | Service ) Types") 3) or neither, meaning "All Groups" if the form's resourceType is null
             */

            int typeId = 0;
            try {
                typeId = Integer.parseInt(hubForm.getResourceType().trim());
            } catch (Exception e) {
            }

            if (typeId == -1) {
                resourceCategory = ResourceCategory.PLATFORM;
            } else if (typeId == -2) {
                resourceCategory = ResourceCategory.SERVER;
            } else if (typeId == -3) {
                resourceCategory = ResourceCategory.SERVICE;
            } else {
                resourceTypeName = decode(hubForm.getResourceType());
                if (resourceTypeName.equals("") || resourceTypeName.equals("ALL")) {
                    resourceTypeName = HubConstants.ALL_RESOURCE_TYPES;
                }
            }
        }

        String pluginName = hubForm.getPlugin();
        if (pluginName != null && pluginName.trim().equals("")) {
            pluginName = null;
        }
        if (pluginName != null) {
            pluginName = decode(pluginName);
        }

        if (hubForm.getGroupCategory() == null) {
            hubForm.setGroupCategory(HubConstants.DEFAULT_GROUP_CATEGORY);
        }

        GroupCategory groupCategory = GroupCategory.valueOf(hubForm.getGroupCategory());
        PageList<ResourceGroupComposite> groups = getGroups(subject, groupCategory, resourceCategory, resourceTypeName,
            pluginName, groupName, pageControl);
        request.setAttribute(Constants.ALL_RESOURCES_ATTR, groups);

        initGroupTypesPulldownMenu(request, hubForm, subject, groupCategory, groupName, resourceCategory,
            resourceTypeName, pluginName);
        initInventorySummary(subject, request);

        SessionUtils.resetReturnPath(request.getSession());

        Portal portal = Portal.createPortal("resource.hub.ResourceHubTitle", ".group.hub");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        String navHierarchy = HubUtils.buildNavHierarchy(groupCategory.toString(), resourceTypeName);
        request.setAttribute(Constants.INVENTORY_HIERARCHY_ATTR, navHierarchy);
        return returnSuccess(request, mapping);
    }

    private void initInventorySummary(Subject user, HttpServletRequest request)
        throws org.rhq.enterprise.server.auth.SessionNotFoundException,
        org.rhq.enterprise.server.auth.SessionTimeoutException, java.rmi.RemoteException {
        InventorySummary summary = LookupUtil.getResourceBoss().getInventorySummary(user);
        request.setAttribute(Constants.RESOURCE_SUMMARY_ATTR, summary);
    }

    private String getGroupName(HttpServletRequest request, GroupHubForm hubForm) {
        MessageResources res = getResources(request);
        String jsInserted = res.getMessage("resource.hub.search.KeywordSearchText");
        String groupName = hubForm.getKeywords();
        if ((groupName != null) && (groupName.equals("") || groupName.equals(jsInserted))) {
            groupName = HubConstants.DEFAULT_GROUP_NAME;
            hubForm.setKeywords(groupName);
        }

        return groupName;
    }

    @SuppressWarnings("deprecation")
    private void initGroupTypesPulldownMenu(HttpServletRequest request, GroupHubForm hubForm, Subject subject,
        GroupCategory groupCategory, String nameFilter, ResourceCategory resourceCategory, String resourceTypeName,
        String pluginName) throws Exception {
        // Set the first entry in the menu to the label "All Group Types".
        hubForm.addType(createMenuLabel(request, "resource.hub.filter.AllGroupTypes", "ALL"));
        if (groupCategory == GroupCategory.COMPATIBLE) {
            List<ResourceType> allResourceTypes = LookupUtil.getResourceTypeManager()
                .getResourceTypesForCompatibleGroups(subject, pluginName);
            Set<String> platformTypes = new TreeSet<String>();
            Set<String> serverTypes = new TreeSet<String>();
            Set<String> serviceTypes = new TreeSet<String>();
            for (ResourceType type : allResourceTypes) {
                ResourceCategory category = type.getCategory();
                if (category == ResourceCategory.PLATFORM) {
                    platformTypes.add(type.getName());
                } else if (category == ResourceCategory.SERVER) {
                    serverTypes.add(type.getName());
                } else if (category == ResourceCategory.SERVICE) {
                    serviceTypes.add(type.getName());
                } else {
                    throw new IllegalArgumentException("Unsupported ResourceCategory '" + category.name()
                        + "' in GroupHubPortalAction.initGroupTypesPulldownMenu");
                }
            }

            addResourceTypeMenuItems(hubForm, platformTypes, RequestUtils.message(request, ALL_PLATFORMS_KEY), "-1");
            addResourceTypeMenuItems(hubForm, serverTypes, RequestUtils.message(request, ALL_SERVERS_KEY), "-2");
            addResourceTypeMenuItems(hubForm, serviceTypes, RequestUtils.message(request, ALL_SERVICES_KEY), "-3");

            hubForm.addPlugin(createMenuLabel(request, "resource.hub.filter.AllPlugins", ""));
            List<Plugin> plugins = LookupUtil.getPluginManager().getPluginsByResourceTypeAndCategory(
                resourceTypeName, resourceCategory);
            for (Plugin plugin : plugins) {
                hubForm.addPlugin(new LabelValueBean(plugin.getDisplayName(), encode(plugin.getName())));
            }
        }
    }

    public void addResourceTypeMenuItems(HubForm form, Set<String> typeNames, String headerLabel, String headerValue) {
        if (!typeNames.isEmpty()) {
            form.addType(new LabelValueBean("", ""));
            form.addType(new LabelValueBean(headerLabel, headerValue));
            for (String resourceTypeName : typeNames) {
                form.addType(new LabelValueBean(resourceTypeName, encode(resourceTypeName)));
            }
        }
    }

    protected LabelValueBean createMenuLabel(HttpServletRequest req, String key, String value) {
        MessageResources messages = getResources(req);
        return new LabelValueBean(messages.getMessage(key), value);
    }

    private PageList<ResourceGroupComposite> getGroups(Subject subject, GroupCategory groupCategory,
        ResourceCategory resourceCategory, String resourceTypeName, String pluginName, String nameFilter,
        PageControl pageControl) throws Exception {
        log.debug("Finding all " + groupCategory + "s with " + "resource category [" + resourceCategory + "] and "
            + "resource type [" + resourceTypeName + "] and " + "resource name [" + nameFilter + "]...");

        PageList<ResourceGroupComposite> groups;

        if ((groupCategory == GroupCategory.COMPATIBLE) || (groupCategory == GroupCategory.MIXED)) {
            log.debug("getting compatible group list");
            groups = LookupUtil.getResourceGroupManager().findResourceGroupComposites(subject, groupCategory,
                resourceCategory, resourceTypeName, pluginName, nameFilter, null, null, pageControl);
        } else {
            throw new RuntimeException("ResourceHub doesn't currently support " + groupCategory.toString()
                + " groupCategory");
        }

        log.debug("found " + groups.size() + " groups");
        return groups;
    }

    private String encode(String parameter) {
        return parameter.replaceAll(" ", "_");
    }

    private String decode(String parameter) {
        return parameter.replaceAll("_", " ");
    }
}