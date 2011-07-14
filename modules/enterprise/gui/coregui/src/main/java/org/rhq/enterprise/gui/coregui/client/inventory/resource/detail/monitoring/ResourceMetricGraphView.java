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
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public class ResourceMetricGraphView extends AbstractMetricGraphView {

    private HTMLFlow resourceTitle;

    public ResourceMetricGraphView(String locatorId) {
        super(locatorId);
    }

    public ResourceMetricGraphView(String locatorId, int resourceId, int definitionId) {
        super(locatorId, resourceId, definitionId);
    }

    public ResourceMetricGraphView(String locatorId, int resourceId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data) {

        super(locatorId, resourceId, def, data);
    }

    protected HTMLFlow getEntityTitle() {
        return resourceTitle;
    }

    protected void renderGraph() {
        if (null == getDefinition()) {

            ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.addFilterId(getEntityId());
            resourceService.findResourcesByCriteria(resourceCriteria, new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_graphs_lookupFailed(), caught);
                }

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
                                resourceTitle = new HTMLFlow(SeleniumUtility.getLocatableHref(url, resource.getName(),
                                    null));
                                resourceTitle.setTooltip(AncestryUtil.getAncestryHoverHTMLForResource(resource, types,
                                    0));

                                ResourceType type = types.get(resource.getResourceType().getId());
                                for (MeasurementDefinition def : type.getMetricDefinitions()) {
                                    if (def.getId() == getDefinitionId()) {
                                        setDefinition(def);

                                        GWTServiceLookup.getMeasurementDataService().findDataForResource(getEntityId(),
                                            new int[] { getDefinitionId() },
                                            System.currentTimeMillis() - (1000L * 60 * 60 * 8),
                                            System.currentTimeMillis(), 60,
                                            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                                public void onFailure(Throwable caught) {
                                                    CoreGUI.getErrorHandler().handleError(
                                                        MSG.view_resource_monitor_graphs_loadFailed(), caught);
                                                }

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

    protected boolean supportsLiveGraphViewDialog() {
        return true;
    }

    protected void displayLiveGraphViewDialog() {
        LiveGraphView.displayAsDialog(getLocatorId(), getEntityId(), getDefinition());
    }

    @Override
    public AbstractMetricGraphView getInstance(String locatorId, int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data) {

        return new ResourceMetricGraphView(locatorId, entityId, def, data);
    }
}
