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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricGraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

/**
 * @deprecated should be replaced with new d3 graph views
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
@Deprecated
public class ResourceMetricGraphView extends AbstractMetricGraphView {

    private HTMLFlow resourceTitle;

    public ResourceMetricGraphView() {
        super();
    }

    public ResourceMetricGraphView(int resourceId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data) {

        super(resourceId, def, data);
    }

    @Override
    protected HTMLFlow getEntityTitle() {
        return resourceTitle;
    }

    @Override
    protected void renderGraph() {
        if (null == getDefinition()) {

            ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.addFilterId(getEntityId());
            resourceService.findResourcesByCriteria(resourceCriteria, new AsyncCallback<PageList<Resource>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
                }

                @Override
                public void onSuccess(PageList<Resource> result) {
                    if (result.isEmpty()) {
                        return;
                    }

                    final Resource resource = result.get(0);
                    HashSet<Integer> typesSet = new HashSet<Integer>();
                    typesSet.add(resource.getResourceType().getId());
                    HashSet<String> ancestries = new HashSet<String>();
                    ancestries.add(resource.getAncestry());
                    // In addition to the types of the result resources, get the types of their ancestry
                    typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                    ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                        typesSet.toArray(new Integer[typesSet.size()]),
                        EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                        new ResourceTypeRepository.TypesLoadedCallback() {

                            public void onTypesLoaded(Map<Integer, ResourceType> types) {
                                String url = LinkManager.getResourceLink(resource.getId());
                                resourceTitle = new HTMLFlow(LinkManager.getHref(url, resource.getName()));
                                resourceTitle.setTooltip(AncestryUtil.getAncestryHoverHTMLForResource(resource, types,
                                    0));

                                ResourceType type = types.get(resource.getResourceType().getId());
                                for (MeasurementDefinition def : type.getMetricDefinitions()) {
                                    if (def.getId() == getDefinitionId()) {
                                        setDefinition(def);

                                        GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(
                                            getEntityId(), new int[] { getDefinitionId() }, 8,
                                            MeasurementUtils.UNIT_HOURS, 60,
                                            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    CoreGUI.getErrorHandler().handleError(
                                                        MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                }

                                                @Override
                                                public void onSuccess(
                                                    List<List<MeasurementDataNumericHighLowComposite>> result) {
                                                    setData(result.get(0));

                                                    drawGraph();
                                                }
                                            });
                                    }
                                }
                            }
                        });
                }
            });

        } else {
            drawGraph();
        }
    }

    @Override
    protected boolean supportsLiveGraphViewDialog() {
        return true;
    }

    @Override
    protected void displayLiveGraphViewDialog() {
        LiveGraphView.displayAsDialog(getEntityId(), getDefinition());
    }

    @Override
    public AbstractMetricGraphView getInstance(int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data) {

        return new ResourceMetricGraphView(entityId, def, data);
    }
}
