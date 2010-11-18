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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

/**
 * The Resource Summary>Overview tab.
 *
 * @author Lukas Krejci
 */
public class OverviewView extends LocatableVLayout {

    private Table errorsGrid;
    
    public OverviewView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId);
        OverviewForm form = new OverviewForm(extendLocatorId("form"), resourceComposite);
        errorsGrid = new Table(extendLocatorId("errors"), "Detected errors", null, null, new String[] { ResourceErrorsDataSource.DETAIL_ID });

        Resource resource = resourceComposite.getResource();        
        ResourceErrorsDataSource errors = new ResourceErrorsDataSource(resource.getId());
        
        errorsGrid.setShowFooter(false);
        errorsGrid.setDataSource(errors);
        
        form.setHeight("*");
        errorsGrid.setHeight(200); //this should be just enough to fit the maximum of 3 rows in this table (there's at most 1 error per type)
        
        addMember(form);
        addMember(errorsGrid);
     
    }

    @Override 
    public void onInit() {
        super.onInit();
        initErrorsGrid();
    }
    
    private void initErrorsGrid() {        
        errorsGrid.setTooltip("Click on the rows to see the error details.");
        errorsGrid.getListGrid().addCellClickHandler(new CellClickHandler() {
            public void onCellClick(CellClickEvent event) {
                ListGridRecord record = event.getRecord();
                final Window w = new Window();
                w.setTitle("Error Details");
                w.setIsModal(true);
                w.setShowMinimizeButton(false);
                w.setShowModalMask(true);
                w.setWidth(640);
                w.setHeight(480);
                w.centerInPage();
                w.setCanDragResize(true);
                
                LocatableVLayout layout = new LocatableVLayout(errorsGrid.extendLocatorId("dialogLayout"), 10);
                layout.setDefaultLayoutAlign(Alignment.CENTER);
                layout.setLayoutMargin(10);
                
                w.addItem(layout);
                
                HTMLPane details = new HTMLPane();                
                details.setContents("<pre>" + record.getAttribute(ResourceErrorsDataSource.DETAIL_ID) + "</pre>");
                layout.addMember(details);
                
                IButton ok = new IButton("Ok");
                ok.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        w.destroy();
                    }
                });
                
                layout.addMember(ok);
                
                w.show();
            }
        });
    }
    
}
