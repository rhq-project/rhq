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

import com.smartgwt.client.widgets.Canvas;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.enterprise.gui.coregui.client.operation.OperationHistoryView;

/**
 * A table that displays all of the operation histories for a particular Resource.
 *
 * @author Ian Springer
 * @author Jay Shaughnessy 
 */
public class ResourceOperationHistoryListView extends OperationHistoryView {

    private ResourceComposite resourceComposite;

    public ResourceOperationHistoryListView(String locatorId, ResourceComposite resourceComposite) {
        super(locatorId, null, EntityContext.forResource(resourceComposite.getResource().getId()));

        this.resourceComposite = resourceComposite;
    }

    @Override
    protected boolean hasControlPermission() {
        return this.resourceComposite.getResourcePermission().isControl();
    }

    @Override
    public Canvas getDetailsView(int id) {
        return new ResourceOperationHistoryDetailsView(this.extendLocatorId("DetailsView"));
    }

}
