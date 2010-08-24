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
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * The Resource Summary>Overview tab.
 *
 * @author Lukas Krejci
 */
public class OverviewView extends VLayout implements ResourceSelectListener {

    private ResourceComposite resourceComposite;

    public OverviewView(ResourceComposite resourceComposite) {
        super();
        this.resourceComposite = resourceComposite;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setLeft("10%");
        setWidth("80%");

        if (this.resourceComposite != null) {
            onResourceSelected(this.resourceComposite);
        }
    }

    public void onResourceSelected(ResourceComposite resourceComposite) {
        addMember(new OverviewForm(resourceComposite));
        buildErrorListGrid(resourceComposite);
    }

    private void buildErrorListGrid(ResourceComposite resourceComposite) {
        final Resource resource = resourceComposite.getResource();
        Table errorsGrid = new Table("Errors");
        
        errorsGrid.setShowFooter(false);
//        errorsGrid.getListGrid().setGroupByField(ResourceErrorsDataSource.ERROR_TYPE_ID);
//        errorsGrid.getListGrid().setGroupStartOpen(GroupStartOpen.ALL);
//        errorsGrid.getListGrid().setShowGroupSummary(true);

        errorsGrid.setDataSource(new ResourceErrorsDataSource(resource.getId()));
        
        //hide only works after we set the datasource
        errorsGrid.getListGrid().hideField(ResourceErrorsDataSource.DETAIL_ID);
        
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
                
                VLayout layout = new VLayout(10);
                layout.setLayoutAlign(Alignment.CENTER);
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
                ok.setAlign(Alignment.CENTER);
                
                layout.addMember(ok);
                
                w.show();
            }
        });
        
        addMember(errorsGrid);
    }
}
