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
package org.rhq.coregui.client.admin.topology;

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
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.form.fields.events.KeyUpHandler;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;

/**
 * Shows the table of all affinity groups.
 *  
 * @author Jirka Kremser
 */
public class AffinityGroupTableView extends TableSection<AffinityGroupWithCountsDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("AffinityGroups", MSG.view_adminTopology_affinityGroups(),
        IconEnum.ALL_GROUPS);

    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    public AffinityGroupTableView() {
        super(null);
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
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new AffinityGroupDetailView(id);
    }

    private void showActions() {
        addTableAction(MSG.view_adminTopology_affinityGroups_createNew(), new AuthorizedTableAction(this,
            TableActionEnablement.ALWAYS, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                showCreateAffinityGroupWindow();
            }
        });

        addTableAction(MSG.view_adminTopology_server_removeSelected(), null, new AuthorizedTableAction(this,
            TableActionEnablement.ANY, Permission.MANAGE_SETTINGS) {
            public void executeAction(final ListGridRecord[] selections, Object actionValue) {
                final List<String> selectedNames = getSelectedNames(selections);
                String message = MSG.view_adminTopology_message_removeAGroupsConfirm(selectedNames.toString());
                SC.ask(message, new BooleanCallback() {
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            int[] selectedIds = getSelectedIds(selections);
                            GWTServiceLookup.getTopologyService().deleteAffinityGroups(selectedIds,
                                new AsyncCallback<Integer>() {
                                    public void onSuccess(Integer count) {
                                        Message msg = new Message(MSG.view_adminTopology_message_removedAGroups(String
                                            .valueOf(count)), Message.Severity.Info);
                                        CoreGUI.getMessageCenter().notify(msg);
                                        refresh();
                                    }

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_adminTopology_message_removeAGroupsFail(selectedNames.toString())
                                                + " " + caught.getMessage(), caught);
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
        modalWindow.setShowMinimizeButton(false);
        modalWindow.setShowMaximizeButton(true);
        modalWindow.setIsModal(true);
        modalWindow.setShowModalMask(true);
        modalWindow.centerInPage();
        modalWindow.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                modalWindow.destroy();
                AffinityGroupTableView.this.refreshTableInfo();
            }
        });

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
        name.addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp(KeyUpEvent event) {
                if ("Enter".equals(event.getKeyName())) {
                    createNewGroup(modalWindow, form);
                }
            }
        });

        form.setFields(name);
        layout.addMember(form);
        VLayout spacer = new VLayout();
        spacer.setHeight(10);
        layout.addMember(spacer);

        IButton cancel = new EnhancedIButton(MSG.common_button_cancel());
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                modalWindow.destroy();
                AffinityGroupTableView.this.refreshTableInfo();
            }
        });
        final IButton create = new EnhancedIButton(MSG.view_adminTopology_affinityGroups_createNew());
        create.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                createNewGroup(modalWindow, form);
            }
        });

        HLayout buttons = new HLayout(10);
        buttons.setLayoutAlign(Alignment.CENTER);
        buttons.addMember(create);
        buttons.addMember(cancel);
        layout.addMember(buttons);
        modalWindow.addItem(layout);
        modalWindow.show();
        name.focusInItem();
    }

    private void createNewGroup(final Window modalWindow, final DynamicForm form) {
        if (form.validate()) {
            String name = form.getValueAsString(FIELD_NAME);
            AffinityGroup affinityGroup = new AffinityGroup(name);
            GWTServiceLookup.getTopologyService().createAffinityGroup(affinityGroup, new AsyncCallback<Integer>() {
                public void onSuccess(Integer affinityGroupId) {
                    modalWindow.destroy();
                    CoreGUI.goToView(VIEW_PATH + "/" + affinityGroupId);
                }

                public void onFailure(Throwable caught) {
                    Map<String, String> errors = new HashMap<String, String>();
                    errors.put(FIELD_NAME, caught.getMessage());
                    form.setErrors(errors, true);
                    AffinityGroupTableView.this.refreshTableInfo();
                }
            });
        }
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
