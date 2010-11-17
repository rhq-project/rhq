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
package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.user.client.History;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;

/**
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class TableSection extends Table implements BookmarkableView {

    private VLayout detailsHolder;
    private Canvas detailsView;
    private String basePath;

    protected TableSection(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
    }

    protected TableSection(String locatorId, String tableTitle, Criteria criteria) {
        super(locatorId, tableTitle, criteria);
    }

    protected TableSection(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers) {
        super(locatorId, tableTitle, sortSpecifiers);
    }

    protected TableSection(String locatorId, String tableTitle, boolean autoFetchData) {
        super(locatorId, tableTitle, autoFetchData);
    }

    protected TableSection(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(locatorId, tableTitle, null, sortSpecifiers, excludedFieldNames);
    }

    protected TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames);
    }

    protected TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames, autoFetchData);
    }

    @Override
    protected void onInit() {
        super.onInit();

        detailsHolder = new VLayout();
        //detailsHolder.setWidth100();
        //detailsHolder.setHeight100();
        detailsHolder.setMargin(5);
        detailsHolder.hide();

        addMember(detailsHolder);

        // if the detailsView is already defined it means we want the details view to be rendered prior to
        // the master view, probably due to a direct navigation or refresh (like F5 when sitting on the details page)
        if (null != detailsView) {
            switchToDetailsView();
        }
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        getListGrid().addCellDoubleClickHandler(new CellDoubleClickHandler() {
            @Override
            public void onCellDoubleClick(CellDoubleClickEvent event) {
                showDetails(event.getRecord());
            }
        });
    }

    /**
     * Shows the details view for the given record of the table.
     *
     * The default implementation of this method assumes there is an
     * id attribute on the record and passes it to {@link #showDetails(int)}.
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

        Integer id = getId(record);
        showDetails(id);
    }

    /**
     * Returns the details canvas with information on the item given its list grid record.
     *
     * The default implementation of this method is to assume there is an
     * id attribute on the record and pass that ID to {@link #getDetailsView(int)}.
     * Subclasses are free to override this - which you usually want to do
     * if you know the full details of the item are stored in the record attributes
     * and thus help avoid making a round trip to the DB.
     *
     * @param record the record of the item whose details to be shown; ; null if empty details view should be shown.
     */
    public Canvas getDetailsView(ListGridRecord record) {
        Integer id = getId(record);
        return getDetailsView(id);
    }

    protected Integer getId(ListGridRecord record) {
        Integer id = (record != null) ? record.getAttributeAsInt("id") : 0;
        if (id == null) {
            String msg = MSG.view_tableSection_error_noId(this.getClass().toString());
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalStateException(msg);
        }
        return id;
    }

    /**
     * Shows empty details for a new item being created.
     * This method is usually called when a user clicks a 'New' button.
     *
     * @see #showDetails(ListGridRecord)
     */
    public void newDetails() {
        History.newItem(basePath + "/0");
    }

    /**
     * Shows the details for an item has the given ID.
     * This method is usually called when a user goes to the details
     * page via a bookmark, double-cick on a list view row, or direct link.
     *
     * @param id the id of the row whose details are to be shown; Should be a valid id, > 0.
     *
     * @see #showDetails(ListGridRecord)
     * 
     * @throws IllegalArgumentException if id <= 0.
     */
    public void showDetails(int id) {
        if (id > 0) {
            History.newItem(basePath + "/" + id);
        } else {
            String msg = MSG.view_tableSection_error_badId(this.getClass().toString(), Integer.toString(id));
            CoreGUI.getErrorHandler().handleError(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Returns the details canvas with information on the item that has the given ID.
     * Note that an empty details view should be returned if the id passed in is 0 (as would
     * be the case if a new item is to be created using the details view).
     *
     * @param id the id of the details to be shown; will be 0 if an empty details view should be shown.
     */
    public abstract Canvas getDetailsView(int id);

    @Override
    public void renderView(ViewPath viewPath) {
        this.basePath = viewPath.getPathToCurrent();

        if (!viewPath.isEnd()) {
            int id = Integer.parseInt(viewPath.getCurrent().getPath());
            this.detailsView = getDetailsView(id);
            if (this.detailsView instanceof BookmarkableView) {
                viewPath.next();
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
            if (contents.isVisible()) {
                contents.animateHide(AnimationEffect.WIPE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        buildDetailsView();
                    }
                });
            } else {
                /*
                 * if the programmer chooses to go directly from the detailView in create-mode to the 
                 * detailsView in edit-mode, the content canvas will already be hidden, which means the
                 * animateHide would be a no-op (the event won't fire).  this causes the detailsHolder 
                 * to keep a reference to the previous detailsView (the one in create-mode) instead of the
                 * newly returned reference from getDetailsView(int) that was called when the renderView
                 * methods were called hierarchically down to render the new detailsView in edit-mode.
                 * therefore, we need to explicitly destroy what's already there (presumably the detailsView
                 * in create-mode), and then rebuild it (presumably the detailsView in edit-mode).
                 */
                for (Canvas child : detailsHolder.getMembers()) {
                    child.destroy();
                }

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
            BackButton backButton = new BackButton(extendLocatorId("BackButton"), MSG.view_tableSection_backButton(),
                basePath);
            detailsHolder.addMember(backButton);
            VLayout verticalSpacer = new VLayout();
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

            if (detailsHolder != null && detailsHolder.isVisible()) {
                detailsHolder.animateHide(AnimationEffect.WIPE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        for (Canvas child : detailsHolder.getMembers()) {
                            child.destroy();
                        }

                        contents.animateShow(AnimationEffect.WIPE);
                    }
                });
            } else {
                contents.animateShow(AnimationEffect.WIPE);
            }
        }
    }
}
