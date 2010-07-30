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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation;

import java.util.EnumSet;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.detail.OperationDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

/**
 * @author Greg Hinkle
 */
public class OperationHistoryView extends VLayout {

    Table table;
    Resource resource;
    Criteria criteria;

    @Override
    protected void onInit() {
        super.onInit();


    }

    public OperationHistoryView() {
        setWidth100();
        setHeight100();
    }

    public OperationHistoryView(Resource resource) {
        this.resource = resource;
        this.criteria = new Criteria("resourceId", String.valueOf(resource.getId()));
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (criteria == null) {
            table = new Table("Operation History");
        } else {
            table = new Table("Operation History", criteria);
        }

        table.setDataSource(new OperationHistoryDataSource());

        table.getListGrid().getField("id").setWidth(40);
        table.getListGrid().getField("operationName").setWidth("*");
        table.getListGrid().getField("status").setWidth(100);
        table.getListGrid().getField("startedTime").setWidth(120);

        if (this.resource == null) {
            table.getListGrid().getField("resource").setWidth(300);
            table.getListGrid().getField("resource").setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Resource res = (Resource) o;
                    return "<a href=\"#Resource/" + res.getId() + "\">" + res.getName() + "</a>";
                }
            });
        } else {
            table.getListGrid().hideField("resource");
        }

        table.getListGrid().addRecordDoubleClickHandler(new RecordDoubleClickHandler() {
            public void onRecordDoubleClick(RecordDoubleClickEvent recordDoubleClickEvent) {
                ResourceOperationHistory history = (ResourceOperationHistory) recordDoubleClickEvent.getRecord().getAttributeAsObject("entity");

                showDetails(history);
            }
        });


        table.addTableAction("Details", Table.SelectionEnablement.SINGLE, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                ResourceOperationHistory history = (ResourceOperationHistory) selection[0].getAttributeAsObject("entity");

                showDetails(history);
            }
        });


        if (resource != null) {
            final Menu operationMenu = new Menu();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                    resource.getResourceType().getId(),
                    EnumSet.of(ResourceTypeRepository.MetadataType.operations),
                    new ResourceTypeRepository.TypeLoadedCallback() {
                        public void onTypesLoaded(ResourceType type) {
                            for (final OperationDefinition od : type.getOperationDefinitions()) {
                                MenuItem menuItem = new MenuItem(od.getDisplayName());
                                operationMenu.addItem(menuItem);
                                menuItem.addClickHandler(new ClickHandler() {
                                    public void onClick(MenuItemClickEvent event) {
                                        new OperationCreateWizard(resource, od).startOperationWizard();
                                    }
                                });
                            }
                        }
                    });

            IMenuButton operationsButton = new IMenuButton("Run Operation", operationMenu);
            operationsButton.setShowMenuBelow(false);
            table.addExtraWidget(operationsButton);
        }


        addMember(table);
    }

    private void showDetails(ResourceOperationHistory history) {
        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        criteria.addFilterId(history.getId());

        criteria.fetchOperationDefinition(true);
        criteria.fetchParameters(true);
        criteria.fetchResults(true);

        GWTServiceLookup.getOperationService().findResourceOperationHistoriesByCriteria(
                criteria, new AsyncCallback<PageList<ResourceOperationHistory>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failure loading operation history", caught);
                    }

                    public void onSuccess(PageList<ResourceOperationHistory> result) {
                        ResourceOperationHistory item = result.get(0);
                        OperationDetailsView.displayDetailsDialog(item);
                    }
                }
        );
    }


    public static OperationHistoryView getResourceHistoryView(Resource resource) {

        return new OperationHistoryView(resource);
    }
}
