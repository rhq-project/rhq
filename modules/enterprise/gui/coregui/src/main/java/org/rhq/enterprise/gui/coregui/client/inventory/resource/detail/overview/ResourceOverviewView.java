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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.overview;

import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.components.SimpleCollapsiblePanel;

/**
 * @author Greg Hinkle
 */
public class ResourceOverviewView extends VLayout {

    private Resource resource;


    public ResourceOverviewView(Resource resource) {
        this.resource = resource;
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        ResourceSummaryView summaryView = new ResourceSummaryView();
        summaryView.onResourceSelected(resource);
        SimpleCollapsiblePanel summaryPanel = new SimpleCollapsiblePanel("Summary", summaryView);


        addMember(summaryPanel);


        FullHTMLPane summaryPane = new FullHTMLPane("/rhq/resource/summary/overview-plain.xhtml?id=" + resource.getId());
        addMember(summaryPane);


    }
}
