/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.history;

import java.util.EnumSet;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.AbstractOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.history.ResourceOperationHistoryDetailsView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.operation.schedule.OperationCreateWizard;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;

/**
 * @author Ian Springer
 */
public class ResourceOperationHistoryListView extends AbstractOperationHistoryListView {

    private ResourceComposite resourceComposite;

    public ResourceOperationHistoryListView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, new ResourceOperationHistoryDataSource(), null, 
            new Criteria(ResourceOperationHistoryDataSource.CriteriaField.RESOURCE_ID,
            String.valueOf(resourceComposite.getResource().getId())));
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        if (this.resourceComposite.getResourcePermission().isControl()) {
            final Resource resource = this.resourceComposite.getResource();
            final Menu operationMenu = new LocatableMenu(this.extendLocatorId("Operation"));
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.operations),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        for (final OperationDefinition operationDefinition : type.getOperationDefinitions()) {
                            MenuItem menuItem = new MenuItem(operationDefinition.getDisplayName());
                            operationMenu.addItem(menuItem);
                            menuItem.addClickHandler(new ClickHandler() {
                                public void onClick(MenuItemClickEvent event) {
                                    new OperationCreateWizard(resource, operationDefinition).startOperationWizard();
                                }
                            });
                        }
                    }
                });

            IMenuButton operationsButton = new LocatableIMenuButton(this.extendLocatorId("Run"),
                MSG.view_operationHistoryList_button_runOperation(), operationMenu);
            operationsButton.setShowMenuBelow(false);
            operationsButton.setAutoFit(true);
            addExtraWidget(operationsButton);
        }
    }

    @Override
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(this.extendLocatorId("Details"));
    }

}
