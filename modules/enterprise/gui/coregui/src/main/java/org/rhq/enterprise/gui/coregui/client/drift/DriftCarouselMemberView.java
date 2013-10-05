/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.coregui.client.drift;

import java.util.ArrayList;
import java.util.Date;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.GenericDriftCriteria;
import org.rhq.core.domain.drift.DriftChangeSet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.carousel.CarouselMember;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;

/**
 * @author Jay Shaughnessy
 */
@SuppressWarnings("unchecked")
public class DriftCarouselMemberView extends DriftHistoryView implements CarouselMember {

    private DriftChangeSet changeSet;
    private Criteria carouselCriteria;
    private ArrayList<DriftSelectionListener> driftSelectionListeners = new ArrayList<DriftSelectionListener>();

    public static DriftCarouselMemberView get(ResourceComposite composite, DriftChangeSet driftChangeSet,
        Criteria initialCriteria) {

        EntityContext context = EntityContext.forResource(composite.getResource().getId());
        boolean hasWriteAccess = composite.getResourcePermission().isDrift();
        return new DriftCarouselMemberView(context, driftChangeSet, hasWriteAccess, initialCriteria);
    }

    public DriftCarouselMemberView(EntityContext context, DriftChangeSet driftChangeSet, boolean hasWriteAccess,
        Criteria initialCriteria) {

        super(null, context, hasWriteAccess, initialCriteria);

        this.carouselCriteria = initialCriteria;
        this.changeSet = driftChangeSet;
        ((DriftCarouselDataSource) getDataSource()).setChangeSetId(this.changeSet.getId());

        // no need to refresh the drift instances, they are fixed.        
        setShowFooterRefresh(false);

        //setWidth("500px");
    }

    @Override
    public DriftDataSource getDataSource() {
        if (null == dataSource) {
            dataSource = new DriftCarouselDataSource(getContext());
        }

        return dataSource;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        setListGridDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                ListGrid listGrid = (ListGrid) event.getSource();
                ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                if (selectedRows != null && selectedRows.length == 1) {
                    ListGridRecord record = selectedRows[0];
                    Integer resourceId = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                    Integer driftDefId = record.getAttributeAsInt(DriftDataSource.ATTR_CHANGESET_DEF_ID);
                    String driftId = getId(record);
                    String url = LinkManager.getDriftCarouselDriftLink(resourceId, driftDefId, driftId);
                    CoreGUI.goToView(url);
                }
            }
        });

        getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {

            public void onSelectionChanged(SelectionEvent event) {
                Record record = event.getRecord();
                boolean isSelected = event.getState();

                for (DriftSelectionListener listener : driftSelectionListeners) {
                    listener.onDriftSelection(record, isSelected);
                }
            }
        });
    }

    public interface DriftSelectionListener {
        void onDriftSelection(Record record, boolean isSelected);
    }

    public void addDriftSelectionListener(DriftSelectionListener listener) {
        this.driftSelectionListeners.add(listener);
    }

    @Override
    protected void configureTableFilters() {
        // filter settings come from the Carousel view and are applied to all Drift views
    }

    @Override
    protected Criteria getCurrentCriteria() {
        return carouselCriteria;
    }

    @Override
    public void refresh(Criteria carouselCriteria) {
        this.carouselCriteria = carouselCriteria;
        super.refresh();
    }

    public void updateTitleCanvas(String titleString) {
        int resourceId = getContext().getResourceId();
        String imageHtml = Canvas.imgHTML(ImageManager.getViewIcon());
        String link = LinkManager.getDriftCarouselSnapshotLink(resourceId, changeSet.getDriftDefinitionId(),
            changeSet.getVersion());
        StringBuilder sb = new StringBuilder();

        sb.append("<a href=\"").append(link).append("\">");
        sb.append(imageHtml);
        sb.append("</a>");
        sb.append("<span class=\"HeaderLabel\">");
        sb.append(MSG.view_drift_table_snapshot());
        sb.append(" ");
        sb.append(changeSet.getVersion());
        sb.append("</span>");
        sb.append("<br/>");
        sb.append(TimestampCellFormatter.DATE_TIME_FORMAT_MEDIUM.format(new Date(this.changeSet.getCtime())));

        Canvas titleCanvas = getTitleCanvas();

        titleCanvas.setWidth100();
        titleCanvas.setHeight(35);
        titleCanvas.setContents(sb.toString());
        titleCanvas.setPadding(4);

        titleCanvas.markForRedraw();
    }

    public static class DriftCarouselDataSource extends DriftDataSource {

        private String changeSetId;

        /**
         * Note, the changeSetId must be set prior to any fetches.
         * @param context
         */
        DriftCarouselDataSource(EntityContext context) {
            super(context);
        }

        protected String getChangeSetId() {
            return changeSetId;
        }

        protected void setChangeSetId(String changeSetId) {
            this.changeSetId = changeSetId;
        }

        @Override
        protected GenericDriftCriteria getFetchCriteria(DSRequest request) {

            // Call super to get standard drift criteria that has been set
            GenericDriftCriteria criteria = super.getFetchCriteria(request);

            if (null == criteria) {
                criteria = new GenericDriftCriteria();
            }

            // Always restrict to the relevant change set
            criteria.addFilterChangeSetId(changeSetId);
            return criteria;
        }

        /**
         * The view that contains the list grid which will display this datasource's data will call this
         * method to get the field information which is used to control the display of the data.
         * 
         * @return list grid fields used to display the datasource data
         */
        @Override
        public ArrayList<ListGridField> getListGridFields() {
            ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

            ListGridField categoryField = new ListGridField(ATTR_CATEGORY, MSG.common_title_category());
            categoryField.setType(ListGridFieldType.IMAGE);
            categoryField.setAlign(Alignment.CENTER);
            categoryField.setShowHover(true);
            categoryField.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String cat = record.getAttribute(ATTR_CATEGORY);
                    if (CATEGORY_ICON_ADD.equals(cat)) {
                        return MSG.view_drift_category_fileAdded();
                    } else if (CATEGORY_ICON_CHANGE.equals(cat)) {
                        return MSG.view_drift_category_fileChanged();
                    } else if (CATEGORY_ICON_REMOVE.equals(cat)) {
                        return MSG.view_drift_category_fileRemoved();
                    } else if (CATEGORY_ICON_NEW.equals(cat)) {
                        return MSG.view_drift_category_fileNew();
                    } else {
                        return ""; // will never get here
                    }
                }
            });
            fields.add(categoryField);

            ListGridField pathField = new ListGridField(ATTR_PATH, MSG.common_title_path());
            fields.add(pathField);

            categoryField.setWidth(80);
            pathField.setWidth("*");

            return fields;
        }
    }

}
