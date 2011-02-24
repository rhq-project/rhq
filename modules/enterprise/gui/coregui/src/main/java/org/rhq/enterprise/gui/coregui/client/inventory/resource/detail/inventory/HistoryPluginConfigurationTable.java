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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.configuration.AbstractConfigurationUpdate;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * Table showing resource plugin configuration history.
 *
 * @author John Mazzitelli
 */
public class HistoryPluginConfigurationTable extends Table<HistoryPluginConfigurationTable.DataSource> {
    private final Resource resource;
    private final ResourcePermission resourcePerms;

    public HistoryPluginConfigurationTable(String locatorId, ResourceComposite composite) {
        super(locatorId, MSG.view_connectionSettingsHistory_table_title());
        this.resource = composite.getResource();
        this.resourcePerms = composite.getResourcePermission();

        setDataSource(new DataSource());
    }

    @Override
    protected void configureTable() {
        ListGridField fieldId = new ListGridField(DataSource.ID, MSG.common_title_version());
        ListGridField fieldDateCreated = new ListGridField(DataSource.DATE_CREATED, MSG.common_title_dateCreated());
        ListGridField fieldLastUpdated = new ListGridField(DataSource.LAST_UPDATED, MSG.common_title_lastUpdated());
        ListGridField fieldUser = new ListGridField(DataSource.USER, MSG.common_title_user());
        ListGridField fieldStatus = new ListGridField(DataSource.STATUS, MSG.common_title_status());

        fieldId.setWidth("10%");
        fieldDateCreated.setWidth("35%");
        fieldLastUpdated.setWidth("35%");
        fieldUser.setWidth("*");
        fieldStatus.setWidth("10%");

        fieldStatus.setType(ListGridFieldType.ICON);
        HashMap<String, String> statusIcons = new HashMap<String, String>(4);
        statusIcons.put(ConfigurationUpdateStatus.SUCCESS.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.SUCCESS));
        statusIcons.put(ConfigurationUpdateStatus.FAILURE.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.FAILURE));
        statusIcons.put(ConfigurationUpdateStatus.INPROGRESS.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.INPROGRESS));
        statusIcons.put(ConfigurationUpdateStatus.NOCHANGE.name(), ImageManager
            .getPluginConfigurationIcon(ConfigurationUpdateStatus.NOCHANGE));
        fieldStatus.setValueIcons(statusIcons);
        fieldStatus.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                final Window winModal = new LocatableWindow(HistoryPluginConfigurationTable.this
                    .extendLocatorId("statusDetailsWin"));
                winModal.setTitle(MSG.view_connectionSettingsHistory_table_statusDetails());
                winModal.setOverflow(Overflow.VISIBLE);
                winModal.setShowMinimizeButton(false);
                winModal.setShowMaximizeButton(true);
                winModal.setIsModal(true);
                winModal.setShowModalMask(true);
                winModal.setAutoSize(true);
                winModal.setAutoCenter(true);
                winModal.setShowResizer(true);
                winModal.setCanDragResize(true);
                winModal.centerInPage();
                winModal.addCloseClickHandler(new CloseClickHandler() {
                    @Override
                    public void onCloseClick(CloseClientEvent event) {
                        winModal.markForDestroy();
                    }
                });

                LocatableHTMLPane htmlPane = new LocatableHTMLPane(HistoryPluginConfigurationTable.this
                    .extendLocatorId("statusDetailsPane"));
                htmlPane.setMargin(10);
                htmlPane.setDefaultWidth(500);
                htmlPane.setDefaultHeight(400);
                htmlPane.setContents(getStatusHtmlString(event.getRecord()));
                winModal.addItem(htmlPane);
                winModal.show();
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
        listGrid.setFields(fieldId, fieldDateCreated, fieldLastUpdated, fieldUser, fieldStatus);

        addTableAction(extendLocatorId("deleteAction"), MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(this.resourcePerms.isInventory() ? TableActionEnablement.ANY
                : TableActionEnablement.NEVER) {

                @Override
                public void executeAction(final ListGridRecord[] selection, Object actionValue) {
                    if (selection == null || selection.length == 0) {
                        return;
                    }

                    ConfigurationGWTServiceAsync service = GWTServiceLookup.getConfigurationService();
                    int[] updateIds = new int[selection.length];
                    int i = 0;
                    for (ListGridRecord record : selection) {
                        updateIds[i++] = record.getAttributeAsInt(DataSource.ID).intValue();
                    }

                    service.purgePluginConfigurationUpdates(updateIds, true, new AsyncCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            refresh();
                            Message message = new Message(
                                MSG.view_connectionSettingsHistory_table_deleteSuccessful(String
                                    .valueOf(selection.length)), Message.Severity.Info, EnumSet.of(
                                    Message.Option.Transient, Message.Option.Sticky));
                            CoreGUI.getMessageCenter().notify(message);
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_connectionSettingsHistory_table_deleteFailure(), caught);
                        }
                    });
                }
            });
    }

    private String getStatusHtmlString(Record record) {
        String html = null;
        AbstractConfigurationUpdate obj = (AbstractConfigurationUpdate) record.getAttributeAsObject(DataSource.OBJECT);
        switch (obj.getStatus()) {
        case SUCCESS: {
            html = MSG.view_connectionSettingsHistory_table_statusSuccess();
            break;
        }
        case INPROGRESS: {
            html = "<p>" + MSG.view_connectionSettingsHistory_table_statusInprogress() + "</p>";
            break;
        }
        case NOCHANGE: {
            html = MSG.view_connectionSettingsHistory_table_statusNochange();
            break;
        }
        case FAILURE: {
            html = obj.getErrorMessage();
            if (html == null) {
                html = "<p>" + MSG.view_connectionSettingsHistory_table_statusFailure() + "</p>";
            } else {
                if (html.length() > 80) {
                    // this was probably an error stack trace, snip it so the tooltip isn't too big
                    html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                        + MSG.view_connectionSettingsHistory_table_clickStatusIcon() + "</p>";
                } else {
                    html = "<pre>" + html + "</pre>";
                }
            }
            break;
        }
        }
        return html;
    }

    class DataSource extends RPCDataSource<PluginConfigurationUpdate> {

        public static final String ID = "id";
        public static final String DATE_CREATED = "dateCreated";
        public static final String LAST_UPDATED = "lastUpdated";
        public static final String USER = "user";
        public static final String STATUS = "status";
        public static final String OBJECT = "object";

        @Override
        public PluginConfigurationUpdate copyValues(Record from) {
            return (PluginConfigurationUpdate) from.getAttributeAsObject(DataSource.OBJECT);
        }

        @Override
        public ListGridRecord copyValues(PluginConfigurationUpdate from) {
            ListGridRecord record = new ListGridRecord();

            record.setAttribute(DataSource.ID, from.getId());
            record.setAttribute(DataSource.DATE_CREATED, new Date(from.getCreatedTime()));
            record.setAttribute(DataSource.LAST_UPDATED, new Date(from.getModifiedTime()));
            record.setAttribute(DataSource.USER, from.getSubjectName());
            record.setAttribute(DataSource.STATUS, from.getStatus().name());

            record.setAttribute(DataSource.OBJECT, from);

            return record;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

            PluginConfigurationUpdateCriteria criteria = new PluginConfigurationUpdateCriteria();
            criteria.addFilterResourceIds(HistoryPluginConfigurationTable.this.resource.getId());

            configurationService.findPluginConfigurationUpdatesByCriteria(criteria,
                new AsyncCallback<PageList<PluginConfigurationUpdate>>() {

                    @Override
                    public void onSuccess(PageList<PluginConfigurationUpdate> result) {
                        response.setData(buildRecords(result));
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_connectionSettingsHistory_table_failFetch(),
                            caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }
    }
}
