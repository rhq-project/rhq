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

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;

import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;
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
        ListGrid errorsGrid = buildErrorListGrid(resourceComposite);
        if (errorsGrid != null) {
            addMember(errorsGrid);
        }
    }

    private static ListGrid buildErrorListGrid(ResourceComposite resourceComposite) {
        final Resource resource = resourceComposite.getResource();
        ListGrid errorsGrid = null;
        if (resource.getResourceErrors() != null && resource.getResourceErrors().size() > 0) {
            errorsGrid = new ListGrid();

            ListGridField summaryField = new ListGridField("summary", "Errors");
            summaryField.addRecordClickHandler(new RecordClickHandler() {

                @Override
                public void onRecordClick(RecordClickEvent event) {
                    String detail = event.getRecord().getAttribute("detail");
                    SC.say("Error details", detail);
                }
            });

            errorsGrid.setFields(summaryField);

            List<ListGridRecord> errorList = new ArrayList<ListGridRecord>();
            for (ResourceError error : resource.getResourceErrors()) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("summary", error.getSummary());
                record.setAttribute("detail", error.getDetail());
                errorList.add(record);
            }

            errorsGrid.setRecords(errorList.toArray(new ListGridRecord[errorList.size()]));
        }

        return errorsGrid;
    }
}
