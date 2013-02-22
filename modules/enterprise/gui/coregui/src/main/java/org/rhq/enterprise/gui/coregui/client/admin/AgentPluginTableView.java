/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.admin.AgentPluginTableView.AgentPluginDataSource;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.components.upload.PluginFileUploadForm;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedIButton;

/**
 * @author John Mazzitelli
 */
public class AgentPluginTableView extends TableSection<AgentPluginDataSource> {

    public static final ViewName VIEW_ID = new ViewName("AgentPlugins", MSG.view_adminConfig_agentPlugins(),
        IconEnum.PLUGIN);
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_LASTUPDATE = "lastUpdate";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_DEPLOYED = "deployed";
    private static final String FIELD_VERSION = "version";

    private boolean showDeleted = false;

    public AgentPluginTableView() {
        super(null);
        setHeight100();
        setWidth100();
        setDataSource(new AgentPluginDataSource());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);

        addTableAction(MSG.common_button_enable(), MSG.common_msg_areYouSure(), new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(ListGridRecord[] selections, Object actionValue) {
                int[] selectedIds = getSelectedIds(selections);
                GWTServiceLookup.getPluginService().enableAgentPlugins(selectedIds,
                    new AsyncCallback<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> result) {
                            Message msg = new Message(MSG.view_admin_plugins_enabledAgentPlugins(result.toString()),
                                Severity.Info);
                            CoreGUI.getMessageCenter().notify(msg);
                            refresh();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler()
                                .handleError(
                                    MSG.view_admin_plugins_enabledAgentPluginsFailure() + " " + caught.getMessage(),
                                    caught);
                            refreshTableInfo();
                        }
                    });
            }
        });

        addTableAction(MSG.common_button_disable(), new AuthorizedTableAction(this, TableActionEnablement.ANY,
            Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                ArrayList<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_admin_plugins_agentDisableConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            GWTServiceLookup.getPluginService().disableAgentPlugins(selectedIds,
                                new AsyncCallback<ArrayList<String>>() {
                                    @Override
                                    public void onSuccess(ArrayList<String> result) {
                                        Message msg = new Message(MSG.view_admin_plugins_disabledAgentPlugins(result
                                            .toString()), Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_admin_plugins_disabledAgentPluginsFailure() + " "
                                                + caught.getMessage(), caught);
                                        refreshTableInfo();
                                    }
                                });
                        } else {
                            refreshTableInfo();
                        }
                    }
                });
            }
        });

        addTableAction(MSG.common_button_delete(), new AuthorizedTableAction(this, TableActionEnablement.ANY,
            Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                ArrayList<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_admin_plugins_agentDeleteConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            GWTServiceLookup.getPluginService().deleteAgentPlugins(selectedIds,
                                new AsyncCallback<ArrayList<String>>() {
                                    @Override
                                    public void onSuccess(ArrayList<String> result) {
                                        Message msg = new Message(MSG.view_admin_plugins_deletedAgentPlugins(result
                                            .toString()), Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_admin_plugins_deletedAgentPluginsFailure() + " "
                                                + caught.getMessage(), caught);
                                        refreshTableInfo();
                                    }
                                });
                        } else {
                            refreshTableInfo();
                        }
                    }
                });
            }
        });

        addTableAction(MSG.common_button_purge(), MSG.common_msg_areYouSure(), new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public boolean isEnabled(ListGridRecord[] selection) {
                if (showDeleted) {
                    return super.isEnabled(selection);
                } else {
                    return false; // we aren't showing deleted plugins, so there is no plugin shown that can be purged anyway
                }
            }

            public void executeAction(ListGridRecord[] selections, Object actionValue) {
                int[] selectedIds = getSelectedIds(selections);
                GWTServiceLookup.getPluginService().purgeAgentPlugins(selectedIds,
                    new AsyncCallback<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> result) {
                            Message msg = new Message(MSG.view_admin_plugins_purgedAgentPlugins(result.toString()),
                                Severity.Info);
                            CoreGUI.getMessageCenter().notify(msg);
                            refresh();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_admin_plugins_purgedAgentPluginsFailure() + " " + caught.getMessage(), caught);
                            refreshTableInfo();
                        }
                    });
            }
        });

        IButton scanForUpdatesButton = new EnhancedIButton(MSG.view_admin_plugins_scan());
        scanForUpdatesButton.setAutoFit(true);
        scanForUpdatesButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                GWTServiceLookup.getPluginService().scanAndRegister(new AsyncCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Message msg = new Message(MSG.view_admin_plugins_scanComplete(), Severity.Info);
                        CoreGUI.getMessageCenter().notify(msg);
                        refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_admin_plugins_scanFailure() + " " + caught.getMessage(), caught);
                        refreshTableInfo();
                    }
                });
            }
        });

        final IButton showDeletedButton = new EnhancedIButton(MSG.view_admin_plugins_showDeleted());
        showDeletedButton.setAutoFit(true);
        showDeletedButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                showDeleted = !showDeleted;
                if (showDeleted) {
                    showDeletedButton.setTitle(MSG.view_admin_plugins_hideDeleted());
                    getListGrid().showField(FIELD_DEPLOYED);
                } else {
                    showDeletedButton.setTitle(MSG.view_admin_plugins_showDeleted());
                    getListGrid().hideField(FIELD_DEPLOYED);
                }
                refresh();
            }
        });

        PluginFileUploadForm pluginUploadForm = new PluginFileUploadForm(MSG.view_admin_plugins_upload(), true);

        addExtraWidget(scanForUpdatesButton, true);
        addExtraWidget(showDeletedButton, true);
        addExtraWidget(pluginUploadForm, true);

        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new AgentPluginDetailView(id);
    }

    private int[] getSelectedIds(ListGridRecord[] selections) {
        if (selections == null) {
            return new int[0];
        }
        int[] ids = new int[selections.length];
        int i = 0;
        for (ListGridRecord selection : selections) {
            ids[i++] = selection.getAttributeAsInt(FIELD_ID);
        }
        return ids;
    }

    private ArrayList<String> getSelectedNames(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        ArrayList<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_NAME));
        }
        return ids;
    }

    public class AgentPluginDataSource extends RPCDataSource<Plugin, Criteria> {

        public AgentPluginDataSource() {
            super();
            List<DataSourceField> fields = addDataSourceFields();
            addFields(fields);
        }

        @Override
        protected List<DataSourceField> addDataSourceFields() {
            List<DataSourceField> fields = super.addDataSourceFields();
            DataSourceField idField = new DataSourceIntegerField(FIELD_ID, MSG.common_title_id(), 50);
            idField.setPrimaryKey(true);
            idField.setHidden(true);
            fields.add(idField);
            return fields;
        }

        public List<ListGridField> getListGridFields() {
            List<ListGridField> fields = new ArrayList<ListGridField>();

            ListGridField idField = new ListGridField(FIELD_ID, MSG.common_title_id());
            idField.setHidden(true);
            fields.add(idField);

            ListGridField nameField = new ListGridField(FIELD_NAME, MSG.common_title_name());
            fields.add(nameField);

            ListGridField descriptionField = new ListGridField(FIELD_DESCRIPTION, MSG.common_title_description());
            fields.add(descriptionField);

            ListGridField lastUpdateField = new ListGridField(FIELD_LASTUPDATE, MSG.common_title_lastUpdated());
            TimestampCellFormatter.prepareDateField(lastUpdateField);
            fields.add(lastUpdateField);

            ListGridField enabledField = new ListGridField(FIELD_ENABLED, MSG.common_title_enabled());
            enabledField.setType(ListGridFieldType.IMAGE);
            enabledField.setAlign(Alignment.CENTER);
            fields.add(enabledField);

            ListGridField deployedField = new ListGridField(FIELD_DEPLOYED, MSG.view_admin_plugins_deployed());
            deployedField.setType(ListGridFieldType.IMAGE);
            deployedField.setAlign(Alignment.CENTER);
            deployedField.setHidden(true);
            fields.add(deployedField);

            ListGridField versionField = new ListGridField(FIELD_VERSION, MSG.common_title_version());
            versionField.setHidden(true);
            fields.add(versionField);

            idField.setWidth(100);
            nameField.setWidth("30%");
            descriptionField.setWidth("*");
            lastUpdateField.setWidth("20%");
            enabledField.setWidth(65);
            deployedField.setWidth(75);
            versionField.setWidth(100);

            return fields;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
            GWTServiceLookup.getPluginService().getAgentPlugins(showDeleted, new AsyncCallback<ArrayList<Plugin>>() {
                public void onSuccess(ArrayList<Plugin> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.size());
                    processResponse(request.getRequestId(), response);
                }

                @Override
                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), t);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
        }

        @Override
        public Plugin copyValues(Record from) {
            // don't need this
            return null;
        }

        @Override
        public ListGridRecord copyValues(Plugin from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_ID, from.getId());
            record.setAttribute(FIELD_NAME, from.getDisplayName());
            record.setAttribute(FIELD_DESCRIPTION, from.getDescription());
            record.setAttribute(FIELD_LASTUPDATE, new Date(from.getMtime()));
            record.setAttribute(FIELD_ENABLED, ImageManager.getAvailabilityIcon(from.isEnabled()));
            record.setAttribute(FIELD_DEPLOYED,
                ImageManager.getAvailabilityIcon(from.getStatus() == PluginStatusType.INSTALLED));
            record.setAttribute(FIELD_VERSION, from.getVersion());
            return record;
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            // we don't use criteria for this datasource, just return null
            return null;
        }
    }

}
