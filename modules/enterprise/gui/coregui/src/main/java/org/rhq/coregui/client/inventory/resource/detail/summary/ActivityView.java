package org.rhq.coregui.client.inventory.resource.detail.summary;

import java.util.HashMap;
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
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.InitializableView;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.dashboard.DashboardContainer;
import org.rhq.coregui.client.dashboard.DashboardView;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceAlertsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceConfigurationUpdatesPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceEventsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceMetricsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceOperationsPortlet;
import org.rhq.coregui.client.gwt.DashboardGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * The content pane for the resource Summary>Activity subtab.
 *
 * @author Jay Shaughnessy
 */
public class ActivityView extends EnhancedVLayout implements DashboardContainer, InitializableView {

    private static final String DASHBOARD_NAME_PREFIX = "ResourceDashboard_";

    private ResourceComposite resourceComposite;

    private DashboardGWTServiceAsync dashboardService = GWTServiceLookup.getDashboardService();

    private DashboardView dashboardView;

    private EnhancedToolStrip footer;
    private IButton editButton;
    private IButton resetButton;

    // Capture the user's global permissions for use by any dashboard or portlet that may need it for rendering.
    private Set<Permission> globalPermissions;

    private boolean editMode = false;

    private boolean isInitialized = false;

    public ActivityView(ResourceComposite resourceComposite) {
        super();
        this.resourceComposite = resourceComposite;
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
                    criteria.addFilterCategory(DashboardCategory.RESOURCE);
                    criteria.addFilterResourceId(resourceComposite.getResource().getId());
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
        //pass in the resource information
        dashboardView = new DashboardView(this, dashboard, EntityContext.forResource(resourceComposite.getResource()
            .getId()), resourceComposite);
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
        Resource resource = resourceComposite.getResource();

        Dashboard dashboard = new Dashboard();

        dashboard.setName(DASHBOARD_NAME_PREFIX + sessionSubject.getId() + "_" + resource.getId());
        dashboard.setCategory(DashboardCategory.RESOURCE);
        dashboard.setResource(resource);
        dashboard.setColumns(2);

        // TODO, add real portlets
        // set leftmost column and let the rest be equally divided
        dashboard.setColumnWidths("40%");
        dashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, "#F1F2F3"));

        //figure out which portlets to display and how
        HashMap<String, String> resKeyNameMap = DashboardView.processPortletNameMapForResource(resourceComposite);
        int colLeft = 0;
        int colRight = 1;
        int rowLeft = 0;
        int rowRight = 0;
        //Left Column
        if (resKeyNameMap.containsKey(ResourceMetricsPortlet.KEY)) {//measurments top left if available
            DashboardPortlet measurements = new DashboardPortlet(ResourceMetricsPortlet.NAME,
                ResourceMetricsPortlet.KEY, 220);
            dashboard.addPortlet(measurements, colLeft, rowLeft++);
            resKeyNameMap.remove(ResourceMetricsPortlet.KEY);
        }

        // right Column(approx 60%. As larger more room to display table and N rows.)
        if (resKeyNameMap.containsKey(ResourceAlertsPortlet.KEY)) {//alerts top right if available
            DashboardPortlet alerts = new DashboardPortlet(ResourceAlertsPortlet.NAME, ResourceAlertsPortlet.KEY, 210);
            dashboard.addPortlet(alerts, colRight, rowRight++);
            resKeyNameMap.remove(ResourceAlertsPortlet.KEY);
        }
        if (resKeyNameMap.containsKey(ResourceEventsPortlet.KEY)) {//events if available
            DashboardPortlet events = new DashboardPortlet(ResourceEventsPortlet.NAME, ResourceEventsPortlet.KEY, 210);
            dashboard.addPortlet(events, colRight, rowRight++);
            resKeyNameMap.remove(ResourceEventsPortlet.KEY);
        }
        if (resKeyNameMap.containsKey(ResourceOperationsPortlet.KEY)) {//operations if available
            DashboardPortlet ops = new DashboardPortlet(ResourceOperationsPortlet.NAME, ResourceOperationsPortlet.KEY,
                210);
            dashboard.addPortlet(ops, colRight, rowRight++);
            resKeyNameMap.remove(ResourceOperationsPortlet.KEY);
        }
        if (resKeyNameMap.containsKey(ResourceConfigurationUpdatesPortlet.KEY)) {//configuration if available
            DashboardPortlet ops = new DashboardPortlet(ResourceConfigurationUpdatesPortlet.NAME,
                ResourceConfigurationUpdatesPortlet.KEY, 210);
            dashboard.addPortlet(ops, colRight, rowRight++);
            resKeyNameMap.remove(ResourceConfigurationUpdatesPortlet.KEY);
        }

        //Fill out left column(typically smaller portlets) then alternate cols with remaining
        boolean displayLeft = false;
        for (String key : resKeyNameMap.keySet()) {
            DashboardPortlet portlet = new DashboardPortlet(resKeyNameMap.get(key), key, 105);
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

        return dashboard;
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
