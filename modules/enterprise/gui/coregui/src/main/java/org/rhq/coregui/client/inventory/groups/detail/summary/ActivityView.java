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
package org.rhq.coregui.client.inventory.groups.detail.summary;

import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.DashboardCriteria;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardCategory;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.InitializableView;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.dashboard.DashboardContainer;
import org.rhq.coregui.client.dashboard.DashboardView;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupAlertsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupConfigurationUpdatesPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupEventsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupOperationsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceEventsPortlet;
import org.rhq.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The content pane for the group Summary>Activity subtab.
 *
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 */
public class ActivityView extends EnhancedVLayout implements DashboardContainer, InitializableView {

    private static final String DASHBOARD_NAME_PREFIX = "GroupDashboard_";

    private ResourceGroupComposite groupComposite;
    private boolean isAutoGroup;
    private boolean isAutoCluster;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    private DashboardView dashboardView;

    private EnhancedToolStrip footer;
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

    public ActivityView(ResourceGroupComposite groupComposite, boolean isAutoCluster, boolean isAutoGroup) {
        super();
        this.groupComposite = groupComposite;
        this.isAutoCluster = isAutoCluster;
        this.isAutoGroup = isAutoGroup;
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
        EntityContext context = EntityContext.forGroup(groupComposite.getResourceGroup().getId(), isAutoCluster,
            isAutoGroup);
        dashboardView = new DashboardView(this, dashboard, context, groupComposite);
        addMember(dashboardView);

        footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        editButton = new EnhancedIButton(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
        editButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                editMode = !editMode;
                editButton.setTitle(editMode ? MSG.common_title_view_mode() : MSG.common_title_edit_mode());
                dashboardView.setEditMode(editMode);
            }
        });

        resetButton = new EnhancedIButton(MSG.common_button_reset());
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
        Map<String, String> groupKeyNameMap = DashboardView.processPortletNameMapForGroup(groupComposite);

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
            DashboardPortlet alerts = new DashboardPortlet(GroupAlertsPortlet.NAME, GroupAlertsPortlet.KEY, 210);
            dashboard.addPortlet(alerts, colRight, rowRight++);
            groupKeyNameMap.remove(GroupAlertsPortlet.KEY);
        }
        if (groupKeyNameMap.containsKey(GroupEventsPortlet.KEY)) {//events if available
            DashboardPortlet events = new DashboardPortlet(GroupEventsPortlet.NAME, GroupEventsPortlet.KEY, 210);
            dashboard.addPortlet(events, colRight, rowRight++);
            groupKeyNameMap.remove(GroupEventsPortlet.KEY);
        }
        if (groupKeyNameMap.containsKey(GroupOperationsPortlet.KEY)) {//operations if available
            DashboardPortlet ops = new DashboardPortlet(GroupOperationsPortlet.NAME, GroupOperationsPortlet.KEY, 210);
            dashboard.addPortlet(ops, colRight, rowRight++);
            groupKeyNameMap.remove(GroupOperationsPortlet.KEY);
        }
        if (groupKeyNameMap.containsKey(GroupConfigurationUpdatesPortlet.KEY)) {//operations if available
            DashboardPortlet ops = new DashboardPortlet(GroupConfigurationUpdatesPortlet.NAME,
                GroupConfigurationUpdatesPortlet.KEY, 210);
            dashboard.addPortlet(ops, colRight, rowRight++);
            groupKeyNameMap.remove(GroupConfigurationUpdatesPortlet.KEY);
        }

        //Fill out left column(typically smaller portlets) then alternate cols with remaining
        displayLeft = false;
        updateDashboardWithPortlets(groupKeyNameMap, dashboard, 105);
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

    @Override
    public boolean isValidDashboardName(String name) {
        return ((name != null) && (dashboardView != null) && name.equals(dashboardView.getDashboard().getName()));
    }

    @Override
    public void refresh() {
        if (isInitialized()) {
            dashboardView.rebuild();
        }
    }

}
