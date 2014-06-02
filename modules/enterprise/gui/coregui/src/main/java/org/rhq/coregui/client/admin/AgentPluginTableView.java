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
package org.rhq.coregui.client.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FormLayoutType;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.DateTimeItem;
import com.smartgwt.client.widgets.form.fields.IntegerItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.PopupWindow;
import org.rhq.coregui.client.admin.AgentPluginTableView.AgentPluginDataSource;
import org.rhq.coregui.client.components.form.DurationItem;
import org.rhq.coregui.client.components.form.RadioGroupWithComponentsItem;
import org.rhq.coregui.client.components.form.TimeUnit;
import org.rhq.coregui.client.components.form.UnitType;
import org.rhq.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.components.upload.PluginFileUploadForm;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

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

        IButton updateOnAgentsButton = new EnhancedIButton(MSG.view_admin_plugins_update_on_agents());
        updateOnAgentsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                VLayout layout = new VLayout();
                final PopupWindow w = new PopupWindow(null);

                Canvas nowCanvas = new Canvas();
                nowCanvas.setWidth(1);
                nowCanvas.setHeight(1);


                DynamicForm delayForm = new DynamicForm();
                delayForm.setTitleOrientation(TitleOrientation.TOP);
                TreeSet<TimeUnit> timeUnits = new TreeSet<TimeUnit>();
                timeUnits.add(TimeUnit.SECONDS);
                timeUnits.add(TimeUnit.MINUTES);
                timeUnits.add(TimeUnit.HOURS);
                timeUnits.add(TimeUnit.DAYS);
                final DurationItem startDelay = new DurationItem("duration", "", timeUnits, false, false);

                delayForm.setFields(startDelay);

                DynamicForm scheduleForm = new DynamicForm();

                final DateTimeItem schedule = new DateTimeItem("schedule");
                schedule.setEnforceDate(true);
                schedule.setCenturyThreshold(99);
                schedule.setShowTitle(false);
                schedule.setStartDate(new Date());
                schedule.setUseMask(true);
                schedule.setShowHint(true);

                scheduleForm.setFields(schedule);


                LinkedHashMap<String, Canvas> items = new LinkedHashMap<String, Canvas>();
                items.put(MSG.view_admin_plugins_update_on_agents_now(), nowCanvas);
                items.put(MSG.view_admin_plugins_update_on_agents_delayed(), delayForm);
                items.put(MSG.view_admin_plugins_update_on_agents_scheduled(), scheduleForm);

                DynamicForm form = new DynamicForm();
                form.setWidth(300);
                final RadioGroupWithComponentsItem scheduling = new RadioGroupWithComponentsItem("scheduling", "", items, form);

                form.setFields(scheduling);

                layout.addMember(form);

                ButtonItem ok = new ButtonItem("ok", MSG.common_button_ok());
                ok.setEndRow(false);
                ok.setAlign(Alignment.RIGHT);
                ButtonItem cancel = new ButtonItem("cancel", MSG.common_button_cancel());
                cancel.setStartRow(false);
                cancel.setAlign(Alignment.LEFT);

                DynamicForm buttons = new DynamicForm();
                buttons.setNumCols(2);
                buttons.setFields(ok, cancel);

                layout.addMember(buttons);

                ok.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
                    @Override
                    public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                        long delay;
                        if (MSG.view_admin_plugins_update_on_agents_now().equals(scheduling.getSelected())) {
                            delay = 0;
                        } else if (MSG.view_admin_plugins_update_on_agents_delayed().equals(scheduling.getSelected())) {
                            Integer value = (Integer) startDelay.getValue();
                            TimeUnit u = startDelay.getValueUnit();

                            if (value == null) {
                                CoreGUI.getErrorHandler()
                                    .handleError(MSG.view_admin_plugins_update_on_agents_no_time_specified());
                                w.hide();
                                return;
                            }

                            delay = value;
                            switch (u) {
                            case SECONDS:
                                delay *= 1000;
                                break;
                            case MINUTES:
                                delay *= 60 * 1000;
                                break;
                            case HOURS:
                                delay *= 60 * 60 * 1000;
                                break;
                            case DAYS:
                                delay *= 24 * 60 * 60 * 1000;
                                break;
                            }
                        } else {
                            Date scheduledDate = schedule.getValueAsDate();
                            delay = scheduledDate.getTime() - new Date().getTime();
                        }

                        GWT.log("About to schedule update of the plugins on the agent in " + delay + " milliseconds.");

                        GWTServiceLookup.getPluginService().updatePluginsOnAgents(delay, new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_admin_plugins_update_on_agents_failure() + " " +
                                        caught.getMessage(), caught);
                                w.hide();
                            }

                            @Override
                            public void onSuccess(Void result) {
                                w.hide();
                            }
                        });
                    }
                });

                scheduling.setValue(MSG.view_admin_plugins_update_on_agents_now());

                cancel.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
                    @Override
                    public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                        w.hide();
                    }
                });

                w.addItem(layout);

                w.setTitle(MSG.view_admin_plugins_update_on_agents());
                w.setHeight(200);
                w.setWidth(340);
                w.show();
            }

        });

        IButton scanForUpdatesButton = new EnhancedIButton(MSG.view_admin_plugins_scan());
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

        PluginFileUploadForm pluginUploadForm = new PluginFileUploadForm(MSG.view_admin_plugins_upload(), true);

        addExtraWidget(scanForUpdatesButton, true);
        addExtraWidget(updateOnAgentsButton, true);
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

            ListGridField versionField = new ListGridField(FIELD_VERSION, MSG.common_title_version());
            versionField.setHidden(true);
            fields.add(versionField);

            idField.setWidth(100);
            nameField.setWidth("30%");
            descriptionField.setWidth("*");
            lastUpdateField.setWidth("20%");
            enabledField.setWidth(65);
            versionField.setWidth(100);

            return fields;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response, Criteria criteria) {
            GWTServiceLookup.getPluginService().getAgentPlugins(false, new AsyncCallback<ArrayList<Plugin>>() {
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
