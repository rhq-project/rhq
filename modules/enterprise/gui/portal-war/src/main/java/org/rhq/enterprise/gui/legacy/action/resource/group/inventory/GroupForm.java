/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.action.resource.group.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 */
public class GroupForm extends BaseValidatorForm {
    //private String typeAndResourceTypeId;
    private String category;
    private List<Map<String, Object>> platformTypes;
    private List<Map<String, Object>> serverTypes;
    private List<Map<String, Object>> serviceTypes;
    private List groupTypes;
    private Integer[] resourceIds;
    private String resourceTypeName;
    private Integer resourceTypeId;
    private boolean recursive;

    private String name;
    private String description;
    private String location;

    private Integer groupId;

    /**
     * @return the number of items of compatible types
     */
    public Integer getCompatibleCount() {
        if ((platformTypes == null) || (serverTypes == null) || (serviceTypes == null)) {
            return new Integer(0);
        }

        return new Integer(platformTypes.size() + serverTypes.size() + serviceTypes.size());
    }

    /**
     * @return the number of items of service types
     */
    public Integer getClusterCount() {
        if (serviceTypes == null) {
            return new Integer(0);
        }

        return new Integer(serviceTypes.size());
    }

    /**
     * Returns the platformTypes.
     *
     * @return List
     */
    public List getPlatformTypes() {
        return platformTypes;
    }

    /**
     * Returns the number of platformTypes.
     *
     * @return List
     */
    public Integer getPlatformTypeCount() {
        if (platformTypes == null) {
            return new Integer(0);
        }

        return new Integer(platformTypes.size());
    }

    /**
     * Returns the serverTypes.
     *
     * @return List
     */
    public List getServerTypes() {
        return serverTypes;
    }

    /**
     * Returns the number of serverTypes.
     *
     * @return Integer
     */
    public Integer getServerTypeCount() {
        if (serverTypes == null) {
            return new Integer(0);
        }

        return new Integer(serverTypes.size());
    }

    /**
     * Returns the serviceTypes.
     *
     * @return List
     */
    public List getServiceTypes() {
        return serviceTypes;
    }

    /**
     * Returns the serviceTypes.
     *
     * @return List
     */
    public Integer getServiceTypeCount() {
        if (serviceTypes == null) {
            return new Integer(0);
        }

        return new Integer(serviceTypes.size());
    }

    /**
     * Sets the platformTypes.
     *
     * @param platformTypes The platformTypes to set
     */
    public void setPlatformTypes(List<ResourceType> platformTypes) {
        this.platformTypes = getOptionListItemsWithDashes(platformTypes);
    }

    /**
     * Sets the serverTypes.
     *
     * @param serverTypes The serverTypes to set
     */
    public void setServerTypes(List<ResourceType> serverTypes) {
        this.serverTypes = getOptionListItemsWithDashes(serverTypes);
    }

    /**
     * Sets the serviceTypes.
     *
     * @param serviceTypes The serviceTypes to set
     */
    public void setServiceTypes(List<ResourceType> serviceTypes) {
        this.serviceTypes = getOptionListItemsWithDashes(serviceTypes);
    }

    private List<Map<String, Object>> getOptionListItemsWithDashes(List<ResourceType> types) {
        Map<String, Integer> typeNameCounts = new HashMap<String, Integer>();
        for (ResourceType type : types) {
            if (typeNameCounts.containsKey(type.getName()) == false) {
                typeNameCounts.put(type.getName(), 1);
            } else {
                typeNameCounts.put(type.getName(), typeNameCounts.get(type.getName()) + 1);
            }
        }

        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        if (types != null) {
            for (ResourceType type : types) {
                // GroupTypeForm needs this data format - a map with "value" and "label" keys
                Map<String, Object> map = new HashMap<String, Object>(2);
                map.put("value", type.getId());
                Integer count = typeNameCounts.get(type.getName());
                map.put("label", "- " + type.getName() + (count > 1 ? (" (" + type.getPlugin() + " Plugin)") : ""));
                items.add(map);
            }
        }

        return items;
    }

    /**
     * @return List
     */
    public List getGroupTypes() {
        return groupTypes;
    }

    /**
     * Sets the groupTypes.
     *
     * @param groupTypes The groupTypes to set
     */
    public void setGroupTypes(List groupTypes) {
        this.groupTypes = groupTypes;
    }

    public Integer[] getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Integer[] resourceIds) {
        this.resourceIds = resourceIds;
    }

    /**
     * over-ride the validate method. need to do validation.
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (shouldValidate(mapping, request)) {
            if (errors == null) {
                errors = new ActionErrors();
            }

            if (category.equals(GroupCategory.COMPATIBLE.name())) {
                if (resourceTypeId == null) {
                    errors.add("resourceTypeId",
                        new ActionMessage("resource.group.inventory.error." + "ResourceTypeId"));
                }
            }
        }

        return errors;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public GroupCategory getGroupCategory() {
        return GroupCategory.valueOf(category);
    }

    public void setGroupCategory(GroupCategory category) {
        this.category = category.name();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public void setResourceTypeName(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }

    public void loadResourceGroup(ResourceGroup group) {
        this.name = group.getName();
        this.description = group.getDescription();
        this.location = "";
        this.groupId = group.getId();
    }

    public void updateResourceGroup(ResourceGroup group) {
        if (name != null) {
            group.setName(name);
        }

        if (description != null) {
            group.setDescription(description);
        }

        /*
        if (location != null) {
            group.setLocation(location);
        }
        */
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
}