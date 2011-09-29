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

import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;

/**
 * @author John Mazzitelli
 */
public class ResourceDriftChangeSetsTreeDataSource extends AbstractDriftChangeSetsTreeDataSource {

    private final EntityContext context;
    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    public ResourceDriftChangeSetsTreeDataSource(boolean canManageDrift, EntityContext context) {
        super(canManageDrift);

        if (context.type != EntityContext.Type.Resource) {
            throw new IllegalArgumentException("wrong context: " + context);
        }

        this.context = context;
    }

    @Override
    protected void fetchDriftConfigurations(final DSRequest request, final DSResponse response) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(context.getResourceId());
        criteria.fetchDriftConfigurations(true);
        this.resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onSuccess(PageList<Resource> result) {
                Set<DriftConfiguration> driftConfigs = result.get(0).getDriftConfigurations();
                response.setData(buildRecords(driftConfigs));
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_drift_snapshots_tree_loadFailure(), caught);
                response.setStatus(DSResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    protected GenericDriftChangeSetCriteria getDriftChangeSetCriteria(final DSRequest request) {
        GenericDriftChangeSetCriteria criteria = super.getDriftChangeSetCriteria(request);
        criteria.addFilterResourceId(this.context.resourceId);
        return criteria;
    }

}
