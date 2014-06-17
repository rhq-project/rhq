/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.help;

import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.components.FullHTMLPane;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.Log;

public class RhAccessView extends FullHTMLPane implements BookmarkableView {

    public RhAccessView(String url) {
        super(url);
    }

    public RhAccessView() {
        this("/rha/support.html#/search");
    }

    public static final ViewName VIEW_ID = new ViewName("Support", "Red Hat Access");

    public static final ViewName PAGE_SEARCH = new ViewName("Search", "Search");
    public static final ViewName PAGE_MY_CASES = new ViewName("MyCases", "My Cases");
    public static final ViewName PAGE_NEW_CASE = new ViewName("NewCase", "Open Case");
    public static final ViewName PAGE_RESOURCE_CASE = new ViewName("ResourceCase", "Open Support Case");

    private int resourceId;
    private ResourceComposite resourceComposite;
    List<MeasurementDataTrait> traits;

    @Override
    public void renderView(ViewPath viewPath) {

        String sessionId = String.valueOf("?sid=" + UserSessionManager.getSessionSubject().getSessionId());

        if (viewPath.isEnd()) {
            setContentsURL("/rha/support.html#/search" + sessionId);
            return;
        }
        String viewId = viewPath.getCurrent().getPath();
        if (PAGE_SEARCH.getName().equals(viewId)) {
            setContentsURL("/rha/support.html#/search" + sessionId);
        }
        else if (PAGE_MY_CASES.getName().equals(viewId)) {
            setContentsURL("/rha/support.html#/case/list" + sessionId);
        }
        else if (PAGE_NEW_CASE.getName().equals(viewId)) {
            setContentsURL("/rha/support.html#/case/new" + sessionId);
        } else if (viewId.startsWith(PAGE_RESOURCE_CASE.getName())) {
            String resourceId = viewPath.getNext().getPath();
            try {
                int resId = Integer.parseInt(resourceId);
                loadSelectedItem(resId, viewPath);
            } catch (NumberFormatException e) {

            }

        }
        markForRedraw();
    }

    protected void loadSelectedItem(final int resourceId, final ViewPath viewPath) {
        this.resourceId = resourceId;

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchTags(true);
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    //Message message = new Message(MSG.view_inventory_resource_loadFailed(String.valueOf(resourceId)),Message.Severity.Warning);
                    //CoreGUI.goToView(InventoryView.VIEW_ID.getName(), message);
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> result) {
                    if (result.isEmpty()) {
                        //noinspection ThrowableInstanceNeverThrown
                        onFailure(new Exception("Resource with id [" + resourceId + "] does not exist."));
                    } else {
                        resourceComposite = result.get(0);
                        loadResourceType(viewPath);
                    }
                }
            });
    }

    private void loadResourceType(final ViewPath viewPath) {
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
            resourceComposite.getResource().getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    try {
                        resourceComposite.getResource().setResourceType(type);
                        loadTraitValues();
                    } finally {

                    }
                }
            });
    }

    private void loadTraitValues() {
        final Resource resource = resourceComposite.getResource();
        GWTServiceLookup.getMeasurementDataService().findCurrentTraitsForResource(resource.getId(),
            DisplayType.SUMMARY, new AsyncCallback<List<MeasurementDataTrait>>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(List<MeasurementDataTrait> result) {
                    String productName = "";
                    String productVersion = "";
                    for (MeasurementDataTrait trait : result) {
                        Log.info(trait.getName());
                        if (trait.getName().equals("Product Version")) {
                            productVersion = trait.getValue();
                            Log.info(trait.getValue());
                        }
                        if (trait.getName().equals("Product Name")) {
                            productName = trait.getValue();
                            Log.info(trait.getValue());
                        }
                    }
                    if ("EAP".equals(productName)) {
                        productName = "Red Hat JBoss Enterprise Application Platform";

                    }
                    if ("Data Grid".equals(productName)) {
                        productName = "Red Hat JBoss Data Grid";
                    }
                    // we need to strip down .GA suffix, since it is not present in RHA
                    productVersion = productVersion.replaceAll("\\.GA.*", "");
                    String sessionId = String.valueOf("&sid=" + UserSessionManager.getSessionSubject().getSessionId());
                    setContentsURL("/rha/support.html#/resource-case/?resourceId=" + resourceId + "&product="
                        + productName + "&version=" + productVersion + sessionId);
                    markForRedraw();
                    Log.info("content url set " + productVersion + " " + productName);
                }
            });
    }

}
