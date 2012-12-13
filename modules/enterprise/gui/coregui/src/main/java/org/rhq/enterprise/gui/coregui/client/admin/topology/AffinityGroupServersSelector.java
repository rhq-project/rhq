/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.admin.topology;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;

/**
 * @author Jirka Kremser
 */
public class AffinityGroupServersSelector extends AbstractSelector<Server, org.rhq.core.domain.criteria.Criteria> {

    private static final String ITEM_ICON = "types/Server_up_16.png";

    private final Integer affinityGroupId;

    private static RPCDataSource<Server, org.rhq.core.domain.criteria.Criteria> datasource = null;

    private static Window modalWindow;
    private static boolean shouldBeClosed;
    private static VLayout layout;
    private List<Integer> originallyAssignedIds;

    private AffinityGroupServersSelector() {
        super("");
        affinityGroupId = -1;
    }

    private AffinityGroupServersSelector(String id, Integer affinityGroupId) {
        super(id, false);
        this.affinityGroupId = affinityGroupId;
        prepareMembers(this);
    }

    private void prepareMembers(final AffinityGroupServersSelector selector) {
        GWTServiceLookup.getCloudService().getServerMembersByAffinityGroupId(affinityGroupId,
            PageControl.getUnlimitedInstance(), new AsyncCallback<PageList<Server>>() {
                public void onSuccess(PageList<Server> result) {
                    ListGridRecord[] records = getDataSource().buildRecords(result);
                    originallyAssignedIds = getIdList(records);
                    setAssigned(records);
                    modalWindow.addItem(layout);
                    modalWindow.show();
                    selector.reset();
                }

                @Override
                public void onFailure(Throwable t) {
                    //todo: CoreGUI.getErrorHandler().handleError(MSG.view_admin_plugins_loadFailure(), t);
                }
            });

    }

    @Override
    protected RPCDataSource<Server, org.rhq.core.domain.criteria.Criteria> getDataSource() {
        if (datasource == null) {
            datasource = new ServerDatasource(affinityGroupId, false);
        }
        return datasource;
    }

    @Override
    protected DynamicForm getAvailableFilterForm() {
        return null; // No Filters Currently
    }

    @Override
    protected int getMaxAvailableRecords() {
        return 500;
    }

    @Override
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        return null; // No Filters Currently
    }

    @Override
    protected String getItemTitle() {
        return MSG.view_adminTopology_affinityGroups();
    }

    @Override
    protected String getItemIcon() {
        return ITEM_ICON;
    }

    public static void show(Integer affinityGroupId, final TableSection parrent) {
        modalWindow = new Window();
        modalWindow.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent event) {
                closeAndRefresh(parrent, false);
            }
        });
        modalWindow.setTitle(MSG.view_adminTopology_affinityGroups() + ": "
            + MSG.view_adminTopology_affinityGroups_createNew());
        modalWindow.setOverflow(Overflow.VISIBLE);
        modalWindow.setWidth(800);
        modalWindow.setHeight(400);
        modalWindow.setAutoCenter(true);
        modalWindow.setCanDragResize(true);
        modalWindow.setCanDragReposition(true);

        layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setPadding(10);
        layout.setLayoutMargin(10);

        final AffinityGroupServersSelector selector = new AffinityGroupServersSelector("foo", affinityGroupId);
        layout.addMember(selector);

        IButton cancel = new LocatableIButton(selector.extendLocatorId("Cancel"), MSG.common_button_cancel());
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                closeAndRefresh(parrent, false);
            }
        });
        IButton save = new LocatableIButton(selector.extendLocatorId("Save"), MSG.common_button_save());
        save.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                List<Integer> actuallySelected = getIdList(selector.getSelectedRecords());
                List<Integer> originallySelected = selector.getOriginallyAssignedIds();
                originallySelected.removeAll(actuallySelected);
                actuallySelected.removeAll(selector.getOriginallyAssignedIds());
                shouldBeClosed = true;
                if (!originallySelected.isEmpty()) {
                    shouldBeClosed = false;
                    GWTServiceLookup.getCloudService().removeServersFromGroup(
                        originallySelected.toArray(new Integer[originallySelected.size()]), new AsyncCallback<Void>() {

                            public void onSuccess(Void result) {
                                closeAndRefresh(parrent, true);
                            }

                            public void onFailure(Throwable caught) {
                                //todo: error handling
                                SC.say("errX");
                            }
                        });
                }
                if (!actuallySelected.isEmpty()) {
                    shouldBeClosed = false;
                    GWTServiceLookup.getCloudService().addServersToGroup(selector.getAffinityGroupId(),
                        actuallySelected.toArray(new Integer[actuallySelected.size()]), new AsyncCallback<Void>() {

                            public void onSuccess(Void result) {
                                closeAndRefresh(parrent, true);
                            }

                            public void onFailure(Throwable caught) {
                                //todo: error handling
                                SC.say("errX");
                            }
                        });
                }
                if (shouldBeClosed) {
                    closeAndRefresh(parrent, false);
                }
                //                SC.say("To del: " + originallySelected + "\n\nTo add: " + actuallySelected);

            }
        });

        HLayout buttons = new HLayout(10);
        buttons.setHeight(20);
        buttons.setLayoutAlign(Alignment.CENTER);
        buttons.setLayoutBottomMargin(0);
        buttons.addMember(save);
        buttons.addMember(cancel);
        layout.addMember(buttons);
    }

    private static void closeAndRefresh(TableSection parrent, boolean fullRefresh) {
        if (modalWindow != null) {
            modalWindow.destroy();
        }
        if (fullRefresh) {
            parrent.refresh();
        } else {
            parrent.refreshTableInfo();
        }
    }

    private static List<Integer> getIdList(ListGridRecord[] records) {
        if (records == null) {
            return null;
        }
        List<Integer> ids = new ArrayList<Integer>(records.length);
        for (ListGridRecord record : records) {
            ids.add(record.getAttributeAsInt(ServerDatasourceField.FIELD_ID.propertyName()));
        }
        return ids;
    }

    public List<Integer> getOriginallyAssignedIds() {
        return new ArrayList<Integer>(originallyAssignedIds);
    }

    public Integer getAffinityGroupId() {
        return affinityGroupId;
    }

}
