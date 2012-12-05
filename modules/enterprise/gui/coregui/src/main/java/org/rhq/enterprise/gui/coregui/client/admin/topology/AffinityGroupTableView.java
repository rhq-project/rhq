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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;

/**
 * @author Jiri Kremser
 * 
 */
public class AffinityGroupTableView extends TableSection<AffinityGroupWithCountsDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("AffinityGroups(GWT)", MSG.view_adminTopology_affinityGroups()
        + "(GWT)", IconEnum.ALL_GROUPS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    public AffinityGroupTableView(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
        setHeight100();
        setWidth100();
        setDataSource(new AffinityGroupWithCountsDatasource());
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        showActions();
        super.configureTable();

        //        for (ListGridField field : fields) {
        //            // adding the cell formatter for name field (clickable link)
        //            if (field.getName() == FIELD_NAME) {
        //                field.setCellFormatter(new CellFormatter() {
        //                    @Override
        //                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
        //                        if (value == null) {
        //                            return "";
        //                        }
        //                        String detailsUrl = "#" + VIEW_PATH + "/" + getId(record);
        //                        String formattedValue = StringUtility.escapeHtml(value.toString());
        //                        return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);
        //
        //                    }
        //                });
        //            }
        //            // TODO: adding the cell formatter for affinity group (clickable link)
        //        }
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new ServerDetailView(extendLocatorId("detailsView"), id);
    }

    private void showActions() {
        addTableAction(extendLocatorId("createNew"), MSG.view_adminTopology_affinityGroups_createNew(),
            new AuthorizedTableAction(this, TableActionEnablement.ALWAYS, Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    showCreateAffinityGroupWindow();
                }
            });

        addTableAction(extendLocatorId("removeSelected"), MSG.view_adminTopology_server_removeSelected(),
            MSG.common_msg_areYouSure(), new AuthorizedTableAction(this, TableActionEnablement.ANY,
                Permission.MANAGE_SETTINGS) {
                public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                    List<String> selectedNames = getSelectedNames(selections);
                    String message = "Really? Delete? For all I've done for you? " + selectedNames;
                    SC.ask(message, new BooleanCallback() {
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                int[] selectedIds = getSelectedIds(selections);
                                SC.say("setting servers to maintenance mode, ids: " + selectedIds);
                                GWTServiceLookup.getCloudService().deleteAffinityGroups(selectedIds,
                                    new AsyncCallback<Integer>() {
                                        public void onSuccess(Integer count) {
                                            // TODO: msg with count
                                            Message msg = new Message(MSG
                                                .view_admin_plugins_disabledServerPlugins("sdf"), Message.Severity.Info);
                                            CoreGUI.getMessageCenter().notify(msg);
                                            refresh();
                                        }

                                        public void onFailure(Throwable caught) {
                                            // TODO: msg
                                            CoreGUI.getErrorHandler().handleError(
                                                MSG.view_admin_plugins_disabledServerPluginsFailure() + " "
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

    private List<String> getSelectedNames(ListGridRecord[] selections) {
        if (selections == null) {
            return new ArrayList<String>(0);
        }
        List<String> ids = new ArrayList<String>(selections.length);
        for (ListGridRecord selection : selections) {
            ids.add(selection.getAttributeAsString(FIELD_NAME));
        }
        return ids;
    }

    private void showCreateAffinityGroupWindow() {
        final Window modalWindow = new Window();
        modalWindow.setTitle(MSG.view_adminTopology_affinityGroups() + ": "
            + MSG.view_adminTopology_affinityGroups_createNew());
        modalWindow.setOverflow(Overflow.VISIBLE);
        modalWindow.setMinWidth(400);
        modalWindow.setMinHeight(400);
        modalWindow.setAutoSize(true);
        modalWindow.setAutoCenter(true);
        modalWindow.setCanDragResize(true);
        modalWindow.setCanDragReposition(true);

        VLayout layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setPadding(15);
        layout.setLayoutMargin(20);
        layout.setVPolicy(LayoutPolicy.FILL);

        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);
        final TextItem name = new TextItem(FIELD_NAME, MSG.common_title_name());
        name.setRequired(true);
        LengthRangeValidator nameLengthValidator = new LengthRangeValidator();
        nameLengthValidator.setMin(3);
        nameLengthValidator.setMax(100);
        name.setValidators(nameLengthValidator);
        form.setFields(name);
        layout.addMember(form);

        VLayout spacer = new VLayout();
        spacer.setHeight(10);
        layout.addMember(spacer);

        IButton cancel = new LocatableIButton(this.extendLocatorId("Cancel"), MSG.common_button_cancel());
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                modalWindow.destroy();
            }
        });
        IButton save = new LocatableIButton(this.extendLocatorId("Create"),
            MSG.view_adminTopology_affinityGroups_createNew());
        save.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (form.validate()) {
                    String name = form.getValueAsString(FIELD_NAME);
                    AffinityGroup affinityGroup = new AffinityGroup(name);
                    GWTServiceLookup.getCloudService().createAffinityGroup(affinityGroup, new AsyncCallback<Integer>() {
                        public void onSuccess(Integer result) {
                            Map<String, String> errors = new HashMap<String, String>();
                            errors.put(FIELD_NAME, result + "");
                            form.setErrors(errors, true);
                            // todo: hide dialog and open the detail window
                        }

                        public void onFailure(Throwable caught) {
                            Map<String, String> errors = new HashMap<String, String>();
                            errors.put(FIELD_NAME, caught.getMessage());
                            form.setErrors(errors, true);
                        }
                    });
                }
            }
        });

        HLayout buttons = new HLayout(10);
        buttons.setLayoutAlign(Alignment.CENTER);
        buttons.addMember(cancel);
        buttons.addMember(save);
        layout.addMember(buttons);

        modalWindow.addItem(layout);
        modalWindow.show();

    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
