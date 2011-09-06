/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.drift;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTreeGrid;

/**
 * @author John Mazzitelli
 */
public abstract class AbstractDriftChangeSetsTreeView extends LocatableTreeGrid {

    // We may need to wait for tree data to be fetched before we can complete processing the selected path.
    // If so, hold it here and retry after the datasource pulls the data.
    private ViewPath pendingPath = null;

    public AbstractDriftChangeSetsTreeView(String locatorId, boolean canManageDrift) {
        super(locatorId);
        setWidth100();
        setHeight100();
        setLeaveScrollbarGap(false);
        // fetch the top nodes at the inital onDraw()
        setAutoFetchData(true);
        setAnimateFolders(false);
        setSelectionType(SelectionStyle.MULTIPLE);
        setShowRollOver(false);
        setShowHeader(false);
        setSortField(AbstractDriftChangeSetsTreeDataSource.ATTR_NAME);
        setSortDirection(SortDirection.DESCENDING);
    }

    @Override
    protected void onInit() {
        super.onInit();

        // We may need to wait for tree data to be fetched before we can complete processing the selected path.
        // When the datasource pulls data keep processing the selectedPath if necessary.
        addDataArrivedHandler(new DataArrivedHandler() {
            public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                if (null != pendingPath) {
                    selectPath(pendingPath);
                }
            }
        });

        addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(final NodeContextClickEvent event) {
                // stop the browser right-click menu
                event.cancel();

                TreeNode eventNode = event.getNode();

                Menu menu = null;
                if (eventNode instanceof DriftConfigurationTreeNode) {
                    menu = buildDriftConfigurationTreeNodeContextMenu((DriftConfigurationTreeNode) eventNode);
                } else if (eventNode instanceof ChangeSetTreeNode) {
                    menu = buildChangeSetTreeNodeContextMenu((ChangeSetTreeNode) eventNode);
                } else if (eventNode instanceof DriftTreeNode) {
                    menu = buildDriftTreeNodeContextMenu((DriftTreeNode) eventNode);
                }
                if (menu != null) {
                    menu.showContextMenu();
                }
            }
        });

    }

    public void selectPath(ViewPath viewPath) {
        Tree theTree = getTree();

        if (viewPath.viewsLeft() > 0) {
            String key = "";
            for (ViewId view : viewPath.getViewPath().subList(2, viewPath.getViewPath().size())) {
                if (key.length() > 0) {
                    key += "_";
                }

                key += view.getPath();

                TreeNode node = theTree.findById(key);

                if (node != null) {
                    // open the node, this will force a fetch of child data if necessary
                    theTree.openFolder(node);
                } else {
                    // wait for data to get loaded...
                    pendingPath = new ViewPath(viewPath.toString());
                    return;
                }
            }

            // we found the node, so keep going
            pendingPath = null;

            final String finalKey = key;
            GWT.runAsync(new RunAsyncCallback() {
                public void onFailure(Throwable reason) {
                }

                public void onSuccess() {
                    TreeNode node = getTree().findById(finalKey);
                    if (node != null) {
                        deselectAllRecords();
                        selectRecord(node);
                    }
                }
            });
        } else {
            deselectAllRecords();
            if (getTotalRows() > 0)
                selectRecord(0);
        }
    }

    public void refresh() {
        invalidateCache();
    }

    /**
     * Returns the link (as a string) that the client should be redirected to
     * if the given node's details are to be shown.
     * 
     * @param node the node whose target details link is to be returned
     * @return the node's details link
     */
    protected abstract String getNodeDetailsLink(TreeNode node);

    /**
     * Deletes the given drift configuration.
     * 
     * @param doomedDriftConfig the drift config to remove
     */
    protected abstract void deleteDriftConfiguration(DriftConfiguration doomedDriftConfig);

    /**
     * Immediately asks for a drift detection scan to be run.
     * 
     * @param driftConfiguration identifies the drift whose detection scan should be initiated.
     */
    protected abstract void detectDrift(DriftConfiguration driftConfiguration);

    /**
     * Builds the right-mouse-click context menu for the given drift configuration node
     * @param node the drift configuration node whose menu is to be displayed
     * @return the context menu to display
     */
    protected Menu buildDriftConfigurationTreeNodeContextMenu(final DriftConfigurationTreeNode node) {

        Menu contextMenu = new Menu();

        // title
        String titleName = node.getTitle();
        if (titleName.length() > 50) {
            // make sure the title isn't really long so the menu is not abnormally wide
            titleName = "..." + titleName.substring(titleName.length() - 50);
        }
        MenuItem titleItem = new MenuItem(titleName);
        titleItem.setEnabled(false);
        contextMenu.setItems(titleItem);

        // separator
        contextMenu.addItem(new MenuItemSeparator());

        // item that deletes the config
        MenuItem deleteItem = new MenuItem(MSG.common_button_delete());
        deleteItem.setIcon(Window.getImgURL(ImageManager.getRemoveIcon()));
        deleteItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                DriftConfiguration driftConfig = node.getDriftConfiguration();
                deleteDriftConfiguration(driftConfig);
            }
        });
        contextMenu.addItem(deleteItem);

        // item that initiates a drift detection scan
        MenuItem detectNowItem = new MenuItem(MSG.view_drift_button_detectNow());
        detectNowItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                DriftConfiguration driftConfig = node.getDriftConfiguration();
                detectDrift(driftConfig);
            }
        });
        contextMenu.addItem(detectNowItem);

        // item that links to the config details
        MenuItem detailsItem = new MenuItem(MSG.common_title_details());
        detailsItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                String link = getNodeDetailsLink(node);
                if (link != null) {
                    CoreGUI.goToView(link);
                }
            }
        });
        contextMenu.addItem(detailsItem);

        return contextMenu;
    }

    /**
     * Builds the right-mouse-click context menu for the given drift node
     * @param node the drift node whose menu is to be displayed
     * @return the context menu to display
     */
    protected Menu buildDriftTreeNodeContextMenu(final DriftTreeNode node) {

        Menu contextMenu = new Menu();

        // title
        String titleName = node.getTitle();
        if (titleName.length() > 50) {
            // make sure the title isn't really long so the menu is not abnormally wide
            titleName = "..." + titleName.substring(titleName.length() - 50);
        }
        MenuItem titleItem = new MenuItem(titleName);
        titleItem.setEnabled(false);
        contextMenu.setItems(titleItem);

        // separator
        contextMenu.addItem(new MenuItemSeparator());

        // item that links to the history details
        MenuItem detailsItem = new MenuItem(MSG.common_title_details());
        detailsItem.addClickHandler(new ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                String link = getNodeDetailsLink(node);
                if (link != null) {
                    CoreGUI.goToView(link);
                }
            }
        });
        contextMenu.addItem(detailsItem);

        return contextMenu;
    }

    /**
     * Builds the right-mouse-click context menu for the given change set node
     * @param node the change set node whose menu is to be displayed
     * @return the context menu to display
     */
    protected Menu buildChangeSetTreeNodeContextMenu(final ChangeSetTreeNode node) {

        Menu contextMenu = new Menu();

        // title
        String titleName = node.getTitle();
        MenuItem titleItem = new MenuItem(titleName);
        titleItem.setEnabled(false);
        contextMenu.setItems(titleItem);

        // separator
        contextMenu.addItem(new MenuItemSeparator());

        // item that shows the drift configuration
        MenuItem idItem = new MenuItem(MSG.common_title_id() + "=" + node.getChangeSetId());
        idItem.setEnabled(false);
        contextMenu.addItem(idItem);

        return contextMenu;
    }

    /**
     * We override this because we know all of our nodes will have titles. Without this,
     * I could not get the title of change set nodes to show - it always fell back to using
     * the name. This way is at least faster, since we don't do any conditional checking,
     * we always immediately use the getTitle results.
     */
    @Override
    protected String getNodeTitle(Record node, int recordNum, ListGridField field) {
        return ((TreeNode) node).getTitle();
    }

    static class DriftConfigurationTreeNode extends TreeNode {
        public static final String ATTR_DRIFT_CONFIG_OBJECT = "object";

        public DriftConfigurationTreeNode(DriftConfiguration driftConfig) {
            setIsFolder(true);
            if (driftConfig.isEnabled()) {
                setIcon("subsystems/drift/DriftConfig_Folder_16.png");
            } else {
                setIcon("subsystems/drift/DriftConfig_Disabled_Folder_16.png");
            }
            setShowOpenIcon(true);
            setID(String.valueOf(driftConfig.getId()));
            setName(buildNodeName(driftConfig)); // we sort on this column
            setAttribute(ATTR_DRIFT_CONFIG_OBJECT, driftConfig);
        }

        public int getDriftConfigurationId() {
            String idAttrib = getAttribute("id");
            return Integer.parseInt(idAttrib);
        }

        public DriftConfiguration getDriftConfiguration() {
            return (DriftConfiguration) getAttributeAsObject(ATTR_DRIFT_CONFIG_OBJECT);
        }

        @Override
        public String getTitle() {
            return super.getName();
        }

        private String buildNodeName(DriftConfiguration driftConfig) {
            return driftConfig.getName();
        }
    }

    static class ChangeSetTreeNode extends TreeNode {
        public ChangeSetTreeNode(DriftChangeSet<?> changeset) {
            setIsFolder(true);
            setShowOpenIcon(true);
            String parentID = String.valueOf(changeset.getDriftConfigurationId());
            setParentID(parentID);
            setID(parentID + "_" + changeset.getId());
            setName(padWithZeroes(changeset.getVersion())); // we sort on this column, hence we make sure the version # is padded
            setTitle(buildNodeName(changeset));

            if (changeset.getVersion() == 0) {
                setIcon("subsystems/drift/ChangeSet_Coverage_Folder_16.png");
            } else {
                setIcon("subsystems/drift/ChangeSet_Folder_16.png");
            }
        }

        public String getChangeSetId() {
            String idAttrib = getAttribute("id");
            return idAttrib.substring(idAttrib.indexOf('_') + 1);
        }

        private String buildNodeName(DriftChangeSet<?> changeset) {
            StringBuilder str = new StringBuilder();
            str.append(MSG.common_title_version());
            str.append(' ');
            str.append(changeset.getVersion());
            str.append(" (");
            str.append(TimestampCellFormatter.format(changeset.getCtime(),
                TimestampCellFormatter.DATE_TIME_FORMAT_SHORT));
            str.append(")");
            return str.toString();
        }
    }

    static class DriftTreeNode extends TreeNode {
        public DriftTreeNode(Drift<?, ?> drift) {
            setIsFolder(false);
            setIcon(ImageManager.getDriftCategoryIcon(drift.getCategory()));
            DriftChangeSet<?> changeset = drift.getChangeSet();
            setParentID(String.valueOf(changeset.getDriftConfigurationId()) + "_" + changeset.getId());
            setID('D' + drift.getId()); // prefix with a 'D' so it doesn't conflict with driftConfig node IDs
            setName(drift.getPath()); // we sort on this column
        }

        public String getDriftId() {
            String idAttrib = getAttribute("id");
            return idAttrib.substring(1); // skip the 'D' prefix!
        }

        @Override
        public String getTitle() {
            return super.getName();
        }
    }

    static private String padWithZeroes(int numberToPad) {
        String stringToPad = String.valueOf(numberToPad);
        char[] zeroes = new char[10 - stringToPad.length()];
        for (int i = 0; i < zeroes.length; i++)
            zeroes[i] = '0';
        return new String(zeroes) + stringToPad;
    }
}
