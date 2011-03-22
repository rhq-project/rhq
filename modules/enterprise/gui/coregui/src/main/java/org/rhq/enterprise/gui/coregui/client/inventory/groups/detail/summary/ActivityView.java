/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.summary;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardCategory;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.InitializableView;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardContainer;
import org.rhq.enterprise.gui.coregui.client.dashboard.DashboardView;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupAlertsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupBundleDeploymentsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupOperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * The content pane for the group Summary>Dashboard subtab.
 *
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 */

public class ActivityView extends LocatableVLayout implements DashboardContainer, InitializableView {

    private static final String DASHBOARD_NAME_PREFIX = "GroupDashboard_";

    private ResourceGroupComposite groupComposite;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    private DashboardView dashboardView;

    private LocatableToolStrip footer;
    private IButton editButton;
    private IButton resetButton;

    // Capture the user's global permissions for use by any dashboard or portlet that may need it for rendering.
    private Set<Permission> globalPermissions;

    private boolean editMode = false;
    private boolean isInitialized = false;
    //default portlet positioning parameters
    private int colLeft = 0;
    private int colRight = 1;
    private int rowLeft = 0;
    private int rowRight = 0;
    private boolean displayLeft = false;

    public ActivityView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);
        this.groupComposite = groupComposite;
    }

    @Override
    protected void onInit() {
        if (!isInitialized()) {
            super.onInit();

            // first async call to get global permissions
            new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

                public void onPermissionsLoaded(Set<Permission> permissions) {
                    globalPermissions = permissions;

                    // now make async call to look for customized dash for this user and entity
                    DashboardCriteria criteria = new DashboardCriteria();
                    criteria.addFilterCategory(DashboardCategory.GROUP);
                    criteria.addFilterGroupId(groupComposite.getResourceGroup().getId());
                    dashboardService.findDashboardsByCriteria(criteria, new AsyncCallback<PageList<Dashboard>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error1(), caught);
                        }

                        public void onSuccess(final PageList<Dashboard> result) {
                            Dashboard dashboard = result.isEmpty() ? getDefaultDashboard() : result.get(0);
                            setDashboard(dashboard);

                            isInitialized = true;

                            // draw() may be done since onInit finishes asynchronously, if so redraw
                            if (isDrawn()) {
                                markForRedraw();
                            }
                        }
                    });
                }
            });
        }
    }

    private void setDashboard(Dashboard dashboard) {
        Canvas[] members = getMembers();
        removeMembers(members);
        //pass in the group information
        dashboardView = new DashboardView(extendLocatorId(dashboard.getName()), this, dashboard, groupComposite, null);
        addMember(dashboardView);

        footer = new LocatableToolStrip(extendLocatorId("Footer"));
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        editButton = new LocatableIButton(footer.extendLocatorId("Mode"), editMode ? MSG.common_title_view_mode() : MSG
            .common_title_edit_mode());
        editButton.setAutoFit(true);
        editButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                editMode = !editMode;
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                dashboardView.setEditMode(editMode);
            }
        });

        resetButton = new LocatableIButton(footer.extendLocatorId("Reset"), MSG.common_button_reset());
        resetButton.setAutoFit(true);
        resetButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                String message = MSG.view_summaryDashboard_resetConfirm();

                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            dashboardView.delete();
                            setDashboard(getDefaultDashboard());
                            markForRedraw();
                        }
                    }
                });
            }
        });

        footer.addMember(editButton);
        footer.addMember(resetButton);

        addMember(footer);
    }

    protected Dashboard getDefaultDashboard() {
        Subject sessionSubject = UserSessionManager.getSessionSubject();
        final ResourceGroup group = groupComposite.getResourceGroup();

        final Dashboard dashboard = new Dashboard();

        dashboard.setName(DASHBOARD_NAME_PREFIX + sessionSubject.getId() + "_" + group.getId());
        dashboard.setCategory(DashboardCategory.GROUP);
        dashboard.setGroup(group);
        dashboard.setColumns(2);

        // set leftmost column and let the rest be equally divided
        dashboard.setColumnWidths("40%");
        dashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, "#F1F2F3"));

        //figure out which portlets to display and how
        Map<String, String> groupKeyNameMap = new HashMap<String, String>(PortletFactory
            .getRegisteredGroupPortletNameMap());
        //remove BundleDeployment and add back later if relevant.
        groupKeyNameMap.remove(GroupBundleDeploymentsPortlet.KEY);
        groupKeyNameMap = DashboardView.processPortletNameMapForGroup(groupKeyNameMap, groupComposite);

        //initiate asynch call to execute and then update the dashboard with bundle deployment portlet if necessary
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(group.getId());
        criteria.fetchExplicitResources(true);
        criteria.setPageControl(new PageControl(0, 1));
        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroup>>() {
                @Override
                public void onSuccess(PageList<ResourceGroup> results) {
                    if (!results.isEmpty()) {
                        ResourceGroup grp = results.get(0);
                        Set<Resource> explicitMembers = grp.getExplicitResources();
                        Resource[] currentResources = new Resource[explicitMembers.size()];
                        explicitMembers.toArray(currentResources);
                        Map<String, String> portletToAdd = new HashMap<String, String>();
                        //membership dynamically determined if all platforms then will be compatible.
                        if (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)) {
                            if (currentResources[0].getResourceType().getCategory().equals(ResourceCategory.PLATFORM)) {
                                //this portlet allowed to add bundle portlet monitoring
                                portletToAdd.put(GroupBundleDeploymentsPortlet.KEY, GroupBundleDeploymentsPortlet.NAME);
                            }
                            if (!portletToAdd.isEmpty()) {
                                //update dashboard with that portlet and reload
                                updateDashboardWithPortlets(portletToAdd, dashboard, 100);
                                //request reload of dashboard widget
                                setDashboard(dashboard);
                                markForRedraw();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving information for group [" + group.getId() + "]:" + caught.getMessage());
                }
            });

        //reset positioning parameters
        colLeft = 0;
        colRight = 1;
        rowLeft = 0;
        rowRight = 0;
        //Left Column
        if (groupKeyNameMap.containsKey(GroupMetricsPortlet.KEY)) {//measurments top left if available
            DashboardPortlet measurements = new DashboardPortlet(GroupMetricsPortlet.NAME, GroupMetricsPortlet.KEY, 220);
            dashboard.addPortlet(measurements, colLeft, rowLeft++);
            groupKeyNameMap.remove(GroupMetricsPortlet.KEY);
        }

        // right Column(approx 60%. As larger more room to display table and N rows.)
        if (groupKeyNameMap.containsKey(GroupAlertsPortlet.KEY)) {//alerts top right if available
            DashboardPortlet alerts = new DashboardPortlet(GroupAlertsPortlet.NAME, GroupAlertsPortlet.KEY, 220);
            dashboard.addPortlet(alerts, colRight, rowRight++);
            groupKeyNameMap.remove(GroupAlertsPortlet.KEY);
        }
        if (groupKeyNameMap.containsKey(GroupOperationsPortlet.KEY)) {//operations if available
            DashboardPortlet ops = new DashboardPortlet(GroupOperationsPortlet.NAME, GroupOperationsPortlet.KEY, 220);
            dashboard.addPortlet(ops, colRight, rowRight++);
            groupKeyNameMap.remove(GroupOperationsPortlet.KEY);
        }

        //Fill out left column(typically smaller portlets) then alternate cols with remaining
        displayLeft = false;
        updateDashboardWithPortlets(groupKeyNameMap, dashboard, 100);
        return dashboard;
    }

    /**Iterates list of new portlets and updates the dashboard reference with these new portlets. 
     * Attempts to fill the spaces around the remaining larger portlets if already installed, then alternates
     * adding to left and right columns. Assumes dashboard has only two columns.
     * 
     * @param keyNameMap portlet key|name map
     * @param dashboard dasboard instance to update
     */
    private void updateDashboardWithPortlets(Map<String, String> keyNameMap, Dashboard dashboard, int initialHeight) {
        if ((keyNameMap != null) && (dashboard != null)) {
            for (String key : keyNameMap.keySet()) {
                //locate portlet and add to dashboard
                DashboardPortlet portlet = new DashboardPortlet(keyNameMap.get(key), key, initialHeight);
                if (rowLeft < 4) {
                    dashboard.addPortlet(portlet, colLeft, rowLeft++);
                } else {//alternate
                    if (!displayLeft) {
                        dashboard.addPortlet(portlet, colRight, rowRight++);
                    } else {
                        dashboard.addPortlet(portlet, colLeft, rowLeft++);
                    }
                    //toggle
                    displayLeft = !displayLeft;
                }
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    public Set<Permission> getGlobalPermissions() {
        return globalPermissions;
    }

    /**
     * name update not supported because the name is derived from the entity id.
     * @return
     */
    public boolean supportsDashboardNameEdit() {
        return false;
    }

    public void updateDashboardNames() {
        return;
    }

}
