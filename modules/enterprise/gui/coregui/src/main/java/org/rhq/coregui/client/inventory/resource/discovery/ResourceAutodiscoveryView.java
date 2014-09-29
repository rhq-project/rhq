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
package org.rhq.coregui.client.inventory.resource.discovery;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeGridField;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.DataArrivedEvent;
import com.smartgwt.client.widgets.tree.events.DataArrivedHandler;

import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.util.TableUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class ResourceAutodiscoveryView extends EnhancedVLayout implements RefreshableView, HasViewName {

    public static final ViewName VIEW_ID = new ViewName("AutodiscoveryQueue", MSG.view_inventory_adq(),
        IconEnum.DISCOVERY_QUEUE);

    // Specify 3m timeout to compensate for import taking a long time for a large number of Resources.
    // TODO (ips, 08/31/11): Remove this once import has been refactored to be partially asynchronous, i.e. where the
    //                       call to importResources() flips all the Resources to a new COMMITTING inventory status
    //                       and then kicks off a background job to do the actual work of committing (syncing to
    //                       Agents, etc.).
    ResourceGWTServiceAsync importResourceService = GWTServiceLookup.getResourceService(3 * 60 * 1000);

    private boolean simple;
    private TreeGrid treeGrid;
    private ToolStrip footer;
    private AutodiscoveryQueueDataSource dataSource;
    // This allows the selection handler to ignore selection changes initiated by us, as opposed to by the user.
    private boolean selectionChangedHandlerDisabled;
    private boolean loadAllPlatforms;

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceAutodiscoveryView() {
        this(false);
    }

    public ResourceAutodiscoveryView(boolean simple) {
        setWidth100();
        setHeight100();

        this.simple = simple;
    }

    @Override
    protected void onInit() {
        super.onInit();

        treeGrid = new TreeGrid();

        treeGrid.setHeight100();

        dataSource = new AutodiscoveryQueueDataSource(treeGrid);
        treeGrid.setDataSource(dataSource);
        treeGrid.setAutoFetchData(true);
        treeGrid.setResizeFieldsInRealTime(true);
        treeGrid.setAutoFitData(Autofit.HORIZONTAL);
        treeGrid.setWrapCells(true);
        treeGrid.setFixedRecordHeights(false);
        treeGrid.setFolderIcon(ImageManager.getDiscoveryQueuePlatformIconBase());
        treeGrid.setNodeIcon(ImageManager.getResourceIcon(ResourceCategory.SERVER));

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

        final IButton importButton = new EnhancedIButton(MSG.common_button_import(), ButtonColor.BLUE);
        final IButton ignoreButton = new EnhancedIButton(MSG.view_autoDiscoveryQ_ignore(), ButtonColor.RED);
        final IButton unignoreButton = new EnhancedIButton(MSG.view_autoDiscoveryQ_unignore());

        footer.addMember(importButton);
        footer.addMember(ignoreButton);
        footer.addMember(unignoreButton);

        disableButtons(importButton, ignoreButton, unignoreButton);

        footer.addMember(new LayoutSpacer());

        // The remaining footer items (status filter, (de)select all buttons, and refresh button) will be right-aligned.

        DynamicForm form = new DynamicForm();
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

        final IButton selectAllButton = new EnhancedIButton(MSG.view_autoDiscoveryQ_selectAll());
        footer.addMember(selectAllButton);
        final IButton deselectAllButton = new EnhancedIButton(MSG.view_autoDiscoveryQ_deselectAll());
        deselectAllButton.setDisabled(true);
        footer.addMember(deselectAllButton);

        IButton refreshButton = new EnhancedIButton(MSG.common_button_refresh());
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

                final TreeNode selectedNode = treeGrid.getTree()
                    .findById(selectionEvent.getRecord().getAttribute("id"));
                TreeNode parentNode = treeGrid.getTree().getParent(selectedNode);
                boolean isPlatform = treeGrid.getTree().isRoot(parentNode);
                boolean isCheckboxMarked = treeGrid.isSelected(selectedNode);

                if (isPlatform) {
                    if (isCheckboxMarked) {
                        SC.ask(MSG.view_autoDiscoveryQ_confirmSelect(), new BooleanCallback() {
                            public void execute(Boolean confirmed) {
                                if (confirmed && !treeGrid.getTree().hasChildren(selectedNode)) {
                                    selectedNode.setAttribute("selectChildOnArrival", "true");
                                    treeGrid.getTree().loadChildren(selectedNode);
                                } else {
                                    if (confirmed) {
                                        selectAllPlatformChildren(selectedNode);
                                    }
                                    updateButtonEnablement(selectAllButton, deselectAllButton, importButton,
                                        ignoreButton, unignoreButton);
                                    selectionChangedHandlerDisabled = false;
                                }
                            }
                        });
                    } else {
                        selectedNode.setAttribute("autoSelectChildren", "false");
                        treeGrid.deselectRecords(treeGrid.getTree().getChildren(selectedNode));

                        // the immediate redraw below should not be necessary, but without it the deselected
                        // platform checkbox remained checked.
                        // treeGrid.redraw();
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

        treeGrid.addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                if (treeGrid != null) {
                    TreeNode parent = dataArrivedEvent.getParentNode();
                    if (!treeGrid.getTree().isRoot(parent)) {
                        // This flag can be set when we select a platform or as part of selectAll button handling. It
                        // means that we want to immediately select the child server nodes.
                        boolean selectChildOnArrival = Boolean.valueOf(parent.getAttribute("selectChildOnArrival"));
                        if (selectChildOnArrival) {
                            // data includes the platform, just get the descendant servers
                            treeGrid.selectRecords(treeGrid.getData().getDescendantLeaves());
                        }
                    }

                    // This logic is relevant to what we do in the selectAll button
                    boolean endDisable = true;
                    if (loadAllPlatforms) {
                        TreeNode rootNode = treeGrid.getTree().getRoot();
                        TreeNode[] platformNodes = treeGrid.getTree().getChildren(rootNode);

                        for (TreeNode platformNode : platformNodes) {
                            if (!treeGrid.getTree().isLoaded(platformNode)) {
                                endDisable = false;
                                break;
                            }
                        }
                    }
                    if (endDisable) {
                        updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                            unignoreButton);
                        selectionChangedHandlerDisabled = false;
                    }

                    // NOTE: Due to a SmartGWT bug, the TreeGrid is not automatically redrawn upon data arrival, and
                    //       calling treeGrid.markForRedraw() doesn't redraw it either. The user can mouse over the grid
                    //       to cause it to redraw, but it is obviously not reasonable to expect that. So we must
                    //       explicitly call redraw() here.
                    treeGrid.redraw();
                }
            }
        });

        dataSource.addFailedFetchListener(new AsyncCallback() {

            // just in case we have a failure while fetching, try to make sure we don't lock up the view.
            public void onFailure(Throwable caught) {
                loadAllPlatforms = false;
                updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
                selectionChangedHandlerDisabled = false;
            }

            public void onSuccess(Object result) {
                // never called
            }
        });

        selectAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_autoDiscoveryQ_confirmSelectAll(), new BooleanCallback() {
                    public void execute(Boolean selectChildServers) {

                        selectionChangedHandlerDisabled = true;
                        TreeNode rootNode = treeGrid.getTree().getRoot();
                        TreeNode[] platformNodes = treeGrid.getTree().getChildren(rootNode);

                        if (selectChildServers) {
                            disableButtons(selectAllButton, deselectAllButton, importButton, ignoreButton,
                                unignoreButton);

                            // we may need to fetch the child servers in order to select them.
                            ArrayList<TreeNode> platformsToLoad = new ArrayList<TreeNode>();

                            for (TreeNode platformNode : platformNodes) {
                                if (treeGrid.getTree().isLoaded(platformNode)) {
                                    // if loaded then just select the nodes
                                    if (!treeGrid.isSelected(platformNode)) {
                                        treeGrid.selectRecord(platformNode);
                                    }
                                    treeGrid.selectRecords(treeGrid.getTree().getChildren(platformNode));

                                } else {
                                    platformsToLoad.add(platformNode);
                                }
                            }

                            if (platformsToLoad.isEmpty()) {
                                // we're done, everything is already loaded
                                treeGrid.markForRedraw();
                                updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                                    unignoreButton);
                                selectionChangedHandlerDisabled = false;

                            } else {
                                // this plays with the dataArrivedHandler which is responsible for
                                // doing the child selection and checking to see if all platforms have been loaded
                                // as the child data comes in.
                                loadAllPlatforms = true;
                                for (TreeNode platformToLoad : platformsToLoad) {
                                    // load and select
                                    if (!treeGrid.isSelected(platformToLoad)) {
                                        treeGrid.selectRecord(platformToLoad);
                                    }
                                    platformToLoad.setAttribute("selectChildOnArrival", "true");
                                    treeGrid.getTree().loadChildren(platformToLoad);
                                }
                            }

                        } else {
                            treeGrid.selectRecords(platformNodes);
                            treeGrid.markForRedraw();
                            updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton,
                                unignoreButton);
                            selectionChangedHandlerDisabled = false;
                        }
                    }
                });
            }
        });

        deselectAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                selectionChangedHandlerDisabled = true;
                treeGrid.deselectAllRecords();
                treeGrid.markForRedraw();
                updateButtonEnablement(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
                selectionChangedHandlerDisabled = false;
            }
        });

        importButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableButtons(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);

                // TODO (ips): Make the below message sticky, but add a new ClearSticky Message option that the
                //             below callback methods can use to clear it once the importResources() call has
                //             completed.
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_autoDiscoveryQ_importInProgress(), Message.Severity.Info, EnumSet
                        .of(Message.Option.Transient)));

                importResources(getSelectedPlatforms());

            }
        });

        ignoreButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                disableButtons(selectAllButton, deselectAllButton, importButton, ignoreButton, unignoreButton);
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_autoDiscoveryQ_ignoreInProgress(), Message.Severity.Info, EnumSet
                        .of(Message.Option.Transient)));

                // assuming here that the ignore list will not be massive and that we can do it all in one go,
                // otherwise re-impl this like import.
                resourceService.ignoreResources(getAllSelectedIds(), new AsyncCallback<Void>() {
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

                // assuming here that the unignore list will not be massive and that we can do it all in one go,
                // otherwise re-impl this like import.
                resourceService.unignoreResources(getAllSelectedIds(), new AsyncCallback<Void>() {
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

    // run through the platforms, committing ids for for 1 platform on each pass.  doing it in chunks prevents
    // a timeout for a large import request.
    private void importResources(final List<TreeNode> platforms) {

        int[] idsToImport = null;
        for (Iterator<TreeNode> i = platforms.iterator(); i.hasNext();) {
            TreeNode platformNode = i.next();
            int[] uncommittedIdsForPlatform = getPlatformSelectedIds(platformNode);
            // remove this platform, it either has nothing to import, or its resources will be imported
            i.remove();
            if (uncommittedIdsForPlatform.length > 0) {
                idsToImport = uncommittedIdsForPlatform;
                break;
            }
        }

        if (null == idsToImport) {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.view_autoDiscoveryQ_importSuccessful(), Message.Severity.Info));
            refresh();
            return;
        }

        importResourceService.importResources(idsToImport, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_autoDiscoveryQ_importFailure(), caught);
                refresh();
            }

            public void onSuccess(Void result) {
                // recurse on the smaller platform list...
                importResources(platforms);
            }
        });

    }

    private void selectAllPlatformChildren(TreeNode platformNode) {
        for (ListGridRecord child : treeGrid.getTree().getChildren(platformNode)) {
            if (!treeGrid.isSelected(child)) {
                treeGrid.selectRecord(child);
            }
        }
    }

    private void updateButtonEnablement(IButton selectAllButton, IButton deselectAllButton, IButton importButton,
        IButton ignoreButton, IButton unignoreButton) {
        if (treeGrid.getSelectedRecords().length == 0) {
            selectAllButton.setDisabled(treeGrid.getRecords().length == 0);
            deselectAllButton.setDisabled(true);
            importButton.setDisabled(true);
            ignoreButton.setDisabled(true);
            unignoreButton.setDisabled(true);
            markForRedraw();
            return;
        }

        boolean allSelected = (treeGrid.getSelectedRecords().length == treeGrid.getRecords().length);
        selectAllButton.setDisabled(allSelected);
        deselectAllButton.setDisabled(false);

        boolean importOk = false;
        boolean ignoreOk = false;
        boolean unignoreOk = false;

        for (ListGridRecord listGridRecord : treeGrid.getSelectedRecords()) {
            TreeNode node = treeGrid.getTree().findById(listGridRecord.getAttribute("id"));
            String status = node.getAttributeAsString("status");

            importOk |= InventoryStatus.NEW.name().equals(status);
            unignoreOk |= InventoryStatus.IGNORED.name().equals(status);
            ignoreOk |= InventoryStatus.NEW.name().equals(status);
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

    private List<TreeNode> getSelectedPlatforms() {
        TreeNode rootNode = treeGrid.getTree().getRoot();
        TreeNode[] platformNodes = treeGrid.getTree().getChildren(rootNode);
        List<TreeNode> result = new ArrayList<TreeNode>(platformNodes.length);

        for (TreeNode platformNode : platformNodes) {
            if (treeGrid.isSelected(platformNode)) {
                result.add(platformNode);
            }
        }

        return result;
    }

    private int[] getPlatformSelectedIds(TreeNode platformNode) {
        List<Integer> result = new ArrayList<Integer>();

        if (!InventoryStatus.COMMITTED.name().equals(platformNode.getAttributeAsString("status"))) {
            result.add(Integer.parseInt(platformNode.getAttributeAsString("id")));
        }

        for (ListGridRecord childNode : treeGrid.getTree().getChildren(platformNode)) {
            if (treeGrid.isSelected(childNode)
                && !InventoryStatus.COMMITTED.name().equals(childNode.getAttributeAsString("status"))) {
                result.add(Integer.parseInt(childNode.getAttributeAsString("id")));
            }
        }

        return TableUtility.getIds(result);
    }

    private int[] getAllSelectedIds() {
        List<Integer> selected = new ArrayList<Integer>();
        for (ListGridRecord node : treeGrid.getSelectedRecords()) {
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
        if (this.treeGrid != null) {
            this.treeGrid.invalidateCache();
            this.treeGrid.markForRedraw();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public TreeGrid getTreeGrid() {
        return treeGrid;
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }
}
