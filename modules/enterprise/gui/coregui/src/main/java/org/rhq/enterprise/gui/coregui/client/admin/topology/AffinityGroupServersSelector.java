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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
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
    
    private static Window modalWindow = null;
    private static VLayout layout = null;
    
    private AffinityGroupServersSelector() {
        super("");
        affinityGroupId = -1;
    }

    private AffinityGroupServersSelector(String id, Integer affinityGroupId) {
        super(id, false);
        this.affinityGroupId = affinityGroupId;
        prepareMembers();
    }
    
    private void prepareMembers() {
        GWTServiceLookup.getCloudService().getServerMembersByAffinityGroupId(affinityGroupId, PageControl.getUnlimitedInstance(), new AsyncCallback<PageList<Server>>() {
            public void onSuccess(PageList<Server> result) {
                ListGridRecord[] records = getDataSource().buildRecords(result);
                setAssigned(records);
//                AffinityGroupServersSelector.super.onInit();
                new Timer(){
                    @Override
                    public void run() {
                        modalWindow.addItem(layout);
                        modalWindow.show();
                    }
                }.schedule(2000);

                
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
    
    public static void show(Integer affinityGroupId) {
        modalWindow = new Window();
        modalWindow.setTitle(MSG.view_adminTopology_affinityGroups() + ": "
            + MSG.view_adminTopology_affinityGroups_createNew());
        modalWindow.setOverflow(Overflow.VISIBLE);
        modalWindow.setMinWidth(800);
        modalWindow.setMinHeight(400);
        modalWindow.setWidth(800);
        modalWindow.setHeight(400);
//        modalWindow.setAutoSize(true);
        modalWindow.setAutoCenter(true);
        modalWindow.setCanDragResize(true);
        modalWindow.setCanDragReposition(true);

        layout = new VLayout();
        layout.setWidth100();
        layout.setHeight100();
        layout.setPadding(15);
        layout.setLayoutMargin(20);
        layout.setVPolicy(LayoutPolicy.FILL);

        AffinityGroupServersSelector selector = new AffinityGroupServersSelector("foo", affinityGroupId);
        layout.addMember(selector);

        VLayout spacer = new VLayout();
        spacer.setHeight(10);
        layout.addMember(spacer);

        IButton cancel = new LocatableIButton(selector.extendLocatorId("Cancel"), MSG.common_button_cancel());
        cancel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                modalWindow.destroy();
            }
        });
        IButton save = new LocatableIButton(selector.extendLocatorId("Save"),
            MSG.common_button_save());
        save.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
               SC.say("asdf..");
            }
        });

        HLayout buttons = new HLayout(10);
        buttons.setLayoutAlign(Alignment.CENTER);
        buttons.addMember(cancel);
        buttons.addMember(save);
        layout.addMember(buttons);
    }

}
