/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.topology;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.composite.AffinityGroupCountComposite;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Jirka Kremser
 *
 */
public class AffinityGroupWithCountsDatasource extends RPCDataSource<AffinityGroupCountComposite, Criteria> {

    public enum Fields {

        FIELD_ID("id", CoreGUI.getMessages().common_title_id()),

        FIELD_NAME("name", CoreGUI.getMessages().common_title_name()),

        FIELD_AGENT_COUNT("agentCount", CoreGUI.getMessages().view_adminTopology_affinityGroups_agentCount()),

        FIELD_SERVER_COUNT("serverCount", CoreGUI.getMessages().view_adminTopology_affinityGroups_serverCount());

        private String propertyName;
        private String title;

        private Fields(String propertyName, String title) {
            this.propertyName = propertyName;
            this.title = title;
        }

        public String propertyName() {
            return propertyName;
        }

        public String title() {
            return title;
        }

        public ListGridField getListGridField() {
            return new ListGridField(propertyName, title);
        }

        public ListGridField getListGridField(String width) {
            ListGridField field = new ListGridField(propertyName, title);
            field.setWidth(width);
            return field;
        }
    }

    public AffinityGroupWithCountsDatasource() {
        super();
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();
        DataSourceField idField = new DataSourceIntegerField(Fields.FIELD_ID.propertyName(), Fields.FIELD_ID.title(),
            50);
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);
        return fields;
    }

    public List<ListGridField> getListGridFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = Fields.FIELD_ID.getListGridField();
        idField.setHidden(true);
        fields.add(idField);
        fields.add(Fields.FIELD_NAME.getListGridField("*"));
        fields.add(Fields.FIELD_AGENT_COUNT.getListGridField("200"));
        fields.add(Fields.FIELD_SERVER_COUNT.getListGridField("200"));

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
        final PageControl pc = getPageControl(request);

        GWTServiceLookup.getTopologyService().getAffinityGroupCountComposites(pc,
            new AsyncCallback<PageList<AffinityGroupCountComposite>>() {
                public void onSuccess(PageList<AffinityGroupCountComposite> result) {
                    response.setData(buildRecords(result));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }

                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_message_fetchAgroupsFail(), t);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.         
        PageControl pageControl = new PageControl(0, getDataPageSize());

        // Initialize sorting.
        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");
            for (String sort : sorts) {
                PageOrdering ordering = (sort.startsWith("-")) ? PageOrdering.DESC : PageOrdering.ASC;
                String columnName = (ordering == PageOrdering.DESC) ? sort.substring(1) : sort;
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }

        return pageControl;
    }

    @Override
    public AffinityGroupCountComposite copyValues(Record from) {
        throw new UnsupportedOperationException("AffinityGroupWithCountsDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(AffinityGroupCountComposite from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute(Fields.FIELD_ID.propertyName(), from.getAffinityGroup().getId());
        record.setAttribute(Fields.FIELD_NAME.propertyName(), from.getAffinityGroup().getName());
        record.setAttribute(Fields.FIELD_AGENT_COUNT.propertyName(), from.getAgentCount());
        record.setAttribute(Fields.FIELD_SERVER_COUNT.propertyName(), from.getServerCount());
        return record;
    }

    @Override
    protected Criteria getFetchCriteria(DSRequest request) {
        // we don't use criteria for this datasource, just return null
        return null;
    }
}
