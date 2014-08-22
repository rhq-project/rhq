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

package org.rhq.coregui.client.components.carousel;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.DetailsView;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.table.AbstractTableSection;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * Provides the typical carousel view with the additional ability of traversing to "details" views - a masters/detail 
 * view in effect. This class is analogous to {@link AbstractTableSection}.
 * 
 * @author Jay Shaughnessy
 */
public abstract class BookmarkableCarousel extends Carousel implements BookmarkableView {

    private boolean initialDisplay;
    private VLayout detailsHolder;
    private Canvas detailsView;
    private String basePath;

    public BookmarkableCarousel() {
        super();
    }

    public BookmarkableCarousel(String titleString) {
        super(titleString);
    }

    public BookmarkableCarousel(String titleString, Criteria criteria) {
        super(titleString, criteria);
    }

    @Override
    protected void onInit() {
        super.onInit();

        this.initialDisplay = true;

        detailsHolder = new EnhancedVLayout();
        detailsHolder.setAlign(VerticalAlignment.TOP);
        detailsHolder.setMargin(4);
        detailsHolder.hide();

        addMember(detailsHolder);

        // if the detailsView is already defined it means we want the details view to be rendered prior to
        // the master view, probably due to a direct navigation or refresh (like F5 when sitting on the details page)
        if (null != detailsView) {
            switchToDetailsView();
        }
    }

    @Override
    public void renderView(ViewPath viewPath) {
        viewPath.next();
        this.basePath = viewPath.getPathToCurrent();

        if (!viewPath.isEnd()) {
            viewPath.next();
            // get the id, which may be in string format
            String id = convertCurrentViewPathToID(viewPath.getCurrent().getPath());
            detailsView = getDetailsView(id);

            switchToDetailsView();

        } else {
            switchToCarouselView();
        }
    }

    // the main CoreGUI class will assume anything with a digit as the first character in a path segment in the
    // URL is an ID.
    public static final String ID_PREFIX = "0id_"; // the prefix to be placed in front of the string IDs in URLs

    protected String convertCurrentViewPathToID(String path) {
        return path.startsWith(ID_PREFIX) ? path.substring(ID_PREFIX.length()) : path;
    }

    /**
     * Returns the details canvas with information on the item that has the given ID.
     * Note that an empty details view should be returned if the id passed in is 0 (as would
     * be the case if a new item is to be created using the details view).
     *
     * @param id the id of the details to be shown; will be "0" if an empty details view should be shown.
     */
    public abstract Canvas getDetailsView(String id);

    /**
     * Switches to viewing the details canvas, hiding the table. This does not
     * do anything with reloading data or switching to the selected row in the table;
     * this only changes the visibility of canvases.
     */
    protected void switchToDetailsView() {
        Canvas contents = getCarouselContents();

        // If the Carousel has not yet been initialized then ignore
        if (contents != null) {
            // If the carousel view is visible then gracefully switch to the details view.
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
            detailsHolder.addMember(backButton);
            VLayout verticalSpacer = new EnhancedVLayout();
            verticalSpacer.setHeight(8);
            detailsHolder.addMember(verticalSpacer);
        }

        detailsHolder.addMember(detailsView);
        detailsHolder.animateShow(AnimationEffect.WIPE);
    }

    /**
     * Switches to viewing the table, hiding the details canvas.
     */
    protected void switchToCarouselView() {
        final Canvas contents = getCarouselContents();
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

}
