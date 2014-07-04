/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components.table;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.DetailsView;
import org.rhq.coregui.client.InitializableView;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Provides the typical table view with the additional ability of traversing to a "details" view
 * when double-clicking a individual row in the table - a masters/detail view in effect.
 *
 * @param <DS> the datasource used to obtain data for the table
 * @param <ID> the type used for IDs. This identifies the type used to uniquely refer to a row in the table
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public abstract class AbstractTableSection<DS extends RPCDataSource, ID> extends Table<DS> implements BookmarkableView,
    InitializableView {

    private VLayout detailsHolder;
    private Canvas detailsView;
    private Canvas header;
    private String basePath;
    private boolean escapeHtmlInDetailsLinkColumn;
    private boolean initialDisplay;
    private boolean initialized;

    protected AbstractTableSection(String tableTitle) {
        super(tableTitle);
    }

    protected AbstractTableSection(String tableTitle, Criteria criteria) {
        super(tableTitle, criteria);
    }

    protected AbstractTableSection(String tableTitle, SortSpecifier[] sortSpecifiers) {
        super(tableTitle, sortSpecifiers);
    }

    protected AbstractTableSection(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers) {
        super(tableTitle, sortSpecifiers, criteria);
    }

    protected AbstractTableSection(String tableTitle, boolean autoFetchData) {
        super(tableTitle, autoFetchData);
    }

    protected AbstractTableSection(String tableTitle, SortSpecifier[] sortSpecifiers, String[] excludedFieldNames) {
        super(tableTitle, null, sortSpecifiers, excludedFieldNames);
    }

    protected AbstractTableSection(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(tableTitle, criteria, sortSpecifiers, excludedFieldNames);
    }

    protected AbstractTableSection(String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(tableTitle, criteria, sortSpecifiers, excludedFieldNames, autoFetchData);
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.initialDisplay = true;

        detailsHolder = new EnhancedVLayout();
        detailsHolder.setAlign(VerticalAlignment.TOP);
        //detailsHolder.setWidth100();
        //detailsHolder.setHeight100();

        detailsHolder.hide();

        addMember(detailsHolder);

        // if the detailsView is already defined it means we want the details view to be rendered prior to
        // the master view, probably due to a direct navigation or refresh (like F5 when sitting on the details page)
        if (null != detailsView) {
            switchToDetailsView();
        }

        this.initialized = true;
    }

    @Override
    public void destroy() {
        this.initialized = false;

        super.destroy();
    }

    @Override
    public boolean isInitialized() {
        return super.isInitialized() && this.initialized;
    }

    /**
     * The default implementation wraps the {@link #getDetailsLinkColumnCellFormatter()} column with the
     * {@link #getDetailsLinkColumnCellFormatter()}. This is typically the 'name' column linking to the detail
     * view, given the 'id'. Also, establishes a double click handler for the row which invokes
     * {@link #showDetails(com.smartgwt.client.widgets.grid.ListGridRecord)}</br>
     * </br>
     * In general, in overrides, call super.configureTable *after* manipulating the ListGrid fields.
     *
     * @see org.rhq.coregui.client.components.table.Table#configureTable()
     */
    @Override
    protected void configureTable() {
        if (isDetailsEnabled()) {
            ListGrid grid = getListGrid();

            // Make the value of some specific field a link to the details view for the corresponding record.
            ListGridField field = (grid != null) ? grid.getField(getDetailsLinkColumnName()) : null;
            if (field != null) {
                field.setCellFormatter(getDetailsLinkColumnCellFormatter());
            }

            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                    if (selectedRows != null && selectedRows.length == 1) {
                        showDetails(selectedRows[0]);
                    }
                }
            });
        }
    }

    protected boolean isDetailsEnabled() {
        return true;
    }

    public void setEscapeHtmlInDetailsLinkColumn(boolean escapeHtmlInDetailsLinkColumn) {
        this.escapeHtmlInDetailsLinkColumn = escapeHtmlInDetailsLinkColumn;
    }

    /**
     * Override if you don't want FIELD_NAME to be wrapped ina link.
     * @return the name of the field to be wrapped, or null if no field should be wrapped.
     */
    protected String getDetailsLinkColumnName() {
        return FIELD_NAME;
    }

    /**
     * Override if you don't want the detailsLinkColumn to have the default link wrapper.
     * @return the desired CellFormatter.
     */
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                if (value == null) {
                    return "";
                }
                ID recordId = getId(record);
                String detailsUrl = "#" + getBasePath() + "/" + convertIDToCurrentViewPath(recordId);
                String formattedValue = (escapeHtmlInDetailsLinkColumn) ? StringUtility.escapeHtml(value.toString())
                    : value.toString();
                return LinkManager.getHref(detailsUrl, formattedValue);
            }
        };
    }

    /**
     * Shows the details view for the given record of the table.
     *
     * The default implementation of this method assumes there is an
     * id attribute on the record and passes it to {@link #showDetails(Object)}.
     * Subclasses are free to override this behavior. Subclasses usually
     * will need to set the {@link #setDetailsView(Canvas) details view}
     * explicitly.
     *
     * @param record the record whose details are to be shown
     */
    public void showDetails(ListGridRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("'record' parameter is null.");
        }

        ID id = getId(record);
        showDetails(id);
    }

    /**
     * Returns the details canvas with information on the item given its list grid record.
     *
     * The default implementation of this method is to assume there is an
     * id attribute on the record and pass that ID to {@link #getDetailsView(Object)}.
     * Subclasses are free to override this - which you usually want to do
     * if you know the full details of the item are stored in the record attributes
     * and thus help avoid making a round trip to the DB.
     *
     * @param record the record of the item whose details to be shown; ; null if empty details view should be shown.
     */
    public Canvas getDetailsView(ListGridRecord record) {
        ID id = getId(record);
        return getDetailsView(id);
    }

    /**
     * Subclasses define how they want to format their identifiers. These uniquely identify
     * rows in the table. Typical values/types for IDs are Integers or Strings.
     *
     * @param record the individual record that contains the ID to be extracted and returned
     *
     * @return the ID of the given row/record from the table.
     */
    protected abstract ID getId(ListGridRecord record);

    /**
     * Shows empty details for a new item being created.
     * This method is usually called when a user clicks a 'New' button.
     *
     * Subclasses are free to override this if they need a custom way to show the details view.
     *
     * @see #showDetails(ListGridRecord)
     */
    public void newDetails() {
        CoreGUI.goToView(basePath + "/0"); // assumes the subclasses will understand "0" means "new details page"
    }

    /**
     * Shows the details for an item that has the given ID.
     * This method is usually called when a user goes to the details
     * page via a bookmark, double-cick on a list view row, or direct link.
     *
     * @param id the id of the row whose details are to be shown; Must be a valid ID.
     *
     * @see #showDetails(ListGridRecord)
     *
     * @throws IllegalArgumentException if id is invalid
     */
    public abstract void showDetails(ID id);

    /**
     * Returns the details canvas with information on the item that has the given ID.
     * Note that an empty details view should be returned if the id passed in is 0 (as would
     * be the case if a new item is to be created using the details view).
     *
     * @param id the id of the details to be shown; will be "0" if an empty details view should be shown.
     */
    public abstract Canvas getDetailsView(ID id);

    /**
     * Given the path from the URL that identifies the ID, this returns the ID represented by that path string.
     * @param path the path as it was found in the current view path (i.e. in the URL)
     * @return the ID that identifies the item referred to by the URL
     */
    protected abstract ID convertCurrentViewPathToID(String path);

    /**
     * Given the ID of a particular item, this returns a path string suitable for placement in a URL such that that URL will
     * identify the particular item.
     *
     * @return how the ID can be represented within a view path (i.e. in a URL)
     * @param id the ID that identifies the item to be referred by in a URL
     */
    protected abstract String convertIDToCurrentViewPath(ID id);

    @Override
    public void renderView(ViewPath viewPath) {
        this.basePath = viewPath.getPathToCurrent();

        if (!viewPath.isEnd()) {
            ID id = convertCurrentViewPathToID(viewPath.getCurrent().getPath());
            this.detailsView = getDetailsView(id);
            if (this.detailsView instanceof BookmarkableView) {
                ((BookmarkableView) this.detailsView).renderView(viewPath);
            }

            switchToDetailsView();
        } else {
            switchToTableView();
        }
    }

    protected String getBasePath() {
        return this.basePath;
    }

    /**
     * For use by subclasses that want to define their own details view.
     *
     * @param detailsView the new details view
     */
    protected void setDetailsView(Canvas detailsView) {
        this.detailsView = detailsView;
    }

    /**
     * Switches to viewing the details canvas, hiding the table. This does not
     * do anything with reloading data or switching to the selected row in the table;
     * this only changes the visibility of canvases.
     */
    protected void switchToDetailsView() {
        Canvas contents = getTableContents();

        // If the Table has not yet been initialized then ignore
        if (contents != null) {
            // If the table view is visible then gracefully switch to the details view.
            if (contents.isVisible()) {
                contents.animateHide(AnimationEffect.WIPE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        buildDetailsView();
                    }
                });
            } else {
                // Even if the table view is not visible, it may not be hidden. Instead, it may be the
                // case that its parent (the encompassing Table/HLayout) may not be visible.  This is unusual
                // because typically we switch between the table and detail view while under the subtab, but
                // if we navigate to the detail view from another subtab (for example, the drift tree context
                // menu) the Table may not be visible and the table view may not be hidden.  To make a long
                // story short, ensure the table view is hidden when displaying the details view.
                contents.hide();

                /*
                 * if the programmer chooses to go directly from the detailView in create-mode to the
                 * detailsView in edit-mode, the content canvas will already be hidden, which means the
                 * animateHide would be a no-op (the event won't fire).  this causes the detailsHolder
                 * to keep a reference to the previous detailsView (the one in create-mode) instead of the
                 * newly returned reference from getDetailsView(ID) that was called when the renderView
                 * methods were called hierarchically down to render the new detailsView in edit-mode.
                 * therefore, we need to explicitly destroy what's already there (presumably the detailsView
                 * in create-mode), and then rebuild it (presumably the detailsView in edit-mode).
                 */
                EnhancedUtility.destroyMembers(detailsHolder);

                buildDetailsView();
            }
        }
    }

    private void buildDetailsView() {
        detailsView.setWidth100();
        detailsView.setHeight100();

        boolean isEditable = (detailsView instanceof DetailsView && ((DetailsView) detailsView).isEditable());
        if (!isEditable) {
            // Only add the "Back to List" button if the details are definitely not editable, because if they are
            // editable, a Cancel button should already be provided by the details view.
            BackButton backButton = new BackButton(MSG.view_tableSection_backButton(), basePath);
            HLayout hlayout = new EnhancedHLayout();
            hlayout.addMember(backButton);
            if (header != null) {
                header.setWidth100();
                header.setAlign(com.smartgwt.client.types.Alignment.CENTER);
                hlayout.addMember(header);
            }
            detailsHolder.addMember(hlayout);
            LayoutSpacer verticalSpacer = new LayoutSpacer();
            verticalSpacer.setHeight(8);
            detailsHolder.addMember(verticalSpacer);
        }

        detailsHolder.addMember(detailsView);
        detailsHolder.animateShow(AnimationEffect.WIPE);
    }

    /**
     * Switches to viewing the table, hiding the details canvas.
     */
    protected void switchToTableView() {
        final Canvas contents = getTableContents();
        if (contents != null) {
            // If this is not the initial display of the table, refresh the table's data. Otherwise, a refresh would be
            // redundant, since the data was just loaded when the table was drawn.
            if (this.initialDisplay) {
                this.initialDisplay = false;
            } else {
                Log.debug("Refreshing data for Table [" + getClass().getName() + "]...");
                refresh();
            }
            // if the detailsHolder is visible then gracefully switch views, otherwise just
            // clean up any lingering details holder and show the table view.
            if (detailsHolder != null && detailsHolder.isVisible()) {
                detailsHolder.animateHide(AnimationEffect.WIPE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        EnhancedUtility.destroyMembers(detailsHolder);

                        contents.animateShow(AnimationEffect.WIPE);
                    }
                });
            } else {
                if (detailsHolder != null) {
                    EnhancedUtility.destroyMembers(detailsHolder);
                }
                contents.animateShow(AnimationEffect.WIPE);
            }
        }
    }

    public void setHeader(Canvas header) {
        this.header = header;
    }

}
