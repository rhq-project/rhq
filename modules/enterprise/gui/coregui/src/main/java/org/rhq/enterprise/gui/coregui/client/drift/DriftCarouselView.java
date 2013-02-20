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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.FileDiffReport;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.DetailsView;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.carousel.BookmarkableCarousel;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.drift.DriftCarouselMemberView.DriftSelectionListener;
import org.rhq.enterprise.gui.coregui.client.drift.util.DiffUtility;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A carousel view used for display of Drift Definition detail.  Each carousel member is a snapshot delta
 * view.  This view in turn serves as a "Master" view for snapshot and drift "Detail" views.  
 * 
 * @author Jay Shaughnessy
 */
public class DriftCarouselView extends BookmarkableCarousel implements DetailsView {

    private static final int CAROUSEL_DEFAULT_SIZE = 4;
    private static final String CAROUSEL_MEMBER_FIXED_WIDTH = "250px";

    private int driftDefId;
    private EntityContext context;
    private boolean hasWriteAccess;
    private Integer maxSnapshotVersion;
    private ArrayList<Record> selectedRecords = new ArrayList<Record>();
    private boolean useDriftDetailsView;

    private DriftGWTServiceAsync driftService = GWTServiceLookup.getDriftService();

    public DriftCarouselView(EntityContext entityContext, int driftDefId, boolean hasWriteAccess) {
        super();

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
                        DriftCarouselMemberView view = new DriftCarouselMemberView(context, changeSet, hasWriteAccess,
                            initialCriteria);
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

                        // descending order, so highest changeset version first
                        if (null == carouselStart) {
                            carouselStart = changeSet.getVersion();
                        }
                        carouselEnd = changeSet.getVersion();

                        if (--size == 0) {
                            break;
                        }
                    }
                    if (null == maxSnapshotVersion || null == carouselStart || maxSnapshotVersion < carouselStart) {
                        maxSnapshotVersion = carouselStart;
                        setCarouselStartFilterMax(maxSnapshotVersion);
                    }

                    setCarouselStartFilter(carouselStart);
                    setCarouselEndFilter(carouselEnd);
                    setCarouselSizeFilter(carouselSize);

                    if (!isRefresh) {
                        DriftCarouselView.super.onDraw();
                    } else {
                        DriftCarouselView.this.refreshCarouselInfo();
                    }
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_load(), caught);
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
        Integer startVersion; // low snapshot version (carouselEndFilter)
        Integer endVersion; // high snapshot version (carouselStartFilter)

        // if no startFilter is set then don't limit the endVersion
        endVersion = getCarouselStartFilter();
        if (null != endVersion) {
            changeSetCriteria.addFilterEndVersion(String.valueOf(endVersion));
        }

        // if no endFilter is set then include changesets greater than 0 (never include 0, the initial snapshot)
        // else ensure endFilter is not greater than makes sense 
        startVersion = getCarouselEndFilter();
        if (null == startVersion || 1 > startVersion) {
            startVersion = 1;

        } else if (null != endVersion && startVersion > endVersion) {
            startVersion = endVersion;
        }

        if (null != maxSnapshotVersion && startVersion > maxSnapshotVersion) {
            startVersion = maxSnapshotVersion;
        }
        changeSetCriteria.addFilterStartVersion(String.valueOf(startVersion));

        // apply the drift-level carousel filters in order to filter out changesets that have no applicable drift 
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
        setTitleBackButton(new BackButton(MSG.view_tableSection_backButton(),
            LinkManager.getDriftDefinitionsLink(this.context.getResourceId())));
    }

    @Override
    protected void configureCarousel() {
        addCarouselAction("Compare", MSG.common_button_compare(), null, new CarouselAction() {

            public void executeAction(Object actionValue) {
                Record record1 = selectedRecords.get(0);
                Record record2 = selectedRecords.get(1);
                final String path = record1.getAttribute(DriftDataSource.ATTR_PATH);
                String id1 = record1.getAttribute(DriftDataSource.ATTR_ID);
                String id2 = record2.getAttribute(DriftDataSource.ATTR_ID);
                // regardless of selection order, compare the same way, showing newest changes with '+' signs 
                int version1 = record1.getAttributeAsInt(DriftDataSource.ATTR_CHANGESET_VERSION);
                int version2 = record2.getAttributeAsInt(DriftDataSource.ATTR_CHANGESET_VERSION);
                String diffOldId = (version1 < version2) ? id1 : id2;
                String diffNewId = (version1 < version2) ? id2 : id1;

                GWTServiceLookup.getDriftService().generateUnifiedDiffByIds(diffOldId, diffNewId,
                    new AsyncCallback<FileDiffReport>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to generate diff.", caught);
                        }

                        public void onSuccess(FileDiffReport diffReport) {
                            String diffContents = DiffUtility.formatAsHtml(diffReport.getDiff(), 1, 2);
                            Window window = DiffUtility.createDiffViewerWindow(diffContents, path, 1, 2);
                            window.show();
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
        categoryIcons.put(DriftCategory.FILE_CHANGED.name(),
            ImageManager.getDriftCategoryIcon(DriftCategory.FILE_CHANGED));
        categoryIcons.put(DriftCategory.FILE_REMOVED.name(),
            ImageManager.getDriftCategoryIcon(DriftCategory.FILE_REMOVED));

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

    // this class is somewhat unusual in that it is a detail view for the drift defs list view, but also a pseudo-master
    // view for snapshot and drift detail views.  It's "pseudo" in that there is no master-detail infrastructure
    // like that found in TableSection. The following paths must be handled (starting at the ^):
    //   #Resource/10001/Drift/Definitions/10001/Drift/driftId
    //   #Resource/10001/Drift/Definitions/10001/Snapshot/version
    //                                           ^
    @Override
    public void renderView(ViewPath viewPath) {
        if (!viewPath.isEnd() && !viewPath.isNextEnd()) {
            String detail = viewPath.getNext().getPath();
            if ("Drift".equals(detail) || "Snapshot".equals(detail)) {
                this.useDriftDetailsView = !viewPath.isNextEnd() && "Drift".equals(viewPath.getNext().getPath());
                super.renderView(viewPath);
            }
        }
    }

    @Override
    public Canvas getDetailsView(String id) {
        if (this.useDriftDetailsView) {
            return new DriftDetailsView(id);
        }

        return new DriftSnapshotView(null, context.getResourceId(), driftDefId, Integer.valueOf(id), hasWriteAccess);
    }
}
