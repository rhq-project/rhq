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
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.taglib.display.StringUtil;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.InventorySummary;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
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

    private static final String VIEW_ATTRIB = "Resource Hub View";

    private static final ResourceType ALL_RESOURCE_TYPES = null;

    protected Log log = LogFactory.getLog(ResourceHubPortalAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Set up the Resource Hub portal.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        ResourceHubForm hubForm = (ResourceHubForm) form;
        org.rhq.core.domain.util.PageControl pageControl = WebUtility.getPageControl(request);
        Subject subject = RequestUtils.getSubject(request);

        //ControlBoss controlBoss = ContextUtils.getControlBoss(context); // TODO: Get rid of this.
        ResourceBossLocal resourceBoss = LookupUtil.getResourceBoss();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

        HttpSession session = request.getSession();
        WebUser user = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);

        // Setup whether we're displaying list view or chart view.
        initView(hubForm, user);

        // Find resources specified by category and potentially type.
        // Collect query params and replace invalid ones with defaults.
        String resourceName = getResourceName(request, hubForm);
        ResourceType resourceType = ALL_RESOURCE_TYPES;
        if ((hubForm.getResourceType() != null) && !hubForm.getResourceType().trim().equals("")) {
            resourceType = resourceTypeManager
                .getResourceTypeById(subject, Integer.parseInt(hubForm.getResourceType()));
        }

        if ((hubForm.getResourceCategory() == null) || hubForm.getResourceCategory().equals("")) {
            hubForm.setResourceCategory(DEFAULT_RESOURCE_CATEGORY);
        }

        ResourceCategory resourceCategory = ResourceCategory.valueOf(hubForm.getResourceCategory());
        PageList<ResourceComposite> resources = getResources(hubForm, subject, resourceManager, resourceCategory,
            resourceType, resourceName, pageControl);
        request.setAttribute(Constants.ALL_RESOURCES_ATTR, resources);

        initResourceTypesPulldownMenu(request, hubForm, subject, resourceTypeManager, resourceCategory, resourceName);

        request.setAttribute(Constants.ALL_RESOURCES_CONTROLLABLE, new ArrayList());
        initInventorySummary(subject, request, resourceBoss);
        SessionUtils.resetReturnPath(request.getSession());
        Portal portal = createPortal();
        request.setAttribute(Constants.PORTAL_KEY, portal);

        String navHierarchy = buildResourceNavHierarchy(resourceCategory, resourceType);
        request.setAttribute(Constants.INVENTORY_HIERARCHY_ATTR, navHierarchy);
        return returnSuccess(request, mapping);
    }

    private String buildResourceNavHierarchy(ResourceCategory resourceCategory, ResourceType resourceType) {
        String navHierarchy; // Start the navHierarchy with the resource category.
        navHierarchy = StringUtil.toUpperCaseAt(resourceCategory.toString(), 0) + "s" + HIERARCHY_SEPARATOR;
        if (resourceType != null) {
            navHierarchy += getResourceTypeDisplayName(resourceType);
        } else {
            navHierarchy += "All " + StringUtil.toUpperCaseAt(resourceCategory.toString(), 0) + "s";
        }

        return navHierarchy;
    }

    private void initInventorySummary(Subject user, HttpServletRequest request, ResourceBossLocal resourceBoss)
        throws org.rhq.enterprise.server.auth.SessionNotFoundException,
        org.rhq.enterprise.server.auth.SessionTimeoutException, java.rmi.RemoteException {
        InventorySummary summary = resourceBoss.getInventorySummary(user);
        request.setAttribute(Constants.RESOURCE_SUMMARY_ATTR, summary);
    }

    private String getResourceName(HttpServletRequest request, ResourceHubForm hubForm) {
        MessageResources res = getResources(request);
        String jsInserted = res.getMessage("resource.hub.search.KeywordSearchText");
        String resourceName = hubForm.getKeywords();
        if ((resourceName != null) && (resourceName.equals("") || resourceName.equals(jsInserted))) {
            resourceName = DEFAULT_RESOURCE_NAME;
            hubForm.setKeywords(resourceName);
        }

        return resourceName;
    }

    protected Portal createPortal() {
        Portal portal = Portal.createPortal("resource.hub.ResourceHubTitle", ".resource.hub");
        return portal;
    }

    protected PageList<ResourceComposite> getResources(ResourceHubForm hubForm, Subject subject,
        ResourceManagerLocal resourceManager, ResourceCategory resourceCategory, ResourceType resourceType,
        String nameFilter, PageControl pageControl) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Finding all resources with category [" + resourceCategory + "] and resource type ["
                + resourceType + "] and resource name [" + nameFilter + "]...");
        }

        return resourceManager.findResourceComposites(subject, resourceCategory, resourceType, null, hubForm
            .getKeywords(), pageControl);
    }

    protected void initResourceTypesPulldownMenu(HttpServletRequest request, ResourceHubForm hubForm, Subject subject,
        ResourceTypeManagerLocal resourceTypeManager, ResourceCategory resourceCategory, String nameFilter)
        throws Exception {
        // Set the first entry in the menu to the label "All <ResourceCategory> Types".
        hubForm.addType(buildResourceTypeMenuCategoryLabel(request, resourceCategory));
        List<ResourceType> resourceTypes = resourceTypeManager.getUtilizedResourceTypesByCategory(subject,
            resourceCategory, nameFilter);

        //Set<ResourceType> resourceTypes = getResourceTypesRepresentedByResources(resources);
        addTypeMenuItems(hubForm, resourceTypes);
    }

    protected LabelValueBean buildResourceTypeMenuCategoryLabel(HttpServletRequest request, @NotNull
    ResourceCategory resourceCategory) {
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

        LabelValueBean menuLabel = createMenuLabel(request, key, "");
        return menuLabel;
    }

    protected LabelValueBean createMenuLabel(HttpServletRequest req, String key, String value) {
        MessageResources messages = getResources(req);
        return new LabelValueBean(messages.getMessage(key), value);
    }

    private void initView(ResourceHubForm hubForm, WebUser user) throws Exception {
        HubView prefView;
        try {
            prefView = HubView.valueOf(user.getPreference(VIEW_ATTRIB));
        } catch (IllegalArgumentException ioe) {
            prefView = HubView.LIST;
        }

        String viewStr = hubForm.getView();
        if (viewStr == null) {
            hubForm.setView(prefView.name());
        }

        HubView view = HubView.valueOf(hubForm.getView().toUpperCase());
        if (!view.equals(prefView)) {
            user.setPreference(VIEW_ATTRIB, view); // Save new preference.
            user.persistPreferences();
        }
    }

    protected void addTypeMenuItems(ResourceHubForm hubForm, List<ResourceType> resourceTypes) {
        for (ResourceType resourceType : resourceTypes) {
            String typeDisplayName = getResourceTypeDisplayName(resourceType);
            hubForm.addType(new LabelValueBean(typeDisplayName, Integer.toString(resourceType.getId())));
        }
    }

    private String getResourceTypeDisplayName(ResourceType resourceType) {
        // TODO: Type display name should probably also include ancestor server/service type names.
        //       (e.g. "JBoss Datasource" rather than simply "Datasource" to distinguish a JBoss datasource from a
        //       WebLogic datasource)
        return resourceType.getName();
    }
}