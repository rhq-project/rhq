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
import org.rhq.enterprise.server.resource.InventorySummary;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
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

        ResourceBossLocal resourceBoss = LookupUtil.getResourceBoss();
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        HttpSession session = request.getSession();
        WebUser user = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);

        // Setup whether we're displaying list view or chart view.
        HubUtils.initView(hubForm, user);

        // Find resources specified by category and potentially type.
        // Collect query params and replace invalid ones with defaults.
        String groupName = getGroupName(request, hubForm);
        ResourceType resourceType = HubConstants.ALL_RESOURCE_TYPES;
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
                // check for != 0 to suppress bad stack traces, even though this should never happen
                if (typeId != 0) {
                    resourceType = resourceTypeManager.getResourceTypeById(subject, typeId);
                }
            }
        }

        if (hubForm.getGroupCategory() == null) {
            hubForm.setGroupCategory(HubConstants.DEFAULT_GROUP_CATEGORY);
        }

        GroupCategory groupCategory = GroupCategory.valueOf(hubForm.getGroupCategory());
        PageList<ResourceGroupComposite> groups = getGroups(subject, groupManager, groupCategory, resourceCategory,
            resourceType, groupName, pageControl);
        request.setAttribute(Constants.ALL_RESOURCES_ATTR, groups);

        initGroupTypesPulldownMenu(request, hubForm, subject, resourceTypeManager, groupCategory, groupName);
        initInventorySummary(subject, request, resourceBoss);

        SessionUtils.resetReturnPath(request.getSession());

        Portal portal = Portal.createPortal("resource.hub.ResourceHubTitle", ".group.hub");
        request.setAttribute(Constants.PORTAL_KEY, portal);

        String navHierarchy = HubUtils.buildNavHierarchy(groupCategory.toString(), resourceType);
        request.setAttribute(Constants.INVENTORY_HIERARCHY_ATTR, navHierarchy);
        return returnSuccess(request, mapping);
    }

    private void initInventorySummary(Subject user, HttpServletRequest request, ResourceBossLocal resourceBoss)
        throws org.rhq.enterprise.server.auth.SessionNotFoundException,
        org.rhq.enterprise.server.auth.SessionTimeoutException, java.rmi.RemoteException {
        InventorySummary summary = resourceBoss.getInventorySummary(user);
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
        ResourceTypeManagerLocal resourceTypeManager, GroupCategory groupCategory, String nameFilter) throws Exception {
        // Set the first entry in the menu to the label "All Group Types".
        hubForm.addType(createMenuLabel(request, "resource.hub.filter.AllGroupTypes", ""));
        if (groupCategory == GroupCategory.COMPATIBLE) {
            List<ResourceType> allResourceTypes = resourceTypeManager.getResourceTypesForCompatibleGroups(subject);
            Set<ResourceType> platformTypes = new TreeSet<ResourceType>();
            Set<ResourceType> serverTypes = new TreeSet<ResourceType>();
            Set<ResourceType> serviceTypes = new TreeSet<ResourceType>();
            for (ResourceType type : allResourceTypes) {
                ResourceCategory category = type.getCategory();
                if (category == ResourceCategory.PLATFORM) {
                    platformTypes.add(type);
                } else if (category == ResourceCategory.SERVER) {
                    serverTypes.add(type);
                } else if (category == ResourceCategory.SERVICE) {
                    serviceTypes.add(type);
                } else {
                    throw new IllegalArgumentException("Unsupported ResourceCategory '" + category.name()
                        + "' in GroupHubPortalAction.initGroupTypesPulldownMenu");
                }
            }

            HubUtils.addResourceTypeMenuItems(hubForm, platformTypes, RequestUtils.message(request, ALL_PLATFORMS_KEY),
                "-1");
            HubUtils.addResourceTypeMenuItems(hubForm, serverTypes, RequestUtils.message(request, ALL_SERVERS_KEY),
                "-2");
            HubUtils.addResourceTypeMenuItems(hubForm, serviceTypes, RequestUtils.message(request, ALL_SERVICES_KEY),
                "-3");
        }
    }

    protected LabelValueBean createMenuLabel(HttpServletRequest req, String key, String value) {
        MessageResources messages = getResources(req);
        return new LabelValueBean(messages.getMessage(key), value);
    }

    private PageList<ResourceGroupComposite> getGroups(Subject subject, ResourceGroupManagerLocal groupManager,
        GroupCategory groupCategory, ResourceCategory resourceCategory, ResourceType resourceType, String nameFilter,
        PageControl pageControl) throws Exception {
        log.debug("Finding all " + groupCategory + "s with " + "resource category [" + resourceType + "] and "
            + "resource type [" + resourceType + "] and " + "resource name [" + nameFilter + "]...");

        PageList<ResourceGroupComposite> groups;

        if ((groupCategory == GroupCategory.COMPATIBLE) || (groupCategory == GroupCategory.MIXED)) {
            log.debug("getting compatible group list");
            groups = groupManager.getAllResourceGroups(subject, groupCategory, resourceCategory, resourceType,
                nameFilter, pageControl);
        } else {
            throw new RuntimeException("ResourceHub doesn't currently support " + groupCategory.toString()
                + " groupCategory");
        }

        log.debug("found " + groups.size() + " groups");
        return groups;
    }
}