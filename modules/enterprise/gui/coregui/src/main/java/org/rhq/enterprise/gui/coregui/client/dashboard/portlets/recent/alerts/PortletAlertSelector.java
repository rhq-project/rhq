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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;

/**
 * @author Simeon Pinder
 */
public class PortletAlertSelector extends ResourceSelector {

    public PortletAlertSelector(String locatorId, Integer[] currentlyAssignedIds, ResourceType resourceTypeFilter,
        boolean forceResourceTypeFilter) {
        super(locatorId, resourceTypeFilter, forceResourceTypeFilter);

        //populate fields for grid.
        ListGridField nameField = new ListGridField("name", MSG.common_title_name());
        ListGridField iconField = new ListGridField("icon", MSG.common_title_icon(), 50);
        iconField.setImageURLPrefix("types/");
        iconField.setType(ListGridFieldType.ICON);
        assignedGrid.setFields(iconField, nameField);

        setWidth(RecentAlertsPortlet.ALERT_RESOURCE_SELECTION_WIDTH);

        //retrieve the previously assigned resource ids
        if ((currentlyAssignedIds != null) && currentlyAssignedIds.length > 0) {
            //build listgrid records
            ResourceCriteria criteria = new ResourceCriteria();
            criteria.addFilterIds(currentlyAssignedIds);

            GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                new AsyncCallback<PageList<Resource>>() {
                    @Override
                    public void onSuccess(PageList<Resource> result) {
                        if (result.size() > 0) {
                            ListGridRecord[] data = (new ResourceDatasource()).buildRecords(result);
                            assignedGrid.setData(data);
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_portlet_recentAlerts_fail_msg(), caught);
                    }
                });
        }
    }

    public Integer[] getAssignedListGridValues() {
        Integer[] listGridValues = new Integer[0];
        if ((null != assignedGrid)) {
            RecordList allRecords = assignedGrid.getDataAsRecordList();
            if (allRecords.getLength() > 0) {
                listGridValues = new Integer[allRecords.getLength()];
                for (int i = 0; i < allRecords.getLength(); i++) {
                    Record record = allRecords.get(i);
                    listGridValues[i] = record.getAttributeAsInt(RecentAlertsPortlet.ID);
                }
            }
        }
        return listGridValues;
    }
}
