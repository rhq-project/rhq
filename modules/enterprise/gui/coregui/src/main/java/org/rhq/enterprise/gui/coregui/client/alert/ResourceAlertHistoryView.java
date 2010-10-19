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
package org.rhq.enterprise.gui.coregui.client.alert;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.resource.composite.ResourceComposite;

/**
 * @author Joseph Marques
 */
public class ResourceAlertHistoryView extends AlertHistoryView {

    public static ResourceAlertHistoryView get(ResourceComposite composite) {
        String tableTitle = "Resource Alert History";
        EntityContext context = EntityContext.forResource(composite.getResource().getId());
        boolean hasWriteAccess = composite.getResourcePermission().isAlert();
        return new ResourceAlertHistoryView(tableTitle, context, hasWriteAccess);
    }

    private ResourceAlertHistoryView(String tableTitle, EntityContext context, boolean hasWriteAccess) {
        super(tableTitle, context, hasWriteAccess);
    }
}
