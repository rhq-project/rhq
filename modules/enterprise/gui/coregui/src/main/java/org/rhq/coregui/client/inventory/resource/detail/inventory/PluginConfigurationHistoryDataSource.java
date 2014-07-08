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
package org.rhq.coregui.client.inventory.resource.detail.inventory;

import java.util.HashSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.detail.configuration.AbstractConfigurationHistoryDataSource;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.Log;

/**
 * A data source that loads information about all the plugin configuration changes that happened
 * for a resource or across all inventory.
 *
 * @author John Mazzitelli
 */
public class PluginConfigurationHistoryDataSource extends
    AbstractConfigurationHistoryDataSource<PluginConfigurationUpdate, PluginConfigurationUpdateCriteria> {

    public PluginConfigurationHistoryDataSource() {
        super();
    }

    @Override
    protected String getConfigurationUpdateStatusIcon(ConfigurationUpdateStatus status) {
        return ImageManager.getPluginConfigurationIcon(status);
    }

    @Override
    protected String getGroupConfigurationUpdateHistoryLink(Integer groupId, Number value) {        
        return LinkManager.getGroupPluginConfigurationUpdateHistoryLink(EntityContext.forGroup(groupId), value.intValue());
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final PluginConfigurationUpdateCriteria criteria) {

        final Integer resourceId = (Integer) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID);
        getConfigurationService().findPluginConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<PluginConfigurationUpdate>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_configurationHistory_error_fetchFailure(),
                        caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(final PageList<PluginConfigurationUpdate> result) {
                    final ListGridRecord[] records = buildRecords(result);
                    if (resourceId == null) {
                        HashSet<Integer> typesSet = new HashSet<Integer>();
                        HashSet<String> ancestries = new HashSet<String>();
                        for (PluginConfigurationUpdate update : result) {
                            Resource resource = update.getResource();
                            typesSet.add(resource.getResourceType().getId());
                            ancestries.add(resource.getAncestry());
                        }

                        // In addition to the types of the result resources, get the types of their ancestry
                        typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
                        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]),
                            new TypesLoadedCallback() {
                                @Override
                                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                                    // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                                    Record[] records = buildRecords(result);
                                    for (Record record : records) {
                                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                                        // Store the types map off the records so we can build a detailed hover string as needed.                      
                                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                                        // Build the decoded ancestry Strings now for display
                                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil
                                            .getAncestryValue(record));
                                    }
                                    response.setData(records);
                                    response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                                    processResponse(request.getRequestId(), response);
                                }
                            });

                        return;
                    }

                    // we are obtaining a single resource's history items. Let's find out which is
                    // its latest, current config item so we can mark it as such
                    getConfigurationService().getLatestPluginConfigurationUpdate(resourceId.intValue(),
                        new AsyncCallback<PluginConfigurationUpdate>() {
                            @Override
                            public void onSuccess(PluginConfigurationUpdate latestResult) {
                                if (latestResult != null) {
                                    for (ListGridRecord record : records) {
                                        boolean latest = record.getAttributeAsInt(Field.ID).intValue() == latestResult
                                            .getId();
                                        record.setAttribute(Field.CURRENT_CONFIG, latest);
                                    }
                                }
                                finish();
                            }

                            @Override
                            public void onFailure(Throwable caught) {
                                // should we show an error message? this just means we can't show any item as the "current" one
                                Log.error("cannot get latest plugin config", caught);
                                finish();
                            }

                            private void finish() {
                                response.setData(records);
                                response.setTotalRows(result.getTotalSize());
                                processResponse(request.getRequestId(), response);
                            }
                        });
                }
            });
    }

    @Override
    protected PluginConfigurationUpdateCriteria createFetchCriteria() {
        return new PluginConfigurationUpdateCriteria();
    }

    @Override
    protected PluginConfigurationUpdateCriteria getFetchCriteria(final DSRequest request) {
        PluginConfigurationUpdateCriteria criteria = super.getFetchCriteria(request);

        criteria.fetchResource(true);
        criteria.fetchGroupConfigurationUpdate(true);

        final Integer resourceId = getFilter(request, CriteriaField.RESOURCE_ID, Integer.class);
        if (resourceId != null) {
            criteria.addFilterResourceIds(resourceId);
        }
        return criteria;
    }

}
