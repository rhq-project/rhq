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
package org.rhq.enterprise.gui.coregui.client.inventory.common.event;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.FieldType;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.HighlightingDatasourceTextField;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Joseph Marques
 */
public class EventCompositeDatasource extends RPCDataSource<EventComposite> {

    private EntityContext entityContext;

    public EventCompositeDatasource(EntityContext context) {
        super();
        this.entityContext = context;
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField id = new DataSourceIntegerField("id", "Id");
        id.setPrimaryKey(true);
        fields.add(id);

        DataSourceTextField timestamp = new DataSourceTextField("timestamp", "Timestamp");
        timestamp.setType(FieldType.DATETIME);
        fields.add(timestamp);

        DataSourceTextField severity = new DataSourceTextField("severity", "Severity");
        fields.add(severity);

        DataSourceTextField details = new HighlightingDatasourceTextField("details", "Details");
        fields.add(details);

        DataSourceTextField sourceLocation = new DataSourceTextField("source", "Source Location");
        fields.add(sourceLocation);

        return fields;
    }

    @Override
    public EventComposite copyValues(ListGridRecord from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(EventComposite from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("id", from.getEventId());
        record.setAttribute("timestamp", from.getTimestamp());
        record.setAttribute("details", from.getEventDetail());
        record.setAttribute("severity", from.getSeverity().name());
        record.setAttribute("source", from.getSourceLocation());

        return record;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        EventCriteria criteria = getFetchCriteria(request);

        GWTServiceLookup.getEventService().findEventCompositesByCriteria(criteria,
            new AsyncCallback<PageList<EventComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load event data", caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                }

                public void onSuccess(PageList<EventComposite> result) {
                    ListGridRecord[] records = buildRecords(result);
                    highlightFilterMatches(request, records);
                    response.setData(records);
                    response.setTotalRows(result.getTotalSize());
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @SuppressWarnings("unchecked")
    protected EventCriteria getFetchCriteria(final DSRequest request) {
        EventCriteria criteria = new EventCriteria();

        PageControl pageControl = getPageControl(request);
        if (pageControl.getOrderingFields().isEmpty()) {
            criteria.addSortTimestamp(PageOrdering.DESC); // default sort
        } else {
            criteria.setPageControl(pageControl);
        }

        // TODO: This call is broken in 2.2, http://code.google.com/p/smartgwt/issues/detail?id=490
        // when using AdvancedCriteria
        Map<String, Object> criteriaMap = request.getCriteria().getValues();

        criteria.addFilterSourceName((String) criteriaMap.get("source"));
        criteria.addFilterDetail((String) criteriaMap.get("details"));

        if (criteriaMap.get("severities") != null) {
            String[] severityStrings = criteriaMap.get("severities").toString().split(",");
            EventSeverity[] severities = new EventSeverity[severityStrings.length];
            int i = 0;
            for (String nextSeverity : severityStrings) {
                severities[i++] = EventSeverity.valueOf(nextSeverity);
            }
            criteria.addFilterSeverities(severities);
        }

        criteria.addFilterEntityContext(entityContext);

        return criteria;
    }
}
