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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

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

        setDataSource(new ResourceDriftChangeSetsTreeDataSource(canManageDrift, context));
    }

    @Override
    protected String getNodeDetailsLink(TreeNode node) {
        if (node instanceof DriftTreeNode) {
            String driftId = ((DriftTreeNode) node).getDriftId();
            String path = LinkManager.getDriftHistoryLink(this.context.resourceId, 0, driftId);
            return path;
        } else if (node instanceof DriftDefinitionTreeNode) {
            int driftDefId = ((DriftDefinitionTreeNode) node).getDriftDefinitionId();
            String path = LinkManager.getDriftDefinitionEditLink(this.context.resourceId, driftDefId);
            return path;
        }
        return null;
    }

    @Override
    protected void deleteDriftDefinition(DriftDefinition doomedDriftDef) {
        GWTServiceLookup.getDriftService().deleteDriftDefinitionsByContext(context,
            new String[] { doomedDriftDef.getName() }, new AsyncCallback<Integer>() {
                public void onSuccess(Integer resultCount) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_drift_success_deleteDefs(String.valueOf(resultCount)),
                            Message.Severity.Info));
                    refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_deleteDefs(), caught);
                }
            });
    }

    @Override
    protected void detectDrift(DriftDefinition driftDef) {
        GWTServiceLookup.getDriftService().detectDrift(context, driftDef, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_drift_success_detectNow(), Message.Severity.Info));
                refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_failure_detectNow(), caught);
            }
        });
    }

}
