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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ErrorMessageWindow;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Table showing group resource configuration history.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupResourceConfigurationTable extends Table<HistoryGroupResourceConfigurationTable.DataSource> {
    private final ResourceGroup group;
    private final ResourcePermission groupPerms;

    public HistoryGroupResourceConfigurationTable(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId, MSG.view_group_resConfig_table_title());
        this.group = groupComposite.getResourceGroup();
        this.groupPerms = groupComposite.getResourcePermission();

        setDataSource(new DataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField fieldId = new ListGridField(DataSource.Field.ID, MSG.common_title_version());
        ListGridField fieldDateCreated = new ListGridField(DataSource.Field.DATECREATED, MSG.common_title_dateCreated());
        ListGridField fieldLastUpdated = new ListGridField(DataSource.Field.LASTUPDATED, MSG.common_title_lastUpdated());
        ListGridField fieldStatus = new ListGridField(DataSource.Field.STATUS, MSG.common_title_status());
        ListGridField fieldUser = new ListGridField(DataSource.Field.USER, MSG.common_title_user());

        fieldId.setWidth("10%");
        fieldDateCreated.setWidth("35%");
        fieldLastUpdated.setWidth("35%");
        fieldStatus.setWidth("10%");
        fieldUser.setWidth("*");

        fieldStatus.setType(ListGridFieldType.ICON);
        HashMap<String, String> statusIcons = new HashMap<String, String>(4);
        statusIcons.put(ConfigurationUpdateStatus.SUCCESS.name(), ImageManager
            .getResourceConfigurationIcon(ConfigurationUpdateStatus.SUCCESS));
        statusIcons.put(ConfigurationUpdateStatus.FAILURE.name(), ImageManager
            .getResourceConfigurationIcon(ConfigurationUpdateStatus.FAILURE));
        statusIcons.put(ConfigurationUpdateStatus.INPROGRESS.name(), ImageManager
            .getResourceConfigurationIcon(ConfigurationUpdateStatus.INPROGRESS));
        statusIcons.put(ConfigurationUpdateStatus.NOCHANGE.name(), ImageManager
            .getResourceConfigurationIcon(ConfigurationUpdateStatus.NOCHANGE));
        fieldStatus.setValueIcons(statusIcons);
        fieldStatus.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                new ErrorMessageWindow("errWin", MSG.common_title_error(), getStatusHtmlString(event.getRecord()))
                    .show();
            }
        });
        fieldStatus.setShowHover(true);
        fieldStatus.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String html = getStatusHtmlString(record);
                return html;
            }
        });

        ListGrid listGrid = getListGrid();
        listGrid.setFields(fieldId, fieldDateCreated, fieldLastUpdated, fieldStatus, fieldUser);

        addTableAction(extendLocatorId("deleteAction"), MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(this.groupPerms.isInventory() ? TableActionEnablement.ANY
                : TableActionEnablement.NEVER) {

                @Override
                public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                    if (selection == null || selection.length == 0) {
                        return;
                    }

                    ConfigurationGWTServiceAsync service = GWTServiceLookup.getConfigurationService();
                    Integer groupId = HistoryGroupResourceConfigurationTable.this.group.getId();
                    Integer[] updateIds = new Integer[selection.length];
                    int i = 0;
                    for (ListGridRecord record : selection) {
                        updateIds[i++] = record.getAttributeAsInt(DataSource.Field.ID);
                    }

                    service.deleteGroupResourceConfigurationUpdate(groupId, updateIds, new AsyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            refresh();
                            Message message = new Message(MSG.view_group_resConfig_table_deleteSuccessful(String
                                .valueOf(selection.length)), Message.Severity.Info, EnumSet.of(
                                Message.Option.Transient, Message.Option.Sticky));
                            CoreGUI.getMessageCenter().notify(message);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_group_resConfig_table_deleteFailure(),
                                caught);
                        }
                    });
                }
            });

        addTableAction(extendLocatorId("viewSettingsAction"), MSG.view_group_resConfig_table_viewSettings(),
            new AbstractTableAction(TableActionEnablement.SINGLE) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    CoreGUI.goToView(LinkManager.getGroupResourceConfigurationUpdateHistoryLink(
                        HistoryGroupResourceConfigurationTable.this.group.getId(), null)
                        + "/" + selection[0].getAttribute(DataSource.Field.ID) + "/Settings");
                }
            });

        addTableAction(extendLocatorId("viewMemberHistoryAction"), MSG.view_group_resConfig_table_viewMemberHistory(),
            new AbstractTableAction(TableActionEnablement.SINGLE) {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    CoreGUI.goToView(LinkManager.getGroupResourceConfigurationUpdateHistoryLink(
                        HistoryGroupResourceConfigurationTable.this.group.getId(), null)
                        + "/" + selection[0].getAttribute(DataSource.Field.ID) + "/Members");
                }
            });

    }

    private String getStatusHtmlString(Record record) {
        String html = null;
        AbstractConfigurationUpdate obj = (AbstractConfigurationUpdate) record
            .getAttributeAsObject(DataSource.Field.OBJECT);
        switch (obj.getStatus()) {
        case SUCCESS: {
            html = MSG.view_group_resConfig_table_statusSuccess();
            break;
        }
        case INPROGRESS: {
            html = "<p>" + MSG.view_group_resConfig_table_statusInprogress() + "</p><p>"
                + MSG.view_group_resConfig_table_msg1() + "</p>";
            break;
        }
        case NOCHANGE: {
            html = MSG.view_group_resConfig_table_statusNochange();
            break;
        }
        case FAILURE: {
            html = obj.getErrorMessage();
            if (html == null) {
                html = "<p>" + MSG.view_group_resConfig_table_statusFailure() + "</p><p>"
                    + MSG.view_group_resConfig_table_msg1() + "</p>";
            } else {
                if (html.length() > 80) {
                    // this was probably an error stack trace, snip it so the tooltip isn't too big
                    html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                        + MSG.view_group_resConfig_table_clickStatusIcon() + "</p>";
                } else {
                    html = "<pre>" + html + "</pre>";
                }
                html = html + "<p>" + MSG.view_group_resConfig_table_msg1() + "</p>";
            }
            break;
        }
        }
        return html;
    }

    class DataSource extends RPCDataSource<GroupResourceConfigurationUpdate, GroupResourceConfigurationUpdateCriteria> {

        public class Field {
            public static final String ID = "id";
            public static final String DATECREATED = "dateCreated";
            public static final String LASTUPDATED = "lastUpdated";
            public static final String STATUS = "status";
            public static final String USER = "user";
            public static final String OBJECT = "object";
        }

        @Override
        public GroupResourceConfigurationUpdate copyValues(Record from) {
            return (GroupResourceConfigurationUpdate) from.getAttributeAsObject(DataSource.Field.OBJECT);
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
                        response.setTotalRows(result.getTotalSize());
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
            ArrayList<Integer> groupList = new ArrayList<Integer>(1);
            groupList.add(HistoryGroupResourceConfigurationTable.this.group.getId());
            criteria.addFilterResourceGroupIds(groupList);
            return criteria;
        }
    }
}
