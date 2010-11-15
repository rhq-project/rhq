/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * A view for group plugin configuration history.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupPluginConfigurationView extends Table {
    private final ResourceGroup group;
    private final ResourcePermission groupPerms;

    public HistoryGroupPluginConfigurationView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId, "Group Connection Settings History");
        this.group = groupComposite.getResourceGroup();
        this.groupPerms = groupComposite.getResourcePermission();

        setDataSource(new DataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField fieldId = new ListGridField("id", "Version");
        ListGridField fieldDateCreated = new ListGridField("dateCreated", "Date Created");
        ListGridField fieldLastUpdated = new ListGridField("lastUpdated", "Last Updated");
        ListGridField fieldStatus = new ListGridField("status", "Status");
        fieldStatus.setType(ListGridFieldType.ICON);
        HashMap<String, String> statusIcons = new HashMap<String, String>(4);
        statusIcons.put(ConfigurationUpdateStatus.SUCCESS.name(), "/images/icons/Connection_ok_16.png");
        statusIcons.put(ConfigurationUpdateStatus.FAILURE.name(), "/images/icons/Connection_failed_16.png");
        statusIcons.put(ConfigurationUpdateStatus.INPROGRESS.name(), "/images/icons/Connection_inprogress_16.png");
        statusIcons.put(ConfigurationUpdateStatus.NOCHANGE.name(), "/images/icons/Connection_16.png");
        fieldStatus.setValueIcons(statusIcons);

        ListGrid listGrid = getListGrid();
        listGrid.setFields(fieldId, fieldDateCreated, fieldLastUpdated, fieldStatus);

        addTableAction(extendLocatorId("deleteAction"), "Delete", "Are You Sure?", new AbstractTableAction(
            this.groupPerms.isInventory() ? TableActionEnablement.ANY : TableActionEnablement.NEVER) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                // TODO Auto-generated method stub
                SC.say("TODO: deleting...");
            }
        });

        addTableAction(extendLocatorId("viewSettingsAction"), "View Settings", new AbstractTableAction(
            TableActionEnablement.SINGLE) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                // TODO Auto-generated method stub
                SC.say("TODO: view settings...");
            }
        });

        addTableAction(extendLocatorId("viewMemberHistoryAction"), "View Member History", new AbstractTableAction(
            TableActionEnablement.SINGLE) {
            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                // TODO Auto-generated method stub
                SC.say("TODO: view member history...");
            }
        });
    }

    private class DataSource extends RPCDataSource<GroupPluginConfigurationUpdate> {

        @Override
        public GroupPluginConfigurationUpdate copyValues(Record from) {
            return (GroupPluginConfigurationUpdate) from.getAttributeAsObject("object");
        }

        @Override
        public ListGridRecord copyValues(GroupPluginConfigurationUpdate from) {
            ListGridRecord record = new ListGridRecord();

            record.setAttribute("id", from.getId());
            record.setAttribute("dateCreated", new Date(from.getCreatedTime()));
            record.setAttribute("lastUpdated", new Date(from.getModifiedTime()));
            record.setAttribute("status", from.getStatus().name());

            record.setAttribute("object", from);

            return record;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

            GroupPluginConfigurationUpdateCriteria criteria = new GroupPluginConfigurationUpdateCriteria();
            ArrayList<Integer> groupList = new ArrayList<Integer>(1);
            groupList.add(HistoryGroupPluginConfigurationView.this.group.getId());
            criteria.addFilterResourceGroupIds(groupList);

            configurationService.findGroupPluginConfigurationUpdatesByCriteria(criteria,
                new AsyncCallback<PageList<GroupPluginConfigurationUpdate>>() {

                    @Override
                    public void onSuccess(PageList<GroupPluginConfigurationUpdate> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to get group plugin config history", caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }
    }
}
