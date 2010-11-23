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

import java.util.HashSet;
import java.util.Set;

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
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class DashboardView extends LocatableVLayout {
    private DashboardsView dashboardsView;
    private Dashboard storedDashboard;

    boolean editMode = false;

    PortalLayout portalLayout;
    DynamicForm editForm;
    IMenuButton addPortlet;

    Set<PortletWindow> portlets = new HashSet<PortletWindow>();

    public DashboardView(String locatorId, DashboardsView dashboardsView, Dashboard storedDashboard) {
        super(locatorId);

        this.dashboardsView = dashboardsView;
        this.storedDashboard = storedDashboard;
        setOverflow(Overflow.AUTO);
        setPadding(5);
    }

    @Override
    protected void onInit() {
        super.onInit();
        buildEditForm();
    }

    public void redraw() {
        for (Canvas c : getChildren()) {
            c.removeFromParent();
        }

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

        loadPortlets();

        addMember(editForm);
        editForm.hide();
        addMember(portalLayout);

    }

    private DynamicForm buildEditForm() {
        editForm = new LocatableDynamicForm(extendLocatorId("Editor"));
        editForm.setAutoWidth();
        editForm.setNumCols(9);

        TextItem nameItem = new TextItem("name", MSG.common_title_dashboard_name());
        nameItem.setValue(storedDashboard.getName());
        nameItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                String val = (String) changedEvent.getValue();
                if (!(null == val || "".equals(val.trim()))) {
                    storedDashboard.setName(val.trim());
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
        //        removeColumn.setIcon("silk/application_side_contract.png");
        removeColumn.setAutoFit(true);
        removeColumn.setStartRow(false);
        removeColumn.setEndRow(false);

        removeColumn.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {

                Canvas[] canvases = portalLayout.getMembers();
                int numMembers = canvases.length;
                if (numMembers > 0) {
                    Canvas lastMember = canvases[numMembers - 1];
                    portalLayout.removeMember(lastMember);
                    numColItem.setValue(numMembers - 1);
                    storedDashboard.setColumns(storedDashboard.getColumns() - 1);
                    save();
                }
                save();
            }
        });

        Menu addPorletMenu = new Menu();
        for (String portletName : PortletFactory.getRegisteredPortlets()) {
            addPorletMenu.addItem(new MenuItem(portletName));
        }

        addPortlet = new LocatableIMenuButton(extendLocatorId("AddPortal"), MSG.common_title_add_portlet(),
            addPorletMenu);

        //        addPortlet = new ButtonItem("addPortlet", "Add Portlet");
        addPortlet.setIcon("[skin]/images/actions/add.png");
        addPortlet.setAutoFit(true);

        addPorletMenu.addItemClickHandler(new ItemClickHandler() {
            public void onItemClick(ItemClickEvent itemClickEvent) {
                String portletTitle = itemClickEvent.getItem().getTitle();
                addPortlet(portletTitle, portletTitle);
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

        editForm.setItems(nameItem, numColItem, addCanvas, picker, addColumn, removeColumn);
        return editForm;
    }

    private void loadPortlets() {

        int col = 0;
        for (int i = 0; i < storedDashboard.getColumns(); i++) {

            for (DashboardPortlet storedPortlet : storedDashboard.getPortlets(i)) {
                final PortletWindow portlet = new PortletWindow(extendLocatorId(storedPortlet.getPortletKey()), this,
                    storedPortlet);
                portlets.add(portlet);
                portlet.setTitle(storedPortlet.getName());

                portlet.setHeight(storedPortlet.getHeight());
                portlet.setVisible(true);

                portalLayout.addPortlet(portlet, col);
            }

            col++;
        }
    }

    private void addPortlet(String portletKey, String portletName) {
        DashboardPortlet storedPortlet = new DashboardPortlet(portletName, portletKey, 250);

        final PortletWindow newPortlet = new PortletWindow(extendLocatorId(portletKey), this, storedPortlet);
        portlets.add(newPortlet);

        storedDashboard.addPortlet(storedPortlet, 0, 0);

        newPortlet.setTitle(portletName);

        newPortlet.setHeight(350);
        newPortlet.setVisible(false);

        PortalColumn column = portalLayout.addPortlet(newPortlet, 0);

        // also insert a blank spacer element, which will trigger the built-in
        //  animateMembers layout animation
        final LayoutSpacer placeHolder = new LayoutSpacer();
        //        placeHolder.setRect(newPortlet.getRect());
        column.addMember(placeHolder); // add to top

        // create an outline around the clicked button
        final Canvas outline = new Canvas();
        outline.setLeft(editForm.getAbsoluteLeft() + addPortlet.getLeft());
        outline.setTop(editForm.getAbsoluteTop());
        outline.setWidth(addPortlet.getWidth());
        outline.setHeight(addPortlet.getHeight());
        outline.setBorder("2px solid 8289A6");
        outline.draw();
        outline.bringToFront();

        outline.animateRect(newPortlet.getPageLeft(), newPortlet.getPageTop(), newPortlet.getVisibleWidth(), newPortlet
            .getViewportHeight(), new AnimationCallback() {
            public void execute(boolean earlyFinish) {
                // callback at end of animation - destroy placeholder and outline; show the new portlet
                placeHolder.destroy();
                outline.destroy();
                newPortlet.show();
            }
        }, 750);
        save();
    }

    public void save() {
        GWTServiceLookup.getDashboardService().storeDashboard(storedDashboard, new AsyncCallback<Dashboard>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_dashboardManager_error(), caught);
            }

            public void onSuccess(Dashboard result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_dashboardManager_saved(result.getName()), Message.Severity.Info));
                storedDashboard = result;

                updateConfigs(result);
            }
        });
    }

    private void updateConfigs(Dashboard result) {
        for (PortletWindow portletWindow : portlets) {
            for (DashboardPortlet portlet : result.getPortlets()) {
                if (portletWindow.getDashboardPortlet().getId() == portlet.getId()) {
                    portletWindow.getDashboardPortlet().setConfiguration(portlet.getConfiguration());
                }
            }
        }
    }

    public void delete() {
        if (null != this.storedDashboard && this.storedDashboard.getId() > 0) {
            GWTServiceLookup.getDashboardService().removeDashboard(this.storedDashboard.getId(),
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("", caught);
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

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        if (editMode) {
            this.editForm.show();
        } else {
            this.editForm.hide();
        }
        markForRedraw();
    }
}
