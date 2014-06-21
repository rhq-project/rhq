/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.client.dashboard;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.ColorSelectedEvent;
import com.smartgwt.client.widgets.form.events.ColorSelectedHandler;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.form.ColorButtonItem;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupBundleDeploymentsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupConfigurationUpdatesPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupOobsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupOperationsPortlet;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupPkgHistoryPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceBundleDeploymentsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceConfigurationUpdatesPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceEventsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceMetricsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceOobsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourceOperationsPortlet;
import org.rhq.coregui.client.dashboard.portlets.resource.ResourcePkgHistoryPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 * @author Simeon Pinder
 */
public class DashboardView extends EnhancedVLayout {

    private DashboardContainer dashboardContainer;
    private Dashboard storedDashboard;

    private boolean editMode = false;

    PortalLayout portalLayout;
    DynamicForm editForm;
    IMenuButton addPortlet;

    HashSet<PortletWindow> portletWindows = new HashSet<PortletWindow>();

    private static String STOP = MSG.view_dashboards_portlets_refresh_none();
    private static String REFRESH1 = MSG.view_dashboards_portlets_refresh_one_min();
    private static String REFRESH5 = MSG.view_dashboards_portlets_refresh_multiple_min(String.valueOf(5));
    private static String REFRESH10 = MSG.view_dashboards_portlets_refresh_multiple_min(String.valueOf(10));
    private static Integer STOP_VALUE = 0;
    private static Integer REFRESH1_VALUE = 1 * 60000;
    private static Integer REFRESH5_VALUE = 5 * 60000;
    private static Integer REFRESH10_VALUE = 10 * 60000;

    private HashMap<Integer, String> refreshMenuMappings;
    private MenuItem[] refreshMenuItems;
    private int refreshInterval = 0;
    private IMenuButton refreshMenuButton;

    private EntityContext context;
    private ResourceGroupComposite groupComposite = null;
    private ResourceComposite resourceComposite = null;

    // this is used to prevent an odd smartgwt problem where onInit() can get called multiple times if
    // the view is set to a Tab's pane.
    private boolean isInitialized = false;

    private PortletWindow maximizedPortlet = null;

    /**
     * Convenience constructor for subsystem context.
     *
     * @param dashboardContainer
     * @param storedDashboard
     */
    public DashboardView(DashboardContainer dashboardContainer, Dashboard storedDashboard) {

        this(dashboardContainer, storedDashboard, EntityContext.forSubsystemView(), null);
    }

    /**
     * @param dashboardContainer
     * @param storedDashboard
     * @param context
     * @param composite ResourceComposite, ResourceGroupComposite or null depending on context
     */
    public DashboardView(DashboardContainer dashboardContainer, Dashboard storedDashboard, EntityContext context,
        Object composite) {

        super();

        this.dashboardContainer = dashboardContainer;
        this.storedDashboard = storedDashboard;
        // This is a workaround for the fact that RHQ 4.1 and earlier wrongly allowed the user to save a dashboard with
        // 0 columns.
        if (this.storedDashboard.getColumns() == 0) {
            this.storedDashboard.setColumns(1);
        }
        this.context = context;

        switch (this.context.getType()) {
        case Resource: {
            if (null == composite) {
                throw new IllegalArgumentException("null composite for resource context");
            }
            this.resourceComposite = (ResourceComposite) composite;
            break;
        }
        case ResourceGroup: {
            if (null == composite) {
                throw new IllegalArgumentException("null composite for group context");
            }
            this.groupComposite = (ResourceGroupComposite) composite;
            break;
        }
        }
    }

    @Override
    protected void onInit() {
        if (!isInitialized) {
            super.onInit();

            this.setWidth100();
            this.setHeight100();

            this.addMember(buildEditForm());
            buildPortlets();

            isInitialized = true;
        }
    }

    public void rebuild() {
        // destroy all of the portlets and recreate from scratch
        portalLayout.removeFromParent();
        portalLayout.destroy();
        portalLayout = null;

        portletWindows.clear();

        buildPortlets();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setEditMode(editMode);
    }

    public void buildPortlets() {
        this.setBackgroundColor(storedDashboard.getConfiguration().getSimpleValue(Dashboard.CFG_BACKGROUND, "transparent"));

        portalLayout = new PortalLayout(this, storedDashboard.getColumns(), storedDashboard.getColumnWidths());

        portalLayout.setOverflow(Overflow.AUTO);
        portalLayout.setWidth100();
        portalLayout.setHeight100();

        loadPortletWindows();

        addMember(portalLayout);
    }

    protected boolean canEditName() {
        return true;
    }

    private DynamicForm buildEditForm() {
        editForm = new DynamicForm();
        editForm.setMargin(5);
        editForm.setAutoWidth();
        editForm.setNumCols(canEditName() ? 12 : 10);

        TextItem nameItem = null;
        if (dashboardContainer.supportsDashboardNameEdit()) {
            nameItem = new TextItem("name", MSG.common_title_dashboard_name());
            nameItem.setValue(storedDashboard.getName());
            nameItem.setLength(200);
            nameItem.setWrapTitle(false);
            nameItem.addBlurHandler(new BlurHandler() {
                public void onBlur(BlurEvent blurEvent) {
                    FormItem nameItem = blurEvent.getItem();
                    String name = (String) nameItem.getValue();
                    String trimmedName = (name == null) ? "" : name.trim();
                    if (dashboardContainer.isValidDashboardName(trimmedName)) {
                        storedDashboard.setName(trimmedName);
                        save();
                        dashboardContainer.updateDashboardNames();
                    } else {
                        // TODO: i18n
                        Message message = new Message("There is already a dashboard named '" + trimmedName
                            + "'. Please specify a name that is not already in use.", Message.Severity.Error, EnumSet
                            .of(Message.Option.Transient));
                        CoreGUI.getMessageCenter().notify(message);
                        nameItem.setValue(storedDashboard.getName());
                    }
                }
            });
        }

        final StaticTextItem numColItem = new StaticTextItem();
        numColItem.setTitle(MSG.common_title_columns());
        numColItem.setValue(storedDashboard.getColumns());
        numColItem.setWrapTitle(false);

        ButtonItem addColumn = new ButtonItem("addColumn", MSG.common_title_add_column());
        addColumn.setAutoFit(true);
        addColumn.setStartRow(false);
        addColumn.setEndRow(false);

        final ButtonItem removeColumn = new ButtonItem("removeColumn", MSG.common_title_remove_column());
        removeColumn.setAutoFit(true);
        removeColumn.setStartRow(false);
        removeColumn.setEndRow(false);
        removeColumn.setDisabled(storedDashboard.getColumns() == 1);

        addColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                portalLayout.addMember(new PortalColumn());
                numColItem.setValue(storedDashboard.getColumns() + 1);
                storedDashboard.setColumns(storedDashboard.getColumns() + 1);
                removeColumn.setDisabled(storedDashboard.getColumns() == 1);
                save();
            }
        });

        removeColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {

                Canvas[] columns = portalLayout.getMembers();
                int numColumns = columns.length;
                if (numColumns > 0) {
                    PortalColumn lastColumn = (PortalColumn) columns[numColumns - 1];
                    for (Canvas portletWindow : lastColumn.getMembers()) {
                        storedDashboard.removePortlet(((PortletWindow) portletWindow).getStoredPortlet());
                    }
                    portalLayout.removeMember(lastColumn);
                    numColItem.setValue(numColumns - 1);
                    storedDashboard.setColumns(storedDashboard.getColumns() - 1);
                    removeColumn.setDisabled(storedDashboard.getColumns() == 1);
                    save();
                }
            }
        });

        // build the menu of valid portlets for this context, sorted by portlet name
        final Menu addPortletMenu = new Menu();
        LinkedHashMap<String, String> valueMap;

        switch (context.getType()) {
        case SubsystemView:
            valueMap = PortletFactory.getGlobalPortletMenuMap();
            break;

        case ResourceGroup:
            valueMap = processPortletNameMapForGroup(this.groupComposite);
            // In addition to the group-specific portlets, make the global portlets available
            valueMap.putAll(PortletFactory.getGlobalPortletMenuMap());
            break;

        case Resource:
            valueMap = processPortletNameMapForResource(this.resourceComposite);
            // In addition to the resource-specific portlets, make the global portlets available
            valueMap.putAll(PortletFactory.getGlobalPortletMenuMap());
            break;

        default:
            throw new IllegalStateException("Unsupported context [" + context + "]");
        }
        for (Iterator<String> i = valueMap.keySet().iterator(); i.hasNext();) {
            String portletKey = i.next();
            String portletName = valueMap.get(portletKey);
            MenuItem menuItem = new MenuItem(portletName);
            menuItem.setAttribute("portletKey", portletKey);
            addPortletMenu.addItem(menuItem);
        }

        addPortlet = new IMenuButton(MSG.common_title_add_portlet(), addPortletMenu);
        addPortlet.setIcon("[skin]/images/actions/add.png");
        addPortlet.setAutoFit(true);

        addPortletMenu.addItemClickHandler(new ItemClickHandler() {
            public void onItemClick(ItemClickEvent itemClickEvent) {
                String key = itemClickEvent.getItem().getAttribute("portletKey");
                String name = itemClickEvent.getItem().getTitle();
                try {
                    addPortlet(key, name);
                } catch (Exception ex) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error3(), ex);
                }
            }
        });

        CanvasItem addCanvas = new CanvasItem();
        addCanvas.setShowTitle(false);
        addCanvas.setCanvas(addPortlet);
        addCanvas.setStartRow(false);
        addCanvas.setEndRow(false);

        ColorButtonItem picker = new ColorButtonItem("colorButton", MSG.common_title_background());
        picker.setStartRow(false);
        picker.setEndRow(false);
        picker.setCurrentColor(storedDashboard.getConfiguration().getSimpleValue(Dashboard.CFG_BACKGROUND, "transparent"));
        picker.setColorSelectedHandler(new ColorSelectedHandler() {
            @Override
            public void onColorSelected(ColorSelectedEvent event) {
                String selectedColor = event.getColor();
                if (selectedColor != null) {
                    setBackgroundColor(selectedColor);
                    storedDashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, selectedColor));
                    save();
                }
            }
        });

        //refresh interval
        Menu refreshMenu = new Menu();
        refreshMenu.setShowShadow(true);
        refreshMenu.setShadowDepth(10);
        refreshMenu.setAutoWidth();
        refreshMenu.setHeight(15);
        ClickHandler menuClick = new ClickHandler() {
            @Override
            public void onClick(MenuItemClickEvent event) {
                String selection = event.getItem().getTitle();
                refreshInterval = 0;
                if (selection != null) {
                    if (selection.equals(STOP)) {
                        refreshInterval = STOP_VALUE;
                    } else if (selection.equals(REFRESH1)) {
                        refreshInterval = REFRESH1_VALUE;
                    } else if (selection.equals(REFRESH5)) {
                        refreshInterval = REFRESH5_VALUE;
                    } else if (selection.equals(REFRESH10)) {
                        refreshInterval = REFRESH10_VALUE;
                    } else {//unable to locate value disable refresh
                        refreshInterval = STOP_VALUE;//
                    }
                    UserSessionManager.getUserPreferences().setPageRefreshInterval(refreshInterval,
                        new UpdatePortletRefreshCallback());
                }
            }
        };

        String[] refreshIntervals = { STOP, REFRESH1, REFRESH5, REFRESH10 };
        Integer[] refreshValues = { STOP_VALUE, REFRESH1_VALUE, REFRESH5_VALUE, REFRESH10_VALUE };
        refreshMenuMappings = new HashMap<Integer, String>();
        refreshMenuItems = new MenuItem[refreshIntervals.length];
        int retrievedRefreshInterval = REFRESH1_VALUE;
        if (null != UserSessionManager.getUserPreferences()) {
            retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
        }
        for (int i = 0; i < refreshIntervals.length; i++) {
            MenuItem item = new MenuItem(refreshIntervals[i], "");
            item.addClickHandler(menuClick);
            refreshMenuMappings.put(refreshValues[i], refreshIntervals[i]);
            if (retrievedRefreshInterval == refreshValues[i]) {
                item.setIcon(ImageManager.getAvailabilityIcon(true));
            }
            refreshMenuItems[i] = item;
        }

        refreshMenu.setItems(refreshMenuItems);
        refreshMenuButton = new IMenuButton(MSG.common_title_refreshInterval(), refreshMenu);
        refreshMenu.setAutoHeight();
        refreshMenuButton.getMenu().setItems(refreshMenuItems);
        refreshMenuButton.setWidth(140);
        refreshMenuButton.setShowTitle(true);
        refreshMenuButton.setTop(0);
        refreshMenuButton.setIconOrientation("left");

        CanvasItem refreshCanvas = new CanvasItem();
        refreshCanvas.setShowTitle(false);
        refreshCanvas.setCanvas(refreshMenuButton);
        refreshCanvas.setStartRow(false);
        refreshCanvas.setEndRow(false);

        if (null != nameItem) {
            editForm.setItems(nameItem, addCanvas, numColItem, addColumn, removeColumn, picker, refreshCanvas);
        } else {
            editForm.setItems(addCanvas, numColItem, addColumn, removeColumn, picker, refreshCanvas);
        }

        updateRefreshMenu();
        this.refreshMenuButton.markForRedraw();

        markForRedraw();

        //attempt to initialize
        editForm.markForRedraw();
        markForRedraw();

        return editForm;
    }

    /** Return the relevant sorted value map for the resource
     */
    public static LinkedHashMap<String, String> processPortletNameMapForResource(ResourceComposite composite) {

        LinkedHashMap<String, String> resourceMenuMap = PortletFactory.getResourcePortletMenuMap();

        if ((composite != null) && (composite.getResource() != null)) {
            resourceMenuMap = new LinkedHashMap<String, String>(resourceMenuMap);

            // filter out portlets not relevent for facets
            Set<ResourceTypeFacet> facets = composite.getResourceFacets().getFacets();
            if (!facets.isEmpty()) {
                // Operation related portlets
                if (!facets.contains(ResourceTypeFacet.OPERATION)) {
                    resourceMenuMap.remove(ResourceOperationsPortlet.KEY);
                }
                // MEASUREMENT related portlets(METRICS)
                if (!facets.contains(ResourceTypeFacet.MEASUREMENT)) {
                    resourceMenuMap.remove(ResourceMetricsPortlet.KEY);
                    resourceMenuMap.remove(ResourceOobsPortlet.KEY);
                }
                // Content related portlets
                if (!facets.contains(ResourceTypeFacet.CONTENT)) {
                    resourceMenuMap.remove(ResourcePkgHistoryPortlet.KEY);
                }
                // Event related portlets
                if (!facets.contains(ResourceTypeFacet.EVENT)) {
                    resourceMenuMap.remove(ResourceEventsPortlet.KEY);
                }
                // Configuration related portlets
                if (!facets.contains(ResourceTypeFacet.CONFIGURATION)) {
                    resourceMenuMap.remove(ResourceConfigurationUpdatesPortlet.KEY);
                }
                // Bundle related portlets
                if (!facets.contains(ResourceTypeFacet.BUNDLE)) {
                    resourceMenuMap.remove(ResourceBundleDeploymentsPortlet.KEY);
                }
            }
        }

        return resourceMenuMap;
    }

    /** Return the relevant sorted value map for the group
     */
    public static LinkedHashMap<String, String> processPortletNameMapForGroup(ResourceGroupComposite composite) {

        LinkedHashMap<String, String> groupMenuMap = PortletFactory.getGroupPortletMenuMap();

        if ((composite != null) && (composite.getResourceGroup() != null)) {
            groupMenuMap = new LinkedHashMap<String, String>(groupMenuMap);

            // filter out portlets not relevent for facets
            Set<ResourceTypeFacet> facets = composite.getResourceFacets().getFacets();
            GroupCategory groupCategory = composite.getResourceGroup().getGroupCategory();

            // if not a compatible group do some pruning.
            if (groupCategory != GroupCategory.COMPATIBLE) {
                groupMenuMap.remove(GroupOperationsPortlet.KEY);
                groupMenuMap.remove(GroupMetricsPortlet.KEY);
                groupMenuMap.remove(GroupOobsPortlet.KEY);
                groupMenuMap.remove(GroupPkgHistoryPortlet.KEY);
                groupMenuMap.remove(GroupConfigurationUpdatesPortlet.KEY);
                groupMenuMap.remove(GroupBundleDeploymentsPortlet.KEY);

            } else {
                // for compatible may still need to do some pruning.
                if (!facets.isEmpty()) {
                    // Operations related portlets(Config,PkgHistory)
                    if (!facets.contains(ResourceTypeFacet.OPERATION)) {
                        groupMenuMap.remove(GroupOperationsPortlet.KEY);
                    }
                    // MEASUREMENT related portlets(METRICS)
                    if (!facets.contains(ResourceTypeFacet.MEASUREMENT)) {
                        groupMenuMap.remove(GroupMetricsPortlet.KEY);
                        groupMenuMap.remove(GroupOobsPortlet.KEY);
                    }
                    // CONTENT related portlets(CONTENT)
                    if (!facets.contains(ResourceTypeFacet.CONTENT)) {
                        groupMenuMap.remove(GroupPkgHistoryPortlet.KEY);
                    }
                    // CONFIGURATION related portlets(CONFIGURATION)
                    if (!facets.contains(ResourceTypeFacet.CONFIGURATION)) {
                        groupMenuMap.remove(GroupConfigurationUpdatesPortlet.KEY);
                    }
                    // BUNDLE related portlets(BUNDLE)
                    if (!facets.contains(ResourceTypeFacet.BUNDLE)) {
                        groupMenuMap.remove(GroupBundleDeploymentsPortlet.KEY);
                    }
                }
            }
        }

        return groupMenuMap;
    }

    private void loadPortletWindows() {
        for (int i = 0; i < storedDashboard.getColumns(); i++) {
            for (DashboardPortlet storedPortlet : storedDashboard.getPortlets(i)) {
                try {
                    PortletWindow portletWindow = new PortletWindow(this, storedPortlet, context);
                    portletWindow.setTitle(storedPortlet.getName());
                    portletWindow.setHeight(storedPortlet.getHeight());
                    portletWindow.setVisible(true);

                    portletWindows.add(portletWindow);
                    portalLayout.addPortletWindow(portletWindow, i);
                } catch (Exception ex) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error2(), ex);
                    continue;
                }
            }
        }
    }

    protected void addPortlet(String portletKey, String portletName) {
        DashboardPortlet storedPortlet = new DashboardPortlet(portletName, portletKey, 250);
        storedDashboard.addPortlet(storedPortlet);

        final PortletWindow newPortletWindow = new PortletWindow(this, storedPortlet, context);
        newPortletWindow.setTitle(portletName);
        newPortletWindow.setHeight(350);
        newPortletWindow.setVisible(false);

        portletWindows.add(newPortletWindow);
        portalLayout.addPortletWindow(newPortletWindow, storedPortlet.getColumn());
        PortalColumn portalColumn = portalLayout.getPortalColumn(storedPortlet.getColumn());

        // also insert a blank spacer element, which will trigger the built-in
        //  animateMembers layout animation
        final LayoutSpacer placeHolder = new LayoutSpacer();
        //        placeHolder.setRect(newPortlet.getRect());
        portalColumn.addMember(placeHolder); // add to top

        // create an outline around the clicked button
        final Canvas outline = new Canvas();
        outline.setLeft(editForm.getAbsoluteLeft() + addPortlet.getLeft());
        outline.setTop(editForm.getAbsoluteTop());
        outline.setWidth(addPortlet.getWidth());
        outline.setHeight(addPortlet.getHeight());
        outline.setBorder("2px solid 8289A6");
        outline.draw();
        outline.bringToFront();

        outline.animateRect(newPortletWindow.getPageLeft(), newPortletWindow.getPageTop(),
            newPortletWindow.getVisibleWidth(), newPortletWindow.getViewportHeight(), new AnimationCallback() {
                public void execute(boolean earlyFinish) {
                    // callback at end of animation - destroy placeholder and outline; show the new portlet
                    placeHolder.destroy();
                    outline.destroy();
                    newPortletWindow.show();
                }
            }, 750);
        save();
    }

    public void removePortlet(PortletWindow portletWindow) {
        storedDashboard.removePortlet(portletWindow.getStoredPortlet());
        this.portletWindows.remove(portletWindow);

        save();
    }

    public void save(Dashboard dashboard) {
        if (null != dashboard) {
            storedDashboard = dashboard;
            save();
        }
    }

    public void save() {
        save((AsyncCallback<Dashboard>) null);
    }

    public String[] updatePortalColumnWidths() {
        int numColumns = storedDashboard.getColumns();
        int totalPixelWidth = 0;
        int[] columnPixelWidths = new int[numColumns];
        for (int i = 0; i < numColumns; ++i) {
            PortalColumn col = portalLayout.getPortalColumn(i);
            totalPixelWidth += col.getWidth();
            columnPixelWidths[i] = col.getWidth();
        }
        String[] columnWidths = new String[numColumns];
        columnWidths[numColumns - 1] = "*";
        for (int i = 0; i < numColumns - 1; ++i) {
            columnWidths[i] = String.valueOf((columnPixelWidths[i] * 100 / totalPixelWidth)) + "%";
        }

        storedDashboard.setColumnWidths(columnWidths);

        return columnWidths;
    }

    public void save(final AsyncCallback<Dashboard> callback) {
        // a variety of edits (dragResize, add/remove column, etc) can cause column width changes. Update them
        // prior to every save.
        updatePortalColumnWidths();

        // since we reset storedDashboard after the async update completes, block modification of the dashboard
        // during that interval.
        DashboardView.this.disable();

        GWTServiceLookup.getDashboardService().storeDashboard(storedDashboard, new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardManager_error(), caught);
                DashboardView.this.enable();

                if (null != callback) {
                    callback.onFailure(caught);
                }
            }

            public void onSuccess(Dashboard result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_dashboardManager_saved(result.getName()), Message.Severity.Info));

                // The portlet definitions have been merged and updated, reset the portlet windows with the
                // up to date portlets.
                updatePortletWindows(result);
                storedDashboard = result;

                if (null != callback) {
                    callback.onSuccess(result);
                }

                DashboardView.this.enable();
            }
        });
    }

    private void updatePortletWindows(Dashboard result) {
        if (result != null) {
            for (PortletWindow portletWindow : portletWindows) {
                for (DashboardPortlet updatedPortlet : result.getPortlets()) {
                    if (equalsDashboardPortlet(portletWindow.getStoredPortlet(), updatedPortlet)) {
                        portletWindow.setStoredPortlet(updatedPortlet);

                        // restarting portlet auto-refresh with newest settings
                        Portlet view = portletWindow.getView();
                        if (view instanceof AutoRefreshPortlet) {
                            ((AutoRefreshPortlet) view).startRefreshCycle();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * This is an enhanced equals for portlets that allows equality for unpersisted portlets. At times (like addPortlet)
     * a portlet may have been associated with its window prior to being persisted. In this case we can consider
     * it equal if it is associated with the same dashboard(1) and has the same positioning. Note that key-name pairing
     * can not be used for equality as a dashboard is allowed to have the same portlet multiple times, with a default
     * name.  But they can not hold the same position.
     * <pre>
     *   (1) Even the dashboard comparison has been made flexible. To allow for lazy persist of the dashboard (to
     *       allow for the default group or resource dashboard to not be persisted) we allow the dash comparison
     *       to be done by name if an entity id is 0.  This should be safe as dashboard names are set prior to
     *       persist, and should be unique for the session user.
     *
     * @param storedPortlet
     * @param updatedPortlet
     * @return
     */
    private boolean equalsDashboardPortlet(DashboardPortlet storedPortlet, DashboardPortlet updatedPortlet) {

        if (storedPortlet.equals(updatedPortlet)) {
            return true;
        }

        // make sure at least one portlet is not persisted for pseudo-equality
        if (storedPortlet.getId() > 0 && updatedPortlet.getId() > 0) {
            return false;
        }

        // must match position for pseudo-equality
        if (storedPortlet.getColumn() != updatedPortlet.getColumn()) {
            return false;
        }

        if (storedPortlet.getIndex() != updatedPortlet.getIndex()) {
            return false;
        }

        // must match dash (ids if persisted, otherwise name) for pseudo-equality
        boolean unpersistedDash = (storedPortlet.getDashboard().getId() == 0 || updatedPortlet.getDashboard().getId() == 0);
        boolean dashMatchId = (!unpersistedDash && (storedPortlet.getDashboard().getId() == updatedPortlet
            .getDashboard().getId()));
        boolean dashMatchName = (unpersistedDash && storedPortlet.getDashboard().getName()
            .equals(updatedPortlet.getDashboard().getName()));
        if (!(dashMatchId || dashMatchName)) {
            return false;
        }

        return true;
    }

    public void delete() {
        if (null != this.storedDashboard && this.storedDashboard.getId() > 0) {
            GWTServiceLookup.getDashboardService().removeDashboard(this.storedDashboard.getId(),
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dashboardManager_deleteFail(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_dashboardManager_deleted(storedDashboard.getName()),
                                Message.Severity.Info));
                    }
                });
        }
    }

    public void resize() {
        portalLayout.resize();
    }

    public Dashboard getDashboard() {
        return storedDashboard;
    }

    public Set<Permission> getGlobalPermissions() {
        return dashboardContainer.getGlobalPermissions();
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;

        // don't allow resizing in min/max in edit mode
        for (PortletWindow portletWindow : portletWindows) {
            portletWindow.hideSizingHeaderControls(editMode);
        }

        if (editMode) {
            this.editForm.show();
            //
        } else {
            this.editForm.hide();
        }
        this.editForm.markForRedraw();
        this.portalLayout.show();
        redraw();
        this.portalLayout.markForRedraw();
    }

    public class UpdatePortletRefreshCallback implements AsyncCallback<Subject> {
        public void onSuccess(Subject subject) {
            String m;
            if (refreshInterval > 0) {
                m = MSG.view_dashboards_portlets_refresh_success1();
            } else {
                m = MSG.view_dashboards_portlets_refresh_success2();
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Info));
            updateRefreshMenu();
            save();
        }

        public void onFailure(Throwable throwable) {
            String m;
            if (refreshInterval > 0) {
                m = MSG.view_dashboards_portlets_refresh_fail1();
            } else {
                m = MSG.view_dashboards_portlets_refresh_fail2();
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Error));
            // Revert back to our original favorite status, since the server update failed.
            updateRefreshMenu();
        }
    }

    public void updateRefreshMenu() {
        if (refreshMenuItems != null) {
            int retrievedRefreshInterval = REFRESH1_VALUE;
            if (null != UserSessionManager.getUserPreferences()) {
                retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
            }
            String currentSelection = refreshMenuMappings.get(retrievedRefreshInterval);
            if (currentSelection != null) {
                //iterate over menu items and update icon details
                for (int i = 0; i < refreshMenuItems.length; i++) {
                    MenuItem menu = refreshMenuItems[i];
                    if (currentSelection.equals(menu.getTitle())) {
                        menu.setIcon(ImageManager.getAvailabilityIcon(true));
                    } else {
                        menu.setIcon("");
                    }
                    refreshMenuItems[i] = menu;
                }
                //update the menu
                refreshMenuButton.getMenu().setItems(refreshMenuItems);
            }
        }
        if (this.refreshMenuButton != null) {
            this.refreshMenuButton.markForRedraw();
        }
    }

    public Dashboard getStoredDashboard() {
        return storedDashboard;
    }

    public EntityContext getContext() {
        return context;
    }

    public ResourceGroupComposite getGroupComposite() {
        return groupComposite;
    }

    public ResourceComposite getResourceComposite() {
        return resourceComposite;
    }

    public boolean isMaximized() {
        return (maximizedPortlet != null);
    }

    public PortletWindow getMaximizePortlet() {
        return maximizedPortlet;
    }

    public void maximizePortlet(PortletWindow portletWindow) {
        if (isMaximized()) {
            return;
        }

        maximizedPortlet = portletWindow;

        int numColumns = storedDashboard.getColumns();
        for (int i = 0; i < numColumns; ++i) {
            PortalColumn col = portalLayout.getPortalColumn(i);
            Canvas portlet = col.getMember(portletWindow.getID());
            if (null == portlet) {
                col.hide();
            } else {
                for (Canvas member : col.getMembers()) {
                    if (!member.equals(portlet)) {
                        member.hide();
                    } else {
                        ((PortletWindow) member).hideSizingHeaderControls(true);
                        member.setHeight100();
                    }
                }
            }
        }

        portalLayout.markForRedraw();
    }

    public void restorePortlet() {
        if (!isMaximized()) {
            return;
        }

        int numColumns = storedDashboard.getColumns();
        for (int i = 0; i < numColumns; ++i) {
            PortalColumn col = portalLayout.getPortalColumn(i);
            if (!col.isVisible()) {
                col.show();
            } else {
                for (Canvas member : col.getMembers()) {
                    if (!member.isVisible()) {
                        member.show();
                    } else {
                        ((PortletWindow) member).hideSizingHeaderControls(false);
                        member.setHeight(maximizedPortlet.getStoredPortlet().getHeight());
                    }
                }
            }
        }

        maximizedPortlet = null;
        portalLayout.markForRedraw();
    }

    // If we redraw the dashboard then also redraw the portlets so that everything is up to date
    @Override
    public void redraw() {
        super.redraw();

        for (PortletWindow pw : portletWindows) {
            // I think this should work with markForRedraw but for some reason it does not
            try {
                ((Canvas) pw.getView()).redraw();
            } catch (Exception ex) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardsManager_error4(), ex);
            }
        }
    }
}
