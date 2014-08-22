/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.coregui.client.inventory.groups.detail.configuration;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * DataSource implementation for HistoryGroupResourceConfigurationTable.
 *
 * @author John Mazzitelli
 * @author Simeon Pinder
 */
public class GroupResourceConfigurationDataSource extends
    RPCDataSource<GroupResourceConfigurationUpdate, GroupResourceConfigurationUpdateCriteria> {

    private int groupId;

    public class Field {
        public static final String ID = "id";
        public static final String DATECREATED = "createdTime";
        public static final String LASTUPDATED = "modifiedTime";
        public static final String STATUS = "status";
        public static final String USER = "subjectName";
        public static final String OBJECT = "object";
    }

    public GroupResourceConfigurationDataSource(int groupId) {
        super();
        this.groupId = groupId;
    }

    @Override
    public GroupResourceConfigurationUpdate copyValues(Record from) {
        return (GroupResourceConfigurationUpdate) from.getAttributeAsObject(Field.OBJECT);
    }

    @Override
    public ListGridRecord copyValues(GroupResourceConfigurationUpdate from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(Field.ID, from.getId());
        record.setAttribute(Field.DATECREATED, new Date(from.getCreatedTime()));
        record.setAttribute(Field.LASTUPDATED, new Date(from.getModifiedTime()));
        record.setAttribute(Field.STATUS, from.getStatus().name());
        record.setAttribute(Field.USER, from.getSubjectName());

        record.setAttribute(Field.OBJECT, from);

        return record;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final GroupResourceConfigurationUpdateCriteria criteria) {
        ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

        configurationService.findGroupResourceConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<GroupResourceConfigurationUpdate>>() {

                @Override
                public void onSuccess(PageList<GroupResourceConfigurationUpdate> result) {
                    response.setData(buildRecords(result));
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_group_resConfig_table_failFetch(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected GroupResourceConfigurationUpdateCriteria getFetchCriteria(final DSRequest request) {
        GroupResourceConfigurationUpdateCriteria criteria = new GroupResourceConfigurationUpdateCriteria();

        ArrayList<Integer> groupIds = new ArrayList<Integer>(1);
        groupIds.add(this.groupId);
        criteria.addFilterResourceGroupIds(groupIds);

        PageControl pageControl = getPageControl(request);
        pageControl.addDefaultOrderingField(Field.ID, PageOrdering.DESC);
        criteria.setPageControl(pageControl);

        return criteria;
    }

}
