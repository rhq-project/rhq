/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.discovery;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.TableUtility;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class ResourceAutodiscoveryView extends LocatableVLayout implements RefreshableView {

    private static final String TITLE = MSG.view_autoDiscoveryQ_title();
    private static final String HEADER_ICON = "global/AutoDiscovery_24.png";

    private boolean simple;
    private TreeGrid treeGrid;
    private ToolStrip footer;
    private DataSource dataSource;
    // This allows the selection handler to ignore selection changes initiated by us, as opposed to by the user.
    private boolean selectionChangedHandlerDisabled;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService(1000000);

    public ResourceAutodiscoveryView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();
    }

    public ResourceAutodiscoveryView(String locatorId, boolean simple) {
        this(locatorId);
        this.simple = simple;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!simple) {
            Img img = new Img(HEADER_ICON, 24, 24);
            img.setPadding(4);

            HTMLFlow title = new HTMLFlow();
            title.setWidth100();
            title.setHeight(35);
            title.setContents(TITLE);
            title.setPadding(4);
            title.setStyleName("HeaderLabel");

            HLayout titleLayout = new HLayout();
            titleLayout.setAutoHeight();
            titleLayout.setAlign(VerticalAlignment.BOTTOM);

            titleLayout.addMember(img);
            titleLayout.addMember(title);

            addMember(titleLayout);
        }

        treeGrid = new LocatableTreeGrid(this.getLocatorId());

        treeGrid.setHeight100();

        dataSource = new AutodiscoveryQueueDataSource(treeGrid);
        treeGrid.setDataSource(dataSource);
        treeGrid.setAutoFetchData(true);
        treeGrid.setResizeFieldsInRealTime(true);
        treeGrid.setAutoFitData(Autofit.HORIZONTAL);
        treeGrid.setWrapCells(true);
        treeGrid.setFixedRecordHeights(false);

        final TreeGridField name, key, type, description, status, ctime;
        name = new TreeGridField("name");
        key = new TreeGridField("resourceKey");
        type = new TreeGridField("typeName");
        description = new TreeGridField("description");
        status = new TreeGridField("statusLabel");
        ctime = new TreeGridField("ctime");
        TimestampCellFormatter.prepareDateField(ctime);

        if (!simple) {
            treeGrid.setFields(name, key, type, description, status, ctime);
        } else {
            treeGrid.setFields(name, key, type, status);
        }

        treeGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);

        // Do this last since it causes the TreeGrid to be initialized.
        treeGrid.deselectAllRecords();

        addMember(treeGrid);

        footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        addMember(footer);

        final IButton importButton = new LocatableIButton(this.extendLocatorId("Import"), MSG
            .view_autoDiscoveryQ_import());
        final IButton ignoreButton = new LocatableIButton(this.extendLocatorId("Ignore"), MSG
            .view_autoDiscoveryQ_ignore());
        final IButton unignoreButton = new LocatableIButton(this.extendLocatorId("Unignore"), MSG
            .view_autoDiscoveryQ_unignore());

        footer.addMember(importButton);
        footer.addMember(ignoreButton);
        footer.addMember(unignoreButton);

        disableButtons(importButton, ignoreButton, unignoreButton);

        footer.addMember(new LayoutSpacer());

        // The remaining footer items (status filter, (de)select all buttons, and refresh button) will be right-aligned.

        DynamicForm form = new LocatableDynamicForm(this.extendLocatorId("Status"));
        final SelectItem statusSelectItem = new SelectItem("status", MSG.view_autoDiscoveryQ_showStatus());
        statusSelectItem.setValueMap(AutodiscoveryQueueDataSource.NEW, AutodiscoveryQueueDataSource.IGNORED,
            AutodiscoveryQueueDataSource.NEW_AND_IGNORED);
        statusSelectItem.setValue(AutodiscoveryQueueDataSource.NEW);
        statusSelectItem.setWrapTitle(false);
        form.setItems(statusSelectItem);

        statusSelectItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent changedEvent) {
                treeGrid.fetchData(new Criteria("status", (String) statusSelectItem.getValue()));
            }
        });
        footer.addMember(form);

        final IButton selectAllButton = new LocatableIButton(this.extendLocatorId("SelectAll"),
                MSG.view_autoDiscoveryQ_selectAll());
        footer.addMember(selectAllButton);
        final IButton deselectAllButton = new LocatableIButton(this.extendLocatorId("DeselectAll"),
                MSG.view_autoDiscoveryQ_deselectAll());
        deselectAllButton.setDisabled(true);
        footer.addMember(deselectAllButton);

        IButton refreshButton = new LocatableIButton(extendLocatorId("Refresh"), MSG.common_button_refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                refresh();
            }
        });
        footer.addMember(refreshButton);

        treeGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionChangedHandlerDisabled || selectionEvent.isRightButtonDown()) {
                    return;
                }
                selectionChangedHandlerDisabled = true;

                final TreeNode selectedNode = (TreeNode) selectionEvent.getRecord();
                TreeNode parentNode = treeGrid.getTree().getParent(selectedNode);
                boolean isPlatform = treeGrid.getTree().isRoot(parentNode);
                boolean isCheckboxMarked = treeGrid.isSelected(selectedNode);

                if (isPlatform) {
                    if (isCheckboxMarked) {
                        SC.ask(MSG.view_autoDiscoveryQ_confirmSelect(), new BooleanCallback() {
                            public void execute(Boolean confirmed) {
                                if (confirmed) {
                                    for (ListGridRecord child : treeGrid.getTree().getChildren(selectedNode)) {
                                        if (!treeGrid.isSelected(child)) {
                                            treeGrid.selectRecord(child);
                                        }
                                    }
                                }
                                updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                                        unignoreButton);
                                selectionChangedHandlerDisabled = false;
                            }
                        });
                    } else {
                        for (ListGridRecord child : treeGrid.getTree().getChildren(selectedNode)) {
                            if (treeGrid.isSelected(child)) {
                                treeGrid.deselectRecord(child);
                            }
                        }
                        // the immediate redraw below should not be necessary, but without it the deselected
                        // platform checkbox remained checked.
                        treeGrid.redraw();
                        updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                                unignoreButton);
                        selectionChangedHandlerDisabled = false;
                    }
                } else {
                    if (isCheckboxMarked) {
                        if (!treeGrid.isSelected(parentNode)) {
                            treeGrid.selectRecord(parentNode);
                        }
                    }
                    updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                            unignoreButton);
                    selectionChangedHandlerDisabled = false;
                }
            }

        });

        selectAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_autoDiscoveryQ_confirmSelectAll(), new BooleanCallback() {
                    public void execute(Boolean selectChildServers) {
                        selectionChangedHandlerDisabled = true;
                        if (selectChildServers) {
                            treeGrid.selectAllRecords();
                        } else {
                            // Select only the platforms.
                            TreeNode rootNode = treeGrid.getTree().getRoot();
                            TreeNode[] platformNodes = treeGrid.getTree().getChildren(rootNode);
                            for (TreeNode platformNode : platformNodes) {
                                treeGrid.selectRecord(platformNode);
                            }
                        }
                        selectionChangedHandlerDisabled = false;
                        updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                                unignoreButton);
                    }
                });
            }
        });

        deselectAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                selectionChangedHandlerDisabled = true;
                treeGrid.deselectAllRecords();
                selectionChangedHandlerDisabled = false;
                updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
            }
        });

        importButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableButtons(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_autoDiscoveryQ_importInProgress(), Message.Severity.Info, EnumSet
                        .of(Message.Option.Transient)));

                resourceService.importResources(getSelectedIds(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_importFailure(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_autoDiscoveryQ_importSuccessful(), Message.Severity.Info));
                        refresh();
                    }
                });
            }
        });

        ignoreButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableButtons(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_autoDiscoveryQ_ignoreInProgress(), Message.Severity.Info, EnumSet
                        .of(Message.Option.Transient)));

                resourceService.ignoreResources(getSelectedIds(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_ignoreFailure(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_autoDiscoveryQ_ignoreSuccessful(), Message.Severity.Info));
                        refresh();
                    }
                });
            }
        });

        unignoreButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableButtons(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_autoDiscoveryQ_unignoreInProgress(), Message.Severity.Info, EnumSet
                        .of(Message.Option.Transient)));

                resourceService.unignoreResources(getSelectedIds(), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_unignoreFailure(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_autoDiscoveryQ_unignoreSuccessful(), Message.Severity.Info));
                        refresh();
                    }
                });
            }
        });

    }

    private void updateButtonEnablement(IButton selectAllButton, IButton deselectAllButton,
                                        IButton importButton, IButton ignoreButton, IButton unignoreButton) {
        if (treeGrid.getSelection().length == 0) {
            selectAllButton.setDisabled(false);
            deselectAllButton.setDisabled(true);
            importButton.setDisabled(true);
            ignoreButton.setDisabled(true);
            unignoreButton.setDisabled(true);
            return;
        }

        boolean allSelected = (treeGrid.getSelection().length == treeGrid.getRecords().length);
        selectAllButton.setDisabled(allSelected);
        deselectAllButton.setDisabled(false);

        boolean importOk = false;
        boolean ignoreOk = false;
        boolean unignoreOk = false;

        for (ListGridRecord listGridRecord : treeGrid.getSelection()) {
            TreeNode node = (TreeNode) listGridRecord;
            String status = node.getAttributeAsString("status");
            TreeNode parentNode = treeGrid.getTree().getParent(node);
            boolean isPlatform = treeGrid.getTree().isRoot(parentNode);

            importOk |= InventoryStatus.NEW.name().equals(status);
            unignoreOk |= InventoryStatus.IGNORED.name().equals(status);

            if (!isPlatform) {
                String parentStatus = parentNode.getAttributeAsString("status");
                if (InventoryStatus.COMMITTED.name().equals(parentStatus)) {
                    ignoreOk |= InventoryStatus.NEW.name().equals(status);
                }
            }
        }

        importButton.setDisabled(!importOk || unignoreOk);
        ignoreButton.setDisabled(!ignoreOk || unignoreOk);
        unignoreButton.setDisabled(!unignoreOk || importOk || ignoreOk);
        markForRedraw();
    }

    private void disableButtons(IButton... buttons) {
        for (IButton button : buttons) {
            button.setDisabled(true);
        }
    }

    private int[] getSelectedIds() {
        List<Integer> selected = new ArrayList<Integer>();
        for (ListGridRecord node : treeGrid.getSelection()) {
            if (!InventoryStatus.COMMITTED.name().equals(node.getAttributeAsString("status"))) {
                selected.add(Integer.parseInt(node.getAttributeAsString("id")));
            }
        }

        return TableUtility.getIds(selected);
    }

    /**
     * Custom refresh operation, as we cannot extend Table because we use a TreeGrid, not a ListGrid.
     */
    public void refresh() {
        this.treeGrid.invalidateCache();
        this.treeGrid.markForRedraw();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public TreeGrid getTreeGrid() {
        return treeGrid;
    }

}
