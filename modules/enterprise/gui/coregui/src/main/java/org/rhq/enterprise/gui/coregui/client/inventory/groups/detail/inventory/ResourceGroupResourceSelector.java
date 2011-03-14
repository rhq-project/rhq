/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourceSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;

/**
 * @author Jay Shaughnessy
 */
public class ResourceGroupResourceSelector extends ResourceSelector {

    Collection<Resource> resources;

    public ResourceGroupResourceSelector(String locatorId, Collection<Resource> resources,
        ResourceType resourceTypeFilter, boolean forceResourceTypeFilter) {

        super(locatorId, resourceTypeFilter, forceResourceTypeFilter);

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
                    ResourceGroupResourceSelector.super.onInit();
                }
            });
        } else {
            super.onInit();
        }
    }

}
