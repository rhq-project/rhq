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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.event;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class EventDatasource extends RPCDataSource<Event> {


    public EventDatasource() {
        super();

//        DataSourceField id = new DataSourceIntegerField("id", "Id");
//        id.setPrimaryKey(true);
//        addField(id);

        DataSourceTextField detail = new DataSourceTextField("detail", "Detail");
        addField(detail);

//        DataSourceTextField detailExcerpt = new DataSourceTextField("detailExcerpt", "Detail Excerpt");
//        addField(detailExcerpt);

        DataSourceTextField severity = new DataSourceTextField("severity", "Servity");
        addField(severity);

//        DataSourceTextField source = new DataSourceTextField("source", "Source");
//        addField(source);

        DataSourceTextField sourceLocation = new DataSourceTextField("sourceLocation", "Source Location");
        addField(sourceLocation);

//        DataSourceTextField type = new DataSourceTextField("type", "Type");
//        addField(type);

        DataSourceTextField timestamp = new DataSourceTextField("timestamp", "Timestamp");
        timestamp.setType(FieldType.DATETIME);
        addField(timestamp);

        
    }


    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        EventCriteria criteria = new EventCriteria();
        criteria.addSortSeverity(PageOrdering.DESC);

        criteria.setPageControl(getPageControl(request));
        criteria.fetchSource(true);

        if (request.getCriteria().getValues().get("resourceId") != null) {
            criteria.addFilterResourceId(request.getCriteria().getAttributeAsInt("resourceId"));
        }

        GWTServiceLookup.getEventService().findEventsByCriteria(criteria,
                new AsyncCallback<PageList<Event>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load event data", caught);
                    }

                    public void onSuccess(PageList<Event> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }
                });

    }

    @Override
    public Event copyValues(ListGridRecord from) {
        return null;  // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(Event from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getId());
        record.setAttribute("detail", from.getDetail());
        record.setAttribute("detailExcerpt", from.getDetailExcerpt());
        record.setAttribute("severity", from.getSeverity().name());
        record.setAttribute("source", from.getSource());
        record.setAttribute("sourceLocation", from.getSource().getLocation());
        record.setAttribute("type", from.getType());
        record.setAttribute("timestamp", new Date(from.getTimestamp()));

        return record;
    }


}
