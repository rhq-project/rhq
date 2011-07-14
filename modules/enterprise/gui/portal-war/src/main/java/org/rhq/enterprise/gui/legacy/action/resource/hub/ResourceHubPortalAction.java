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

import java.util.ArrayList;
import java.util.List;

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
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.InventorySummary;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.taglib.display.StringUtil;
import org.rhq.enterprise.gui.legacy.util.HubUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> that sets up the resource hub page (ResourceHub.jsp).
 *
 * @author Ian Springer
 */
public class ResourceHubPortalAction extends BaseAction {
    public static final int SELECTOR_GROUP_COMPAT = 1;
    public static final int SELECTOR_GROUP_ADHOC = 2;

    private static final String DEFAULT_RESOURCE_CATEGORY = ResourceCategory.PLATFORM.name();
    private static final String DEFAULT_RESOURCE_NAME = null;
    private static final String HIERARCHY_SEPARATOR = " > ";
    private static final String ALL_RESOURCE_TYPES = null;
    private static final String ALL_PLUGINS = null;

    protected Log log = LogFactory.getLog(ResourceHubPortalAction.class.getName());

    private MessageResources messages;

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

    private static String CHARSET = "UTF-16";

    // ---------------------------------------------------- Public Methods

    /**
     * Set up the Resource Hub portal.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        messages = getResources(request);
        ResourceHubForm hubForm = (ResourceHubForm) form;

        String searchExpression = hubForm.getKeywords();
        if (searchExpression != null && searchExpression.equals("Resource Name")) {
            searchExpression = null; // user didn't type a filter, just selected a category and clicked 'GO'
        }
        String category = hubForm.getResourceCategory();
        String subtab = "all";
        try {
            ResourceCategory.valueOf(category.toUpperCase());
            subtab = category.toLowerCase();
        } catch (Exception e) {
            subtab = "hub";
        }
        String url = "/rhq/inventory/browseResources.xhtml?subtab=" + subtab;
        if (searchExpression != null && !searchExpression.trim().equals("")) {
            url += "&search=name=" + searchExpression;
        }
        response.sendRedirect(url);

        org.rhq.core.domain.util.PageControl pageControl = WebUtility.getPageControl(request);
        Subject subject = RequestUtils.getSubject(request);

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // Setup whether we're displaying list view or chart view.
        HubUtils.initView(hubForm, user);

        // Find resources specified by category and potentially type.
        // Collect query params and replace invalid ones with defaults.
        String resourceName = getResourceName(request, hubForm);
        String resourceType = ALL_RESOURCE_TYPES;
        if ((hubForm.getResourceType() != null) && !hubForm.getResourceType().trim().equals("")) {
            resourceType = decode(hubForm.getResourceType());
        }

        if ((hubForm.getResourceCategory() == null) || hubForm.getResourceCategory().equals("")) {
            hubForm.setResourceCategory(DEFAULT_RESOURCE_CATEGORY);
        }

        ResourceCategory resourceCategory = ResourceCategory.valueOf(hubForm.getResourceCategory());
        String pluginName = ALL_PLUGINS;
        if ((hubForm.getPlugin() != null) && !hubForm.getPlugin().trim().equals("")) {
            pluginName = decode(hubForm.getPlugin());
        }
        PageList<ResourceComposite> resources = LookupUtil.getResourceManager().findResourceComposites(subject,
            resourceCategory, resourceType, pluginName, null, hubForm.getKeywords(), true, pageControl);
        request.setAttribute(Constants.ALL_RESOURCES_ATTR, resources);

        initResourceTypesAndPluginsPulldownMenu(hubForm, subject, resourceCategory, resourceName, resourceType,
            pluginName);

        request.setAttribute(Constants.ALL_RESOURCES_CONTROLLABLE, new ArrayList());
        initInventorySummary(subject, request);
        SessionUtils.resetReturnPath(request.getSession());
        request.setAttribute(Constants.PORTAL_KEY, Portal
            .createPortal("resource.hub.ResourceHubTitle", ".resource.hub"));

        String navHierarchy = buildResourceNavHierarchy(resourceCategory, resourceType);
        request.setAttribute(Constants.INVENTORY_HIERARCHY_ATTR, navHierarchy);
        return returnSuccess(request, mapping);
    }

    private String buildResourceNavHierarchy(ResourceCategory resourceCategory, String resourceTypeName) {
        String navHierarchy; // Start the navHierarchy with the resource category.
        navHierarchy = StringUtil.toUpperCaseAt(resourceCategory.toString(), 0) + "s" + HIERARCHY_SEPARATOR;
        if (resourceTypeName != null) {
            navHierarchy += resourceTypeName;
        } else {
            navHierarchy += "All " + StringUtil.toUpperCaseAt(resourceCategory.toString(), 0) + "s";
        }

        return navHierarchy;
    }

    private void initInventorySummary(Subject user, HttpServletRequest request)
        throws org.rhq.enterprise.server.auth.SessionNotFoundException,
        org.rhq.enterprise.server.auth.SessionTimeoutException, java.rmi.RemoteException {
        InventorySummary summary = LookupUtil.getResourceBoss().getInventorySummary(user);
        request.setAttribute(Constants.RESOURCE_SUMMARY_ATTR, summary);
    }

    private String getResourceName(HttpServletRequest request, ResourceHubForm hubForm) {
        String jsInserted = messages.getMessage("resource.hub.search.KeywordSearchText");
        String resourceName = hubForm.getKeywords();
        if ((resourceName != null) && (resourceName.equals("") || resourceName.equals(jsInserted))) {
            resourceName = DEFAULT_RESOURCE_NAME;
            hubForm.setKeywords(resourceName);
        }

        return resourceName;
    }

    protected void initResourceTypesAndPluginsPulldownMenu(ResourceHubForm hubForm, Subject subject,
        ResourceCategory resourceCategory, String nameFilter, String typeName, String pluginName) throws Exception {
        // Set the first entry in the menu to the label "All <ResourceCategory> Types".
        hubForm.addType(buildResourceTypeMenuCategoryLabel(resourceCategory));
        List<String> resourceTypeNames = resourceTypeManager.getUtilizedResourceTypeNamesByCategory(subject,
            resourceCategory, nameFilter, pluginName);
        for (String resourceTypeName : resourceTypeNames) {
            hubForm.addType(new LabelValueBean(resourceTypeName, encode(resourceTypeName)));
        }

        // Set the first entry in the menu to the label "All Plugins Types"
        hubForm.addPlugin(new LabelValueBean(messages.getMessage("resource.hub.filter.AllPlugins"), ""));
        List<Plugin> plugins = LookupUtil.getPluginManager().getPluginsByResourceTypeAndCategory(typeName,
            resourceCategory);
        for (Plugin plugin : plugins) {
            hubForm.addPlugin(new LabelValueBean(plugin.getDisplayName(), encode(plugin.getName())));
        }
    }

    protected LabelValueBean buildResourceTypeMenuCategoryLabel(@NotNull ResourceCategory resourceCategory) {
        String key = null;
        switch (resourceCategory) {
        case PLATFORM: {
            key = "resource.hub.filter.AllPlatformTypes";
            break;
        }

        case SERVER: {
            key = "resource.hub.filter.AllServerTypes";
            break;
        }

        case SERVICE: {
            key = "resource.hub.filter.AllServiceTypes";
            break;
        }
        }

        LabelValueBean menuLabel = new LabelValueBean(messages.getMessage(key), "");
        return menuLabel;
    }

    private String encode(String parameter) {
        return parameter.replaceAll(" ", "_");
    }

    private String decode(String parameter) {
        return parameter.replaceAll("_", " ");
    }
}