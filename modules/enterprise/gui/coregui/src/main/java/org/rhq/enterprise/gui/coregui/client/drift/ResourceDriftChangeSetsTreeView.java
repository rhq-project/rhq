/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.drift;

import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.LinkManager;

/**
 * @author John Mazzitelli
 */
public class ResourceDriftChangeSetsTreeView extends AbstractDriftChangeSetsTreeView {

    private final EntityContext context;

    public ResourceDriftChangeSetsTreeView(String locatorId, boolean canManageDrift, EntityContext context) {

        super(locatorId, canManageDrift);

        if (context.type != EntityContext.Type.Resource) {
            throw new IllegalArgumentException("wrong context: " + context);
        }

        this.context = context;
    }

    protected String getNodeTargetLink(TreeNode node) {
        String driftIdStr = node.getAttribute(DriftChangeSetsTreeDataSource.ATTR_ID).split("_")[1];
        String path = LinkManager.getDriftHistoryLink(this.context.resourceId, Integer.valueOf(driftIdStr));
        return path;
    }

}
