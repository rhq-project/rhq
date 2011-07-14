/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.traits;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.MeasurementDataTraitCriteria;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementDataTraitListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The group Monitoring>Traits subtab.
 *
 * @author Ian Springer
 */
public class TraitsView extends AbstractMeasurementDataTraitListView {
    private int groupId;

    public TraitsView(String locatorId, int groupId) {
        super(locatorId, new TraitsDataSource(groupId), createCriteria(groupId));
        this.groupId = groupId;
    }

    @Override
    protected void configureTable() {
        ListGrid listGrid = getListGrid();

        listGrid.setShowAllRecords(true);
        listGrid.setGroupStartOpen(GroupStartOpen.ALL);
        listGrid.groupBy(MeasurementDataTraitCriteria.SORT_FIELD_DISPLAY_NAME);

        ListGridField resourceNameField = listGrid.getField(MeasurementDataTraitCriteria.SORT_FIELD_RESOURCE_NAME);
        resourceNameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                return SeleniumUtility.getLocatableHref(url, o.toString(), null);
            }
        });
        resourceNameField.setShowHover(true);
        resourceNameField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
            }
        });
        resourceNameField.setCanGroupBy(true);

        AncestryUtil.setupAncestryListGridField(listGrid);
        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(int definitionId) {
        return new TraitsDetailView(extendLocatorId("Detail"), this.groupId, definitionId);
    }

    private static Criteria createCriteria(int groupId) {
        Criteria criteria = new Criteria();

        criteria.addCriteria(MeasurementDataTraitCriteria.FILTER_FIELD_GROUP_ID, groupId);
        criteria.addCriteria(MeasurementDataTraitCriteria.FILTER_FIELD_MAX_TIMESTAMP, true);

        return criteria;
    }
}
