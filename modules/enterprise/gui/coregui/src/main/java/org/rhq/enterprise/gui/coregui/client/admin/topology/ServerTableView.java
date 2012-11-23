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
package org.rhq.enterprise.gui.coregui.client.admin.topology;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * @author Jiri Kremser
 */
public class ServerTableView extends TableSection<ServerNodeDatasource> {

    public static final ViewName VIEW_ID = new ViewName("Servers(GWT)", MSG.view_adminTopology_servers()+"(GWT)", IconEnum.SERVERS);
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

//    private boolean showUndeployed = false;

    public ServerTableView(String locatorId) {
        super(locatorId, MSG.view_adminTopology_servers()+"(GWT)");
        setHeight100();
        setWidth100();
        setDataSource(new ServerNodeDatasource());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(ServerNodeDataourceField.FIELD_NAME.propertyName(), SortDirection.ASCENDING);

        addTableAction(extendLocatorId("setNormal"), MSG.view_adminTopology_server_setNormal(), MSG.common_msg_areYouSure(),
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    int[] selectedIds = getSelectedIds(selections);
                    SC.say("setting servers to normal mode, ids: " + selectedIds);
//                    GWTServiceLookup.getPluginService().enableServerPlugins(selectedIds,
//                        new AsyncCallback<ArrayList<String>>() {
//                            @Override
//                            public void onSuccess(ArrayList<String> result) {
//                                Message msg = new Message(
//                                    MSG.view_admin_plugins_enabledServerPlugins(result.toString()), Severity.Info);
//                                CoreGUI.getMessageCenter().notify(msg);
//                                refresh();
//                            }
//
//                            @Override
//                            public void onFailure(Throwable caught) {
//                                CoreGUI.getErrorHandler().handleError(
//                                    MSG.view_admin_plugins_enabledServerPluginsFailure() + " " + caught.getMessage(),
//                                    caught);
//                                refreshTableInfo();
//                            }
//                        });
                }
            });

        addTableAction(extendLocatorId("setMaintenance"), MSG.view_adminTopology_server_setMaintenance(), new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                ArrayList<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_admin_plugins_serverDisableConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            SC.say("setting servers to maintenance mode, ids: " + selectedIds);
//                            GWTServiceLookup.getPluginService().disableServerPlugins(selectedIds,
//                                new AsyncCallback<ArrayList<String>>() {
//                                    @Override
//                                    public void onSuccess(ArrayList<String> result) {
//                                        Message msg = new Message(MSG.view_admin_plugins_disabledServerPlugins(result
//                                            .toString()), Severity.Info);
//                                        CoreGUI.getMessageCenter().notify(msg);
//                                        refresh();
//                                    }
//
//                                    @Override
//                                    public void onFailure(Throwable caught) {
//                                        CoreGUI.getErrorHandler().handleError(
//                                            MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
//                                                + caught.getMessage(), caught);
//                                        refreshTableInfo();
//                                    }
//                                });
                        } else {
                            refreshTableInfo();
                        }
                    }
                });
            }
        });

        
        addTableAction(extendLocatorId("removeSelected"), MSG.view_adminTopology_server_removeSelected(), MSG.common_msg_areYouSure(),
            new AuthorizedTableAction(this, TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
                public void executeAction(ListGridRecord[] selections, Object actionValue) {
                    int[] selectedIds = getSelectedIds(selections);
                    SC.say("removing servers with ids: " + selectedIds);
//                    GWTServiceLookup.getCloudService().purgeServerPlugins(selectedIds,
//                        new AsyncCallback<ArrayList<String>>() {
//                            @Override
//                            public void onSuccess(ArrayList<String> result) {
//                                Message msg = new Message(
//                                    MSG.view_admin_plugins_purgedServerPlugins(result.toString()), Severity.Info);
//                                CoreGUI.getMessageCenter().notify(msg);
//                                refresh();
//                            }
//
//                            @Override
//                            public void onFailure(Throwable caught) {
//                                CoreGUI.getErrorHandler().handleError(
//                                    MSG.view_admin_plugins_purgedServerPluginsFailure() + " " + caught.getMessage(),
//                                    caught);
//                                refreshTableInfo();
//                            }
//                        });
                }
            });

//        IButton scanForUpdatesButton = new LocatableIButton(extendLocatorId("scanButton"),
//            MSG.view_admin_plugins_scan());
//        scanForUpdatesButton.setAutoFit(true);
//        scanForUpdatesButton.addClickHandler(new ClickHandler() {
//            public void onClick(ClickEvent event) {
//                GWTServiceLookup.getPluginService().scanAndRegister(new AsyncCallback<Void>() {
//                    @Override
//                    public void onSuccess(Void result) {
//                        Message msg = new Message(MSG.view_admin_plugins_scanComplete(), Severity.Info);
//                        CoreGUI.getMessageCenter().notify(msg);
//                        refresh();
//                    }
//
//                    @Override
//                    public void onFailure(Throwable caught) {
//                        CoreGUI.getErrorHandler().handleError(
//                            MSG.view_admin_plugins_scanFailure() + " " + caught.getMessage(), caught);
//                        refreshTableInfo();
//                    }
//                });
//            }
//        });

//        IButton restartMasterPCButton = new LocatableIButton(extendLocatorId("restartMasterPCButton"),
//            MSG.view_admin_plugins_restartMasterPC());
//        restartMasterPCButton.setAutoFit(true);
//        restartMasterPCButton.addClickHandler(new ClickHandler() {
//            public void onClick(ClickEvent event) {
//                Message msg = new Message(MSG.view_admin_plugins_restartMasterPCStarted(), Severity.Info);
//                CoreGUI.getMessageCenter().notify(msg);
//
//                GWTServiceLookup.getPluginService().restartMasterPluginContainer(new AsyncCallback<Void>() {
//                    @Override
//                    public void onSuccess(Void result) {
//                        Message msg = new Message(MSG.view_admin_plugins_restartMasterPCComplete(), Severity.Info);
//                        CoreGUI.getMessageCenter().notify(msg);
//                        refresh();
//                    }
//
//                    @Override
//                    public void onFailure(Throwable caught) {
//                        CoreGUI.getErrorHandler().handleError(
//                            MSG.view_admin_plugins_restartMasterPCFailure() + " " + caught.getMessage(), caught);
//                        refreshTableInfo();
//                    }
//                });
//            }
//        });

//        PluginFileUploadForm pluginUploadForm = new PluginFileUploadForm(extendLocatorId("upload"),
//            MSG.view_admin_plugins_upload(), true);

//        addExtraWidget(scanForUpdatesButton, true);
//        addExtraWidget(restartMasterPCButton, true);
//        addExtraWidget(pluginUploadForm, true);

        super.configureTable();
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerDetailView(extendLocatorId("detailsView"), id);
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

    

}
