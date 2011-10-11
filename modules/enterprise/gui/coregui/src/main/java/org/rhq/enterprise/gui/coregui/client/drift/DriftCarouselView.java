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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.PopupWindow;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.carousel.Carousel;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.drift.DriftCarouselMemberView.DriftSelectionListener;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jay Shaughnessy
 */
public class DriftCarouselView extends Carousel implements DetailsView, BookmarkableView {

    private static final int CAROUSEL_DEFAULT_SIZE = 4;
    private static final String CAROUSEL_MEMBER_FIXED_WIDTH = "250px";

    private int driftDefId;
    private EntityContext context;
    private boolean hasWriteAccess;
    private Integer maxCarouselEndFilter;
    private ArrayList<Record> selectedRecords = new ArrayList<Record>();

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

    public DriftCarouselView(String locatorId, EntityContext entityContext, int driftDefId, boolean hasWriteAccess) {
        super(locatorId);

        this.context = entityContext;
        this.driftDefId = driftDefId;
        this.hasWriteAccess = hasWriteAccess;
    }

    @Override
    protected void onDraw() {

        DriftDefinitionCriteria defCriteria = new DriftDefinitionCriteria();
        defCriteria.addFilterId(driftDefId);
        defCriteria.fetchConfiguration(true);

        driftService.findDriftDefinitionsByCriteria(defCriteria, new AsyncCallback<PageList<DriftDefinition>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
            }

            public void onSuccess(PageList<DriftDefinition> result) {
                // Create and add the details canvas for the def
                buildTitle(result.get(0));

                buildCarousel(false);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void buildCarousel(final boolean isRefresh) {
        super.buildCarousel(isRefresh);

        // clear our list of currently selected records because we're recreating the carousel members
        selectedRecords.clear();

        // Fetch the ChangeSets ("header" info only, these will be held in memory)
        GenericDriftChangeSetCriteria changeSetCriteria = new GenericDriftChangeSetCriteria();
        // Limit to change sets for the relevant drift detection definition
        changeSetCriteria.addFilterDriftDefinitionId(driftDefId);
        // Never include the initial snapshot, limit to drift instances only
        changeSetCriteria.addFilterStartVersion("1");
        // Limit to change sets meeting any current carousel filtering criteria        
        if (isRefresh) {
            addCarouselCriteria(changeSetCriteria);
        }

        // return most recent first
        changeSetCriteria.addSortVersion(PageOrdering.DESC);

        driftService.findDriftChangeSetsByCriteria(changeSetCriteria,
            new AsyncCallback<PageList<? extends DriftChangeSet>>() {

                public void onSuccess(PageList<? extends DriftChangeSet> result) {

                    Integer carouselSize = getCarouselSizeFilter();
                    carouselSize = (null == carouselSize || carouselSize < 1) ? CAROUSEL_DEFAULT_SIZE : carouselSize;
                    int size = carouselSize;
                    Integer carouselStart = null;
                    Integer carouselEnd = null;
                    Criteria initialCriteria = getInitialMemberCriteria(isRefresh ? getCurrentCriteria() : null);

                    for (DriftChangeSet changeSet : result) {
                        DriftCarouselMemberView view = new DriftCarouselMemberView(extendLocatorId(changeSet.getId()),
                            context, changeSet, hasWriteAccess, initialCriteria);
                        addCarouselMember(view);
                        view.addDriftSelectionListener(new DriftSelectionListener() {

                            public void onDriftSelection(Record record, boolean isSelected) {
                                if (isSelected) {
                                    selectedRecords.add(record);
                                } else {
                                    selectedRecords.remove(record);
                                }

                                DriftCarouselView.this.refreshCarouselInfo();
                                setFilter(DriftDataSource.FILTER_PATH, record.getAttribute(DriftDataSource.ATTR_PATH));
                            }
                        });

                        if (null == carouselStart) {
                            carouselStart = changeSet.getVersion();
                            if (null == maxCarouselEndFilter) {
                                maxCarouselEndFilter = carouselStart;
                            }
                        }
                        carouselEnd = changeSet.getVersion();

                        if (--size == 0) {
                            break;
                        }
                    }

                    if (!isRefresh) {
                        DriftCarouselView.super.onDraw();
                    } else {
                        DriftCarouselView.this.refreshCarouselInfo();
                    }

                    setCarouselStartFilter(carouselStart);
                    setCarouselEndFilter(carouselEnd);
                    setCarouselSizeFilter(carouselSize);
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_snapshots_tree_loadFailure(), caught);
                }
            });
    }

    protected static Criteria getInitialMemberCriteria(Criteria additionalCriteria) {
        if (null == additionalCriteria) {
            return DriftHistoryView.INITIAL_CRITERIA;
        }

        Criteria initialCriteria = new Criteria();
        addCriteria(initialCriteria, DriftHistoryView.INITIAL_CRITERIA);
        addCriteria(initialCriteria, additionalCriteria);
        return initialCriteria;
    }

    @Override
    protected int getDefaultCarouselSize() {
        return CAROUSEL_DEFAULT_SIZE;
    }

    @Override
    protected String getCarouselMemberFixedWidth() {
        return CAROUSEL_MEMBER_FIXED_WIDTH;
    }

    /**
     * Only return change sets that actually have drift that matches the current carousel filters, including
     * the drift level filtering. This allows us to handle gaps in the presented snapshots when certain changesets 
     * have no drift matching desired criteria.  
     * 
     * @param changeSetCriteria
     */
    private void addCarouselCriteria(GenericDriftChangeSetCriteria changeSetCriteria) {
        Integer carouselStartFilter;
        Integer carouselEndFilter;

        // if no startFilter is set then include changesets up to the most recent
        try {
            carouselStartFilter = Integer.valueOf(getCarouselStartFilter());
            if (carouselStartFilter > 0) {
                changeSetCriteria.addFilterEndVersion(String.valueOf(carouselStartFilter));
            }
        } catch (Exception e) {
            carouselStartFilter = null;
        }

        // if no endFilter is set then include changesets greater than 0 (never include 0, the initial snapshot)
        try {
            carouselEndFilter = Integer.valueOf(getCarouselEndFilter());
            if (carouselEndFilter < 1) {
                carouselEndFilter = 1;
            } else if (carouselEndFilter <= maxCarouselEndFilter) {
                changeSetCriteria.addFilterStartVersion(String.valueOf(carouselEndFilter));
            }
        } catch (Exception e) {
            carouselEndFilter = null;
        }

        // apply the drift-level carousel filters in order to filter out changesets that have no applicab;e drift 
        Criteria criteria = getCurrentCriteria();
        DriftCategory[] driftCategoriesFilter = RPCDataSource.getArrayFilter(criteria,
            DriftDataSource.FILTER_CATEGORIES, DriftCategory.class);
        changeSetCriteria.addFilterDriftCategories(driftCategoriesFilter);

        String driftPathFilter = RPCDataSource.getFilter(criteria, DriftDataSource.FILTER_PATH, String.class);
        if (null != driftPathFilter && !driftPathFilter.isEmpty()) {
            changeSetCriteria.addFilterDriftPath(driftPathFilter);
        }
    }

    private void buildTitle(DriftDefinition driftDef) {

        setTitleString(driftDef.getName());
        setTitleBackButton(new BackButton(extendLocatorId("BackButton"), MSG.view_tableSection_backButton(),
            LinkManager.getDriftDefinitionsLink(this.context.getResourceId())));
    }

    @Override
    protected void configureCarousel() {
        addCarouselAction("Compare", MSG.common_button_compare(), null, new CarouselAction() {

            public void executeAction(Object actionValue) {

                String id1 = selectedRecords.get(0).getAttribute(DriftDataSource.ATTR_ID);
                String id2 = selectedRecords.get(1).getAttribute(DriftDataSource.ATTR_ID);
                final String path = selectedRecords.get(0).getAttribute(DriftDataSource.ATTR_PATH);

                GWTServiceLookup.getDriftService().generateUnifiedDiffByIds(id1, id2,
                    new AsyncCallback<FileDiffReport>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to generate diff", caught);
                        }

                        @Override
                        public void onSuccess(FileDiffReport diffReport) {
                            String diffContents = toHtml(diffReport.getDiff(), 1, 2);
                            LocatableWindow window = createDiffViewer(diffContents, path, 1, 2);
                            window.show();
                        }

                        private String toHtml(List<String> deltas, int oldVersion, int newVersion) {
                            StringBuilder diff = new StringBuilder();
                            diff.append("<font color=\"red\">").append(deltas.get(0)).append(":").append(oldVersion)
                                .append("</font><br/>");
                            diff.append("<font color=\"green\">").append(deltas.get(1)).append(":").append(newVersion)
                                .append("</font><br/>");

                            for (String line : deltas.subList(2, deltas.size())) {
                                if (line.startsWith("@@")) {
                                    diff.append("<font color=\"purple\">").append(line).append("</font><br/>");
                                } else if (line.startsWith("-")) {
                                    diff.append("<font color=\"red\">").append(line).append("</font><br/>");
                                } else if (line.startsWith("+")) {
                                    diff.append("<font color=\"green\">").append(line).append("</font><br/>");
                                } else {
                                    diff.append(line).append("<br/>");
                                }
                            }
                            return diff.toString();
                        }

                        private LocatableWindow createDiffViewer(String contents, String path, int oldVersion,
                            int newVersion) {
                            VLayout layout = new VLayout();
                            DynamicForm form = new DynamicForm();
                            form.setWidth100();
                            form.setHeight100();

                            CanvasItem canvasItem = new CanvasItem();
                            canvasItem.setColSpan(2);
                            canvasItem.setShowTitle(false);
                            canvasItem.setWidth("*");
                            canvasItem.setHeight("*");

                            Canvas canvas = new Canvas();
                            canvas.setContents(contents);
                            canvasItem.setCanvas(canvas);

                            form.setItems(canvasItem);
                            layout.addMember(form);

                            PopupWindow window = new PopupWindow("diffViewer", layout);
                            window.setTitle(path + ":" + oldVersion + ":" + newVersion);
                            window.setIsModal(false);

                            return window;
                        }

                    });

            }

            public boolean isEnabled() {
                if (selectedRecords.size() == 2) {
                    String path1 = selectedRecords.get(0).getAttribute(DriftDataSource.ATTR_PATH);
                    String path2 = selectedRecords.get(1).getAttribute(DriftDataSource.ATTR_PATH);

                    return (null != path1) && path1.equals(path2);
                }

                return false;
            }
        });

        super.configureCarousel();
    }

    @Override
    protected void configureCarouselFilters() {
        // drift category filter 
        LinkedHashMap<String, String> categories = new LinkedHashMap<String, String>(3);
        categories.put(DriftCategory.FILE_ADDED.name(), MSG.view_drift_category_fileAdded());
        categories.put(DriftCategory.FILE_CHANGED.name(), MSG.view_drift_category_fileChanged());
        categories.put(DriftCategory.FILE_REMOVED.name(), MSG.view_drift_category_fileRemoved());
        LinkedHashMap<String, String> categoryIcons = new LinkedHashMap<String, String>(3);
        categoryIcons.put(DriftCategory.FILE_ADDED.name(), ImageManager.getDriftCategoryIcon(DriftCategory.FILE_ADDED));
        categoryIcons.put(DriftCategory.FILE_CHANGED.name(), ImageManager
            .getDriftCategoryIcon(DriftCategory.FILE_CHANGED));
        categoryIcons.put(DriftCategory.FILE_REMOVED.name(), ImageManager
            .getDriftCategoryIcon(DriftCategory.FILE_REMOVED));

        SelectItem categoryFilter = new EnumSelectItem(DriftDataSource.FILTER_CATEGORIES, MSG.common_title_category(),
            DriftCategory.class, categories, categoryIcons);

        // drift file path filter
        TextItem pathFilter = new TextItem(DriftDataSource.FILTER_PATH, MSG.common_title_path());
        pathFilter.setEndRow(true);

        if (isShowFilterForm()) {
            setFilterFormItems(categoryFilter, pathFilter);
        }
    }

    @Override
    protected String getCarouselStartFilterLabel() {
        return MSG.view_drift_carousel_startFilterLabel();
    }

    @Override
    protected String getCarouselSizeFilterLabel() {
        return MSG.view_drift_carousel_sizeFilterLabel();
    }

    @Override
    public boolean isEditable() {
        // This ensures the default BackButton is not presented. Instead, we implement our own back capability
        return true;
    }

    private boolean initialDisplay;
    private VLayout detailsHolder;
    private Canvas detailsView;
    private String basePath;

    @Override
    protected void onInit() {
        super.onInit();

        this.initialDisplay = true;

        detailsHolder = new LocatableVLayout(extendLocatorId("carousel"));
        detailsHolder.setAlign(VerticalAlignment.TOP);
        //detailsHolder.setWidth100();
        //detailsHolder.setHeight100();
        detailsHolder.setMargin(4);
        detailsHolder.hide();

        addMember(detailsHolder);

        // if the detailsView is already defined it means we want the details view to be rendered prior to
        // the master view, probably due to a direct navigation or refresh (like F5 when sitting on the details page)
        if (null != detailsView) {
            switchToDetailsView();
        }
    }

    // this class is somewhat unusual in that it is a detail view for the drift defs list view, but also a pseudo-
    // master view for snapshot and drift detail views.  It's "pseudo" in that there is no master-detail infrastucture
    // like that found in TableSection. The following paths must be handled (starting at the ^):
    //   #Resource/10001/Drift/Definitions/10001/History/driftId
    //                                           ^
    //   #Resource/10001/Drift/Definitions/10001/Snapshot/version
    //                                           ^
    @Override
    public void renderView(ViewPath viewPath) {
        viewPath.next();
        this.basePath = viewPath.getPathToCurrent();

        if (!viewPath.isEnd()) {
            // we have two detail views for the drift carousel, a drift history or a snapshot view. Figure out which one
            // we're dealing with. 
            boolean isHistory = "History".equals(viewPath.getCurrent().getPath());
            viewPath.next();
            // get the id, which may be in string format
            String id = convertCurrentViewPathToID(viewPath.getCurrent().getPath());

            if (isHistory) {
                detailsView = new DriftDetailsView(extendLocatorId("History"), id);
            } else {
                detailsView = new DriftSnapshotView(extendLocatorId("Snapshot"), context.getResourceId(), driftDefId,
                    Integer.valueOf(id));
            }

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
                SeleniumUtility.destroyMembers(detailsHolder);

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
            VLayout verticalSpacer = new LocatableVLayout(extendLocatorId("verticalSpacer"));
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
                        SeleniumUtility.destroyMembers(detailsHolder);

                        contents.animateShow(AnimationEffect.WIPE);
                    }
                });
            } else {
                if (detailsHolder != null) {
                    SeleniumUtility.destroyMembers(detailsHolder);
                }
                contents.animateShow(AnimationEffect.WIPE);
            }
        }
    }

}
