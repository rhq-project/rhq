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
package org.rhq.coregui.client.dashboard.portlets.resource;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.async.CountDownLatch;

/**
 * This portlet allows the end user to customize the metric display
 *
 * @author Simeon Pinder
 */
public class ResourceMetricsPortlet extends GroupMetricsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceMetrics";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_metrics();

    private int resourceId = -1;

    public ResourceMetricsPortlet(int resourceId) {
        super(EntityContext.forResource(-1));
        this.resourceId = resourceId;
    }

    private volatile Resource resource = null;

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.Resource != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new ResourceMetricsPortlet(context.getResourceId());
        }
    }

    @Override
    protected void showPopupWithChart(String title, MeasurementDefinition md) {
        ChartViewWindow window = new ChartViewWindow(title, "", refreshablePortlet);
        D3GraphListView graphView = D3GraphListView.createSingleGraph(resource, md.getId(), true);
        window.addItem(graphView);
        window.show();
    }

    @Override
    protected DynamicForm getEmptyDataForm() {
        return AbstractActivityView.createEmptyDisplayRow(AbstractActivityView.RECENT_MEASUREMENTS_NONE);
    }

    @Override
    protected String getSeeMoreLink() {
        return LinkManager.getResourceMonitoringGraphsLink(resourceId);
    }

    @Override
    protected MeasurementScheduleCriteria addFilterKey(MeasurementScheduleCriteria criteria) {
        criteria.addFilterResourceId(resourceId);
        return criteria;
    }

    @Override
    protected void fetchEnabledMetrics(List<MeasurementSchedule> schedules, int[] definitionArrayIds,
        final String[] displayOrder, final Map<String, MeasurementDefinition> measurementDefMap, final VLayout layout) {
        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
            CustomDateRangeState.getInstance().getStartTime(), CustomDateRangeState.getInstance().getEndTime(), 60,
            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent metrics charting data for resource [" + resourceId + "]:"
                        + caught.getMessage());
                    setRefreshing(false);
                }

                @Override
                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                    renderData(results, displayOrder, measurementDefMap, layout);
                }
            }

        );
    }

    @Override
    protected void fetchResourceType(final CountDownLatch latch, final VLayout layout) {
        //locate resource reference
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(this.resourceId);

        //locate the resource
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource resource composite for resource [" + resourceId + "]:"
                        + caught.getMessage());
                    setRefreshing(false);
                    latch.countDown();
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> results) {
                    if (!results.isEmpty()) {
                        final ResourceComposite resourceComposite = results.get(0);
                        resource = resourceComposite.getResource();
                        // Load the fully fetched ResourceType.
                        ResourceType resourceType = resource.getResourceType();
                        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceType.getId(),
                            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                            new ResourceTypeRepository.TypeLoadedCallback() {
                                public void onTypesLoaded(ResourceType type) {
                                    resource.setResourceType(type);
                                    latch.countDown();
                                }
                            });
                    } else {
                        latch.countDown();
                    }
                }
            });
    }
}
