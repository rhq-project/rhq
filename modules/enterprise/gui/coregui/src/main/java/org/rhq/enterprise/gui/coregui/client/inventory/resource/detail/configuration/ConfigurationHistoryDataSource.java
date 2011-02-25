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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;

/**
 * A data source that loads information about all the configuration changes that happened
 * for a resource or across all inventory.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ConfigurationHistoryDataSource extends AbstractConfigurationHistoryDataSource<ResourceConfigurationUpdate> {

    public ConfigurationHistoryDataSource() {
        super();
    }

    @Override
    protected String getConfigurationUpdateStatusIcon(ConfigurationUpdateStatus status) {
        return ImageManager.getResourceConfigurationIcon(status);
    }

    @Override
    protected String getGroupConfigurationUpdateHistoryLink(Integer groupId, Number value) {
        return LinkManager.getGroupResourceConfigurationUpdateHistoryLink(groupId, value.intValue());
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {

        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);
        criteria.fetchGroupConfigurationUpdate(true);

        criteria.setPageControl(getPageControl(request));

        final Integer resourceId = (Integer) request.getCriteria().getValues().get(CriteriaField.RESOURCE_ID);
        if (resourceId != null) {
            criteria.addFilterResourceIds(resourceId);
        }

        getConfigurationService().findResourceConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_configurationHistory_error_fetchFailure(),
                        caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(final PageList<ResourceConfigurationUpdate> result) {
                    final ListGridRecord[] records = buildRecords(result);
                    if (resourceId == null) {
                        response.setData(records);
                        response.setTotalRows(result.getTotalSize());
                        processResponse(request.getRequestId(), response);
                        return; // we can finish now, we don't need any additional information
                    }

                    // we are obtaining a single resource's history items. Let's find out which is
                    // its latest, current config item so we can mark it as such
                    getConfigurationService().getLatestResourceConfigurationUpdate(resourceId.intValue(),
                        new AsyncCallback<ResourceConfigurationUpdate>() {
                            @Override
                            public void onSuccess(ResourceConfigurationUpdate latestResult) {
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
                                Log.error("cannot get latest resource config", caught);
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
}
