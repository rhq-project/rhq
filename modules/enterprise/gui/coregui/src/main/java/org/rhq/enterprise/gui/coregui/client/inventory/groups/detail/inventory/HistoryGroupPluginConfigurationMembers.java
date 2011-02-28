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

import java.util.Date;
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
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * Shows a table of individual resource members that belonged to the group when the group plugin configuration was updated.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupPluginConfigurationMembers extends LocatableVLayout {
    private final ResourceGroup group;
    private final ResourcePermission groupPerms;
    private final int groupUpdateId;

    public HistoryGroupPluginConfigurationMembers(String locatorId, ResourceGroupComposite groupComposite, int updateId) {
        super(locatorId);
        this.group = groupComposite.getResourceGroup();
        this.groupPerms = groupComposite.getResourcePermission();
        this.groupUpdateId = updateId;

        setMargin(5);
        setMembersMargin(5);
        String backPath = LinkManager.getGroupPluginConfigurationUpdateHistoryLink(this.group.getId(), null);
        BackButton backButton = new BackButton(extendLocatorId("BackButton"), MSG.view_tableSection_backButton(),
            backPath);
        addMember(backButton);

        MembersTable table = new MembersTable(extendLocatorId("Table"));
        addMember(table);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
    }

    private class MembersTable extends Table {
        public MembersTable(String locatorId) {
            super(locatorId, MSG.view_group_pluginConfig_members_title());
            setDataSource(new DataSource());
        }

        @Override
        protected void configureTable() {
            ListGridField fieldResource = new ListGridField("resourceLink", MSG.common_title_resource());
            ListGridField fieldDateCreated = new ListGridField("dateCreated", MSG.common_title_dateCreated());
            ListGridField fieldLastUpdated = new ListGridField("lastUpdated", MSG.common_title_lastUpdated());
            ListGridField fieldUser = new ListGridField("user", MSG.common_title_user());
            ListGridField fieldStatus = new ListGridField("status", MSG.common_title_status());

            fieldResource.setWidth("*");
            fieldDateCreated.setWidth("15%");
            fieldLastUpdated.setWidth("15%");
            fieldUser.setWidth("10%");
            fieldStatus.setWidth("10%");

            fieldResource.setType(ListGridFieldType.LINK);
            fieldResource.setTarget("_self");

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
                    final Window winModal = new LocatableWindow(HistoryGroupPluginConfigurationMembers.this
                        .extendLocatorId("statusDetailsWin"));
                    winModal.setTitle(MSG.view_group_pluginConfig_members_statusDetails());
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

                    LocatableHTMLPane htmlPane = new LocatableHTMLPane(HistoryGroupPluginConfigurationMembers.this
                        .extendLocatorId("statusDetailsPane"));
                    htmlPane.setMargin(10);
                    htmlPane.setDefaultWidth(500);
                    htmlPane.setDefaultHeight(400);
                    htmlPane.setContents("<pre>" + getStatusHtmlString(event.getRecord()) + "</pre>");
                    winModal.addItem(htmlPane);
                    winModal.show();
                }
            });
            fieldStatus.setShowHover(true);
            fieldStatus.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String html = getStatusHtmlString(record);
                    if (html.length() > 80) {
                        // this was probably an error stack trace, snip it so the tooltip isn't too big
                        html = "<pre>" + html.substring(0, 80) + "...</pre><p>"
                            + MSG.view_group_pluginConfig_table_clickStatusIcon() + "</p>";
                    }
                    return html;
                }
            });

            ListGrid listGrid = getListGrid();
            listGrid.setFields(fieldResource, fieldDateCreated, fieldLastUpdated, fieldUser, fieldStatus);

            listGrid.setLinkTextProperty("resourceName");

        }

        private String getStatusHtmlString(Record record) {
            String html = null;
            AbstractConfigurationUpdate obj = (AbstractConfigurationUpdate) record.getAttributeAsObject("object");
            switch (obj.getStatus()) {
            case SUCCESS: {
                html = MSG.view_group_pluginConfig_members_statusSuccess();
                break;
            }
            case INPROGRESS: {
                html = MSG.view_group_pluginConfig_members_statusInprogress();
                break;
            }
            case NOCHANGE: {
                html = MSG.view_group_pluginConfig_members_statusNochange();
                break;
            }
            case FAILURE: {
                html = obj.getErrorMessage();
                if (html == null) {
                    html = MSG.view_group_pluginConfig_members_statusFailure();
                }
                break;
            }
            }
            return html;
        }

        private class DataSource extends RPCDataSource<PluginConfigurationUpdate> {

            @Override
            public PluginConfigurationUpdate copyValues(Record from) {
                return (PluginConfigurationUpdate) from.getAttributeAsObject("object");
            }

            @Override
            public ListGridRecord copyValues(PluginConfigurationUpdate from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute("id", from.getId());
                record.setAttribute("resourceLink", LinkManager.getResourceLink(from.getResource().getId()));
                record.setAttribute("resourceName", from.getResource().getName());
                record.setAttribute("dateCreated", new Date(from.getCreatedTime()));
                record.setAttribute("lastUpdated", new Date(from.getModifiedTime()));
                record.setAttribute("user", from.getSubjectName());
                record.setAttribute("status", from.getStatus().name());

                record.setAttribute("object", from);

                return record;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response) {
                ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

                PluginConfigurationUpdateCriteria criteria = new PluginConfigurationUpdateCriteria();
                criteria.addFilterGroupConfigurationUpdateId(HistoryGroupPluginConfigurationMembers.this.groupUpdateId);
                // TODO need to disambiguate resources
                criteria.fetchResource(true);

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
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_group_pluginConfig_members_fetchFailure(String
                                    .valueOf(HistoryGroupPluginConfigurationMembers.this.groupUpdateId)), caught);
                            response.setStatus(DSResponse.STATUS_FAILURE);
                            processResponse(request.getRequestId(), response);
                        }
                    });
            }
        }
    }
}
