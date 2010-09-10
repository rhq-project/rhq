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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.create.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.detail.OperationDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;

/**
 * @author Greg Hinkle
 */
public class OperationHistoryView extends TableSection {

    private ResourceComposite composite;
    private Resource resource;

    public OperationHistoryView(String locatorId) {
        super(locatorId, "Operation History");
        setWidth100();
        setHeight100();

        setDataSource(new OperationHistoryDataSource());

    }

    public OperationHistoryView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, "Operation History", new Criteria("resourceId", String.valueOf(resourceComposite.getResource()
            .getId())));
        this.composite = resourceComposite;
        this.resource = resourceComposite.getResource();

        setDataSource(new OperationHistoryDataSource());

    }

    @Override
    protected void configureTable() {

        getListGrid().getField("id").setWidth(40);
        getListGrid().getField("operationName").setWidth("*");
        getListGrid().getField("status").setWidth(100);
        getListGrid().getField("status").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                OperationRequestStatus status = OperationRequestStatus.valueOf((String) o);
                String icon = "";
                switch (status) {
                case INPROGRESS:
                    break;
                case SUCCESS:
                    icon = "_ok";
                    break;
                case FAILURE:
                    icon = "_failed";
                    break;
                case CANCELED:
                    icon = "_cancel";
                    break;
                }

                return Canvas.imgHTML("subsystems/control/Operation" + icon + "_16.png", 16, 16)
                    + status.getDisplayName();
            }
        });

        getListGrid().getField("startedTime").setWidth(120);

        if (this.resource == null) {
            getListGrid().getField("resource").setWidth(300);
            getListGrid().getField("resource").setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Resource res = (Resource) o;
                    return "<a href=\"#Resource/" + res.getId() + "\">" + res.getName() + "</a>";
                }
            });
        } else {
            getListGrid().hideField("resource");
        }

        if (resource != null && composite.getResourcePermission().isControl()) {
            final Menu operationMenu = new LocatableMenu(this.extendLocatorId("Operation"));
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
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

            IMenuButton operationsButton = new LocatableIMenuButton(this.extendLocatorId("Run"), "Run Operation",
                operationMenu);
            operationsButton.setShowMenuBelow(false);
            addExtraWidget(operationsButton);
        }

    }

    @Override
    public Canvas getDetailsView(int id) {
        OperationDetailsView detailsView = new OperationDetailsView(this.extendLocatorId("Details"));

        return detailsView;
    }

    public static OperationHistoryView getResourceHistoryView(String locatorId, ResourceComposite resource) {

        return new OperationHistoryView(locatorId, resource);
    }

}
