/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 */
public class ResGroupResourceSelector extends ResourceSelector {
    private static final int MAX_AVAILABLE_RECORDS = 300;

    private Collection<Resource> resources;

    public ResGroupResourceSelector(Collection<Resource> resources, ResourceType resourceTypeFilter,
                                    boolean forceResourceTypeFilter) {

        super(resourceTypeFilter, forceResourceTypeFilter);

        this.resources = resources;

        this.setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {

        if (null != this.resources) {
            // to properly handle ancestry we need to provide the LGRecords with necessary ResourceType info for
            // the initially assigned resources

            HashSet<Integer> typesSet = new HashSet<Integer>();
            HashSet<String> ancestries = new HashSet<String>();
            for (Resource resource : this.resources) {
                typesSet.add(resource.getResourceType().getId());
                ancestries.add(resource.getAncestry());
            }

            // In addition to the types of the result resources, get the types of their ancestry
            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.
                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                    ListGridRecord[] records = (new ResourceDatasource()).buildRecords(resources);

                    for (Record record : records) {
                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                        // Store the types map off the records so we can build a detailed hover string as needed.
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);
                    }

                    setAssigned(records);
                    ResGroupResourceSelector.super.onInit();
                }
            });
        } else {
            super.onInit();
        }
    }

    @Override
    protected int getMaxAvailableRecords() {
        return MAX_AVAILABLE_RECORDS;
    }

    @Override
    protected RPCDataSource<Resource, ResourceCriteria> getDataSource() {
        return new SelectedResourcesAwareDS();
    }

    private class SelectedResourcesAwareDS extends SelectedResourceDataSource {

        @Override
        public void executeFetch(final DSRequest request, final DSResponse response, final ResourceCriteria criteria) {
            getResourceService().findGroupMemberCandidateResources(criteria, getSelectedResourceIds(),
                new AsyncCallback<PageList<Resource>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                        response.setStatus(RPCResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onSuccess(PageList<Resource> result) {
                        dataRetrieved(result, response, request);
                    }
                });
        }

        private int[] getSelectedResourceIds() {
            ListGridRecord[] assignedRecords = assignedGrid.getRecords();
            int[] selectedResourceIds = new int[assignedRecords.length];
            for (int i = 0; i < assignedRecords.length; i++) {
                ListGridRecord assignedRecord = assignedRecords[i];
                selectedResourceIds[i] = assignedRecord.getAttributeAsInt(getSelectorKey());
            }
            return selectedResourceIds;
        }
    }
}
