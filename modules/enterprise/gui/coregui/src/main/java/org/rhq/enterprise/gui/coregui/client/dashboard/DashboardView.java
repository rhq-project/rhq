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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.ColorPickerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Simeon Pinder
 */
public class DashboardView extends LocatableVLayout {
    private DashboardsView dashboardsView;
    private Dashboard storedDashboard;

    boolean editMode = false;

    PortalLayout portalLayout;
    LocatableDynamicForm editForm;
    IMenuButton addPortlet;

    Set<PortletWindow> portletWindows = new HashSet<PortletWindow>();
    private static String STOP = MSG.view_dashboards_portlets_refresh_none();
    private static String REFRESH1 = MSG.view_dashboards_portlets_refresh_one_min();
    private static String REFRESH5 = MSG.view_dashboards_portlets_refresh_multiple_min(String.valueOf(5));
    private static String REFRESH10 = MSG.view_dashboards_portlets_refresh_multiple_min(String.valueOf(10));
    private static Integer STOP_VALUE = 0;
    private static Integer REFRESH1_VALUE = 1 * Long.valueOf(MeasurementUtility.MINUTES).intValue();
    private static Integer REFRESH5_VALUE = 5 * Long.valueOf(MeasurementUtility.MINUTES).intValue();
    private static Integer REFRESH10_VALUE = 10 * Long.valueOf(MeasurementUtility.MINUTES).intValue();

    private HashMap<Integer, String> refreshMenuMappings;
    private MenuItem[] refreshMenuItems;
    private int refreshInterval = 0;
    private LocatableIMenuButton refreshMenuButton;
    private HashMap<String, PortletViewFactory> portletMap = null;

    // this is used to prevent an odd smartgwt problem where onInit() can get called multiple times if
    // the view is set to a Tab's pane.
    private boolean isInitialized = false;

    public DashboardView(String locatorId, DashboardsView dashboardsView, Dashboard storedDashboard) {
        super(locatorId);

        this.dashboardsView = dashboardsView;
        this.storedDashboard = storedDashboard;
        setOverflow(Overflow.AUTO);
        setPadding(5);
    }

    @Override
    protected void onInit() {

        if (!isInitialized) {
            super.onInit();
            addMember(buildEditForm());
            isInitialized = true;
        }
    }

    public void redraw() {
        // destroy all of the portlets and recreate from scratch
        portalLayout.removeFromParent();
        portalLayout.destroy();
        portalLayout = null;

        buildPortlets();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        buildPortlets();
    }

    public void buildPortlets() {
        setWidth100();
        setHeight100();

        setBackgroundColor(storedDashboard.getConfiguration().getSimpleValue(Dashboard.CFG_BACKGROUND, "white"));

        portalLayout = new PortalLayout(extendLocatorId("PortalLayout"), this, storedDashboard.getColumns());
        portalLayout.setWidth100();
        portalLayout.setHeight100();

        loadPortletWindows();

        addMember(portalLayout);
    }

    private DynamicForm buildEditForm() {
        editForm = new LocatableDynamicForm(extendLocatorId("Editor"));
        editForm.setAutoWidth();
        editForm.setNumCols(12);

        TextItem nameItem = new TextItem("name", MSG.common_title_dashboard_name());
        nameItem.setValue(storedDashboard.getName());
        nameItem.setWrapTitle(false);
        nameItem.addBlurHandler(new BlurHandler() {
            public void onBlur(BlurEvent blurEvent) {
                String val = (String) blurEvent.getItem().getValue();
                val = (null == val) ? "" : val.trim();
                if (!("".equals(val) || val.equals(storedDashboard.getName()))) {
                    storedDashboard.setName(val);
                    save();
                    dashboardsView.updateNames();
                }
            }
        });

        final StaticTextItem numColItem = new StaticTextItem();
        numColItem.setTitle(MSG.common_title_columns());
        numColItem.setValue(storedDashboard.getColumns());

        ButtonItem addColumn = new ButtonItem("addColumn", MSG.common_title_add_column());
        //        addColumn.setIcon("silk/application_side_expand.png");
        addColumn.setAutoFit(true);
        addColumn.setStartRow(false);
        addColumn.setEndRow(false);

        addColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                portalLayout.addMember(new PortalColumn());
                numColItem.setValue(storedDashboard.getColumns() + 1);
                storedDashboard.setColumns(storedDashboard.getColumns() + 1);
                save();
            }
        });

        ButtonItem removeColumn = new ButtonItem("removeColumn", MSG.common_title_remove_column());
        removeColumn.setAutoFit(true);
        removeColumn.setStartRow(false);
        removeColumn.setEndRow(false);

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
                    save();
                }
            }
        });

        Menu addPortletMenu = new LocatableMenu(editForm.extendLocatorId("PortletMenu"));
        HashMap<String, String> keyNameMap = PortletFactory.getRegisteredPortletNameMap();
        // the assumption here is that the portlet names are unique. we want a sorted menu here, so create a
        // sorted map from portlet name to portlet key and use that to generate the menu. It would be nice if you
        // could just call Menu.sort() but it's not supported (yet?).
        TreeMap<String, String> nameKeyMap = new TreeMap<String, String>();
        for (String portletKey : keyNameMap.keySet()) {
            nameKeyMap.put(keyNameMap.get(portletKey), portletKey);
        }
        // now use the reversed map for the menu generation
        for (String portletName : nameKeyMap.keySet()) {
            MenuItem menuItem = new MenuItem(portletName);
            menuItem.setAttribute("portletKey", nameKeyMap.get(portletName));
            addPortletMenu.addItem(menuItem);
        }
        addPortlet = new LocatableIMenuButton(editForm.extendLocatorId("AddPortlet"), MSG.common_title_add_portlet(),
            addPortletMenu);

        addPortlet.setIcon("[skin]/images/actions/add.png");
        addPortlet.setAutoFit(true);

        addPortletMenu.addItemClickHandler(new ItemClickHandler() {
            public void onItemClick(ItemClickEvent itemClickEvent) {
                String key = itemClickEvent.getItem().getAttribute("portletKey");
                String name = itemClickEvent.getItem().getTitle();
                addPortlet(key, name);
            }
        });

        CanvasItem addCanvas = new CanvasItem();
        addCanvas.setShowTitle(false);
        addCanvas.setCanvas(addPortlet);
        addCanvas.setStartRow(false);
        addCanvas.setEndRow(false);

        ColorPickerItem picker = new ColorPickerItem();
        picker.setTitle(MSG.common_title_background());
        picker.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                Object v = changedEvent.getValue();
                com.allen_sauer.gwt.log.client.Log.info("color changed to " + v);
                setBackgroundColor(String.valueOf(v));
                storedDashboard.getConfiguration().put(new PropertySimple(Dashboard.CFG_BACKGROUND, String.valueOf(v)));
                save();
            }
        });
        picker.setValue(storedDashboard.getConfiguration().getSimpleValue(Dashboard.CFG_BACKGROUND, "white"));

        //refresh interval
        LocatableMenu refreshMenu = new LocatableMenu(editForm.extendLocatorId("AutoRefreshMenu"));
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
        refreshMenuButton = new LocatableIMenuButton(editForm.extendLocatorId("AutoRefreshButton"), MSG
            .common_title_change_refresh_time(), refreshMenu);
        refreshMenu.setAutoHeight();
        refreshMenuButton.getMenu().setItems(refreshMenuItems);
        refreshMenuButton.setWidth(140);
        refreshMenuButton.setShowTitle(true);
        refreshMenuButton.setTop(0);
        refreshMenuButton.setIconOrientation("left");

        CanvasItem refreshCanvas = new CanvasItem();
        refreshCanvas.setTitle(MSG.common_title_portlet_auto_refresh());
        refreshCanvas.setWrapTitle(false);
        refreshCanvas.setCanvas(refreshMenuButton);
        refreshCanvas.setStartRow(false);
        refreshCanvas.setEndRow(false);

        editForm.setItems(nameItem, addCanvas, numColItem, addColumn, removeColumn, picker, refreshCanvas);
        updateRefreshMenu();
        this.refreshMenuButton.markForRedraw();
        markForRedraw();

        return editForm;
    }

    private void loadPortletWindows() {

        for (int i = 0; i < storedDashboard.getColumns(); i++) {
            for (DashboardPortlet storedPortlet : storedDashboard.getPortlets(i)) {
                String locatorId = getPortletLocatorId(portalLayout, storedPortlet);
                final PortletWindow portletWindow = new PortletWindow(locatorId, this, storedPortlet);
                portletWindows.add(portletWindow);
                portletWindow.setTitle(storedPortlet.getName());

                portletWindow.setHeight(storedPortlet.getHeight());
                portletWindow.setVisible(true);

                portalLayout.addPortletWindow(portletWindow, i);
            }
        }
    }

    /**
     * LocatorIds need to be repeatable and non-duplicated.  The natural key for a portlet is the Id but the Id
     * is not a good locatorId as it may change (it's a sequence generated id) on subsequent test runs.  A portlet has
     * an internal identifier (portletKey) and a name, but the key-name tuple is not guaranteed to be unique as
     * multiple instances of the same portlet type may be present on the same, or across multiple dashboards. There
     * is one tuple that is guaranteed unique and useful for a repeatable locator Id: DashBoard-Position.  This
     * means that the on a single dashboard each portlet has a unique column-columnIndex pair.  Although portlets
     * can move, and the positions can change at runtime, it's still valid for a locatorId because it is
     * unique and repeatable for test purposes. We also add the portletKey for an easier visual cue.
     * The portalLayout's locatorId already incorporates the dashboardName, so we need only extend it with the
     * positioning information. 
     * 
     * @param portalLayout
     * @param dashboardPortlet
     * @return The locatorId for the portlet. Form PortleyKey_DashboardId_Column_ColumnIndex
     */
    private String getPortletLocatorId(PortalLayout portalLayout, DashboardPortlet dashboardPortlet) {
        StringBuilder locatorId = new StringBuilder(dashboardPortlet.getPortletKey());
        locatorId.append("_");
        locatorId.append(dashboardPortlet.getColumn());
        locatorId.append("_");
        locatorId.append(dashboardPortlet.getIndex());

        return portalLayout.extendLocatorId(locatorId.toString());
    }

    private void addPortlet(String portletKey, String portletName) {
        DashboardPortlet storedPortlet = new DashboardPortlet(portletName, portletKey, 250);
        storedDashboard.addPortlet(storedPortlet);

        String locatorId = getPortletLocatorId(portalLayout, storedPortlet);
        final PortletWindow newPortletWindow = new PortletWindow(locatorId, this, storedPortlet);
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

        outline.animateRect(newPortletWindow.getPageLeft(), newPortletWindow.getPageTop(), newPortletWindow
            .getVisibleWidth(), newPortletWindow.getViewportHeight(), new AnimationCallback() {
            public void execute(boolean earlyFinish) {
                // callback at end of animation - destroy placeholder and outline; show the new portlet
                placeHolder.destroy();
                outline.destroy();
                newPortletWindow.show();
            }
        }, 750);
        save();
    }

    public void removePortlet(DashboardPortlet portlet) {
        storedDashboard.removePortlet(portlet);
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

    public void save(final AsyncCallback<Dashboard> callback) {
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
            if (portletMap == null) {
                portletMap = new HashMap<String, PortletViewFactory>();
                for (String key : PortletFactory.getRegisteredPortletKeys()) {
                    portletMap.put(key, PortletFactory.getRegisteredPortletFactory(key));
                }
            }
            for (PortletWindow portletWindow : portletWindows) {
                for (DashboardPortlet portlet : result.getPortlets()) {
                    if (equalsDashboardPortlet(portletWindow.getStoredPortlet(), portlet)) {
                        portletWindow.setStoredPortlet(portlet);

                        //restarting portlet auto-refresh with newest settings
                        Portlet view = portletWindow.getView();
                        if (view instanceof AutoRefreshPortlet) {
                            ((AutoRefreshPortlet) view).startRefreshCycle();
                        }
                    }
                }
            }
        }
    }

    /**
     * This is an enhanced equals for portlets that allows equality for unpersisted portlets. At times (like addPortlet)
     * a portlet may have been associated with its window prior to being persisted. In this case we can consider
     * it equal if it is associated with the same dashboard and has the same positioning. Note that key-name pairing
     * can not be used for equality as a dashboard is allowed to have the same portlet multiple times, with a default
     * name.  But they can not hold the same position. 
     * 
     * @param portlet1
     * @param portlet2
     * @return
     */
    private boolean equalsDashboardPortlet(DashboardPortlet portlet1, DashboardPortlet portlet2) {

        if (portlet1.equals(portlet2)) {
            return true;
        }

        // make sure at least one portlet is not persisted
        if (portlet1.getId() > 0 && portlet2.getId() > 0) {
            return false;
        }

        // must match dash and position for psuedo-equality
        if (portlet1.getDashboard().getId() != portlet2.getDashboard().getId()) {
            return false;
        }

        if (portlet1.getColumn() != portlet2.getColumn()) {
            return false;
        }

        if (portlet1.getIndex() != portlet2.getIndex()) {
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
        return dashboardsView.getGlobalPermissions();
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {
            this.editForm.show();
        } else {
            this.editForm.hide();
        }
        markForRedraw();
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
            if (currentSelection != null) {//iterate over menu items and update icon details
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
}
