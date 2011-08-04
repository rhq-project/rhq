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
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.CellFormatter;
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
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ErrorMessageWindow;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * Shows a table of individual resource members that belonged to the group when the group configuration was updated.
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

    private class MembersTable extends Table<MembersTable.DataSource> {
        public MembersTable(String locatorId) {
            super(locatorId, MSG.view_group_pluginConfig_members_title());
            setDataSource(new DataSource());
        }

        @Override
        protected void configureTable() {
            ListGridField fieldResource = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            fieldResource.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    String url = LinkManager
                        .getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                    return SeleniumUtility.getLocatableHref(url, o.toString(), null);
                }
            });
            fieldResource.setShowHover(true);
            fieldResource.setHoverCustomizer(new HoverCustomizer() {

                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });

            ListGridField fieldAncestry = AncestryUtil.setupAncestryListGridField();
            ListGridField fieldDateCreated = new ListGridField(DataSource.Field.DATECREATED, MSG
                .common_title_dateCreated());
            TimestampCellFormatter.prepareDateField(fieldDateCreated);
            ListGridField fieldLastUpdated = new ListGridField(DataSource.Field.LASTUPDATED, MSG
                .common_title_lastUpdated());
            TimestampCellFormatter.prepareDateField(fieldLastUpdated);
            ListGridField fieldStatus = new ListGridField(DataSource.Field.STATUS, MSG.common_title_status());
            ListGridField fieldUser = new ListGridField(DataSource.Field.USER, MSG.common_title_user());

            fieldResource.setWidth("30%");
            fieldAncestry.setWidth("*");
            fieldDateCreated.setWidth(150);
            fieldLastUpdated.setWidth(150);
            fieldStatus.setWidth("10%");
            fieldUser.setWidth("10%");

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
                    new ErrorMessageWindow("statusDetailsWin", MSG.view_group_pluginConfig_members_statusDetails(),
                        "<pre>" + getStatusHtmlString(event.getRecord()) + "</pre>").show();
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
            listGrid
                .setFields(fieldResource, fieldAncestry, fieldDateCreated, fieldLastUpdated, fieldStatus, fieldUser);
        }

        private String getStatusHtmlString(Record record) {
            String html = null;
            AbstractConfigurationUpdate obj = (AbstractConfigurationUpdate) record
                .getAttributeAsObject(DataSource.Field.OBJECT);
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

        private class DataSource extends RPCDataSource<PluginConfigurationUpdate, PluginConfigurationUpdateCriteria> {

            public class Field {
                public static final String ID = "id";
                public static final String DATECREATED = "dateCreated";
                public static final String LASTUPDATED = "lastUpdated";
                public static final String STATUS = "status";
                public static final String USER = "user";
                public static final String OBJECT = "object";
            }

            @Override
            public PluginConfigurationUpdate copyValues(Record from) {
                return (PluginConfigurationUpdate) from.getAttributeAsObject(Field.OBJECT);
            }

            @Override
            public ListGridRecord copyValues(PluginConfigurationUpdate from) {
                ListGridRecord record = new ListGridRecord();

                record.setAttribute(Field.ID, from.getId());
                record.setAttribute(Field.DATECREATED, new Date(from.getCreatedTime()));
                record.setAttribute(Field.LASTUPDATED, new Date(from.getModifiedTime()));
                record.setAttribute(Field.USER, from.getSubjectName());
                record.setAttribute(Field.STATUS, from.getStatus().name());

                // for ancestry handling
                Resource resource = from.getResource();
                record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
                record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
                record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
                record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

                record.setAttribute(Field.OBJECT, from);

                return record;
            }

            @Override
            protected void executeFetch(final DSRequest request, final DSResponse response,
                final PluginConfigurationUpdateCriteria criteria) {

                ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

                configurationService.findPluginConfigurationUpdatesByCriteria(criteria,
                    new AsyncCallback<PageList<PluginConfigurationUpdate>>() {

                        @Override
                        public void onSuccess(final PageList<PluginConfigurationUpdate> result) {
                            HashSet<Integer> typesSet = new HashSet<Integer>();
                            HashSet<String> ancestries = new HashSet<String>();
                            for (PluginConfigurationUpdate update : result) {
                                Resource resource = update.getResource();
                                typesSet.add(resource.getResourceType().getId());
                                ancestries.add(resource.getAncestry());
                            }

                            // In addition to the types of the result resources, get the types of their ancestry
                            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
                            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]),
                                new TypesLoadedCallback() {
                                    @Override
                                    public void onTypesLoaded(Map<Integer, ResourceType> types) {
                                        // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                                        AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                                        Record[] records = buildRecords(result);
                                        for (Record record : records) {
                                            // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                                            // Store the types map off the records so we can build a detailed hover string as needed.                      
                                            record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                                            // Build the decoded ancestry Strings now for display
                                            record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil
                                                .getAncestryValue(record));
                                        }
                                        response.setData(records);
                                        response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                                        processResponse(request.getRequestId(), response);
                                    }
                                });
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

            @Override
            protected PluginConfigurationUpdateCriteria getFetchCriteria(DSRequest request) {
                PluginConfigurationUpdateCriteria criteria = new PluginConfigurationUpdateCriteria();
                criteria.addFilterGroupConfigurationUpdateId(HistoryGroupPluginConfigurationMembers.this.groupUpdateId);
                criteria.fetchResource(true);
                return criteria;
            }
        }
    }
}
