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

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.DriftDefinitionCriteria;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.carousel.Carousel;
import org.rhq.enterprise.gui.coregui.client.components.form.EnumSelectItem;
import org.rhq.enterprise.gui.coregui.client.gwt.DriftGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jay Shaughnessy
 */
public class DriftCarouselView extends Carousel {

    private static final int CAROUSEL_DEFAULT_SIZE = 4;
    private static final String CAROUSEL_MEMBER_FIXED_WIDTH = "250px";

    private int driftDefId;
    private EntityContext context;
    private boolean hasWriteAccess;

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
                addTitleString(result.get(0));

                buildCarousel(false);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void buildCarousel(final boolean isRefresh) {
        super.buildCarousel(isRefresh);

        // Fetch the ChangeSet headers
        GenericDriftChangeSetCriteria changeSetCriteria = new GenericDriftChangeSetCriteria();
        changeSetCriteria.addFilterDriftDefinitionId(driftDefId);
        setChangeSetVersionCriteria(changeSetCriteria);

        changeSetCriteria.addSortVersion(PageOrdering.DESC);

        driftService.findDriftChangeSetsByCriteria(changeSetCriteria,
            new AsyncCallback<PageList<? extends DriftChangeSet>>() {

                public void onSuccess(PageList<? extends DriftChangeSet> result) {

                    Integer carouselSize = getCarouselSizeFilter();
                    carouselSize = (null == carouselSize || carouselSize < 1) ? CAROUSEL_DEFAULT_SIZE : carouselSize;
                    int size = carouselSize;
                    Integer carouselStart = null;
                    Criteria initialCriteria = getInitialMemberCriteria(isRefresh ? getCurrentCriteria() : null);

                    Map map = initialCriteria.getValues();
                    map.entrySet();

                    for (DriftChangeSet changeSet : result) {
                        DriftCarouselMemberView view = new DriftCarouselMemberView(extendLocatorId(changeSet.getId()),
                            context, changeSet, hasWriteAccess, initialCriteria);
                        addCarouselMember(view);

                        if (null == carouselStart) {
                            carouselStart = changeSet.getVersion();
                        }

                        if (--size == 0) {
                            break;
                        }
                    }

                    if (!isRefresh) {
                        DriftCarouselView.super.onDraw();
                    }

                    setCarouselStartFilter(carouselStart);
                    setCarouselSizeFilter(carouselSize);
                    setCarouselDirty(false);
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

    private void setChangeSetVersionCriteria(GenericDriftChangeSetCriteria changeSetCriteria) {
        Integer carouselStart;

        try {
            carouselStart = Integer.valueOf(getCarouselStartFilter());
        } catch (Exception e) {
            carouselStart = null;
        }

        if (null != carouselStart) {
            changeSetCriteria.addFilterEndVersion(String.valueOf(carouselStart));
        }
        changeSetCriteria.addFilterStartVersion("1");
    }

    private void addTitleString(DriftDefinition driftDef) {

        setTitleString(driftDef.getName());
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

}
