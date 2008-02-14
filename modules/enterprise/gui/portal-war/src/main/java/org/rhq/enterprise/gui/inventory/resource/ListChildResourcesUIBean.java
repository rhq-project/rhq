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
package org.rhq.enterprise.gui.inventory.resource;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The JSF managed bean for the Child Resources section of the Resource Inventory page
 * (/rhq/resource/inventory/view.xhtml).
 *
 * @author Ian Springer
 */
public class ListChildResourcesUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListChildResourcesUIBean";

    private static final String FORM_ID = "childResourcesForm";
    private static final String CHILD_TYPE_FILTER_INPUT_ID = "childTypeFilter";
    private static final String CHILD_TYPE_FILTER_INPUT_CLIENT_ID = FORM_ID + ':' + CHILD_TYPE_FILTER_INPUT_ID;
    private static final String CHILD_TYPE_FILTER_VALUE_ALL = "ALL";
    private static final String NBSP = "&nbsp;";
    private static final String DEFAULT_RESOURCE_TYPE_ID = "SELECT_TYPE";

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

    private String childTypeFilter;

    private List<ResourceType> utilizedChildServerTypes;
    private List<ResourceType> utilizedChildServiceTypes;
    private int utilizedChildTypesCount;

    private List<ResourceType> childServerTypes;
    private List<ResourceType> childServiceTypes;
    private int childTypesCount;

    private List<ResourceType> creatableChildServerTypes;
    private List<ResourceType> creatableChildServiceTypes;
    private int creatableChildTypesCount;

    private List<ResourceType> manuallyAddableChildServerTypes;
    private List<ResourceType> manuallyAddableChildServiceTypes;
    private int manuallyAddableChildTypesCount;

    private int deletableChildTypesCount;

    private String manuallyAddResourceTypeId;
    private String createNewResourceTypeId;

    public ListChildResourcesUIBean() {
        this.utilizedChildServerTypes = this.resourceTypeManager.getUtilizedChildResourceTypesByCategory(
            EnterpriseFacesContextUtility.getSubject(), EnterpriseFacesContextUtility.getResource(),
            ResourceCategory.SERVER);
        this.utilizedChildServiceTypes = this.resourceTypeManager.getUtilizedChildResourceTypesByCategory(
            EnterpriseFacesContextUtility.getSubject(), EnterpriseFacesContextUtility.getResource(),
            ResourceCategory.SERVICE);
        this.utilizedChildTypesCount = this.utilizedChildServerTypes.size() + this.utilizedChildServiceTypes.size();

        this.childServerTypes = this.resourceTypeManager.getChildResourceTypesByCategory(EnterpriseFacesContextUtility
            .getSubject(), EnterpriseFacesContextUtility.getResource(), ResourceCategory.SERVER);
        this.childServiceTypes = this.resourceTypeManager.getChildResourceTypesByCategory(EnterpriseFacesContextUtility
            .getSubject(), EnterpriseFacesContextUtility.getResource(), ResourceCategory.SERVICE);
        this.childTypesCount = this.childServerTypes.size() + this.childServiceTypes.size();

        // TODO: Singleton resource types already represented in the resource's children should not be listed in the
        //       Manually Add and Create New pulldown menus. (ips, 09/10/07)
        this.creatableChildServerTypes = getCreatableResourceTypes(this.childServerTypes);
        this.creatableChildServiceTypes = getCreatableResourceTypes(this.childServiceTypes);
        this.creatableChildTypesCount = this.creatableChildServerTypes.size() + this.creatableChildServiceTypes.size();

        this.manuallyAddableChildServerTypes = getManuallyAddableResourceTypes(this.childServerTypes);
        this.manuallyAddableChildServiceTypes = getManuallyAddableResourceTypes(this.childServiceTypes);
        this.manuallyAddableChildTypesCount = this.manuallyAddableChildServerTypes.size()
            + this.manuallyAddableChildServiceTypes.size();

        List<ResourceType> deletableChildServerTypes = getCreatableResourceTypes(this.childServerTypes);
        List<ResourceType> deletableChildServiceTypes = getCreatableResourceTypes(this.childServiceTypes);
        this.deletableChildTypesCount = deletableChildServerTypes.size() + deletableChildServiceTypes.size();

        this.manuallyAddResourceTypeId = DEFAULT_RESOURCE_TYPE_ID;
        this.createNewResourceTypeId = DEFAULT_RESOURCE_TYPE_ID;
    }

    public List<SelectItem> getUtilizedChildServerTypes() {
        List<SelectItem> utilizedChildServerTypes = convertToSelectItems(this.utilizedChildServerTypes, true);
        if (!this.utilizedChildServerTypes.isEmpty()) {
            utilizedChildServerTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVER, false));
        }

        return utilizedChildServerTypes;
    }

    public List<SelectItem> getUtilizedChildServiceTypes() {
        List<SelectItem> utilizedChildServiceTypes = convertToSelectItems(this.utilizedChildServiceTypes, true);
        if (!this.utilizedChildServiceTypes.isEmpty()) {
            utilizedChildServiceTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVICE, false));
        }

        return utilizedChildServiceTypes;
    }

    public int getUtilizedChildTypesCount() {
        return this.utilizedChildTypesCount;
    }

    public List<SelectItem> getChildServerTypes() {
        List<SelectItem> childServerTypes = convertToSelectItems(this.childServerTypes, false);
        if (!this.childServerTypes.isEmpty()) {
            childServerTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVER, true));
        }

        return childServerTypes;
    }

    public List<SelectItem> getChildServiceTypes() {
        List<SelectItem> childServiceTypes = convertToSelectItems(this.childServiceTypes, false);
        if (!this.childServiceTypes.isEmpty()) {
            childServiceTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVICE, true));
        }

        return childServiceTypes;
    }

    public int getChildTypesCount() {
        return this.childTypesCount;
    }

    public List<SelectItem> getCreatableChildServerTypes() {
        // TODO: jdobies, Jun 27, 2007: Add authz check to make sure the user can create
        List<SelectItem> creatableChildServerTypes = convertToSelectItems(this.creatableChildServerTypes, false);
        if (!this.creatableChildServerTypes.isEmpty()) {
            // add label...
            creatableChildServerTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVER, true));
        }

        return creatableChildServerTypes;
    }

    public List<SelectItem> getCreatableChildServiceTypes() {
        // TODO: jdobies, Jun 27, 2007: Add authz check to make sure the user can create
        List<SelectItem> creatableChildServiceTypes = convertToSelectItems(this.creatableChildServiceTypes, false);
        if (!this.creatableChildServiceTypes.isEmpty()) {
            // add label...
            creatableChildServiceTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVICE, true));
        }

        return creatableChildServiceTypes;
    }

    public List<SelectItem> getManuallyAddableChildServerTypes() {
        List<SelectItem> manuallyAddableChildServerTypes = convertToSelectItems(this.manuallyAddableChildServerTypes,
            false);
        if (!this.manuallyAddableChildServerTypes.isEmpty()) {
            // add label...
            manuallyAddableChildServerTypes.add(0, createSelectItemForResourceCategory(ResourceCategory.SERVER, true));
        }

        return manuallyAddableChildServerTypes;
    }

    public List<SelectItem> getManuallyAddableChildServiceTypes() {
        List<SelectItem> manuallyAddableChildServiceTypes = convertToSelectItems(this.manuallyAddableChildServiceTypes,
            false);
        if (!this.manuallyAddableChildServiceTypes.isEmpty()) {
            // add label...
            manuallyAddableChildServiceTypes
                .add(0, createSelectItemForResourceCategory(ResourceCategory.SERVICE, true));
        }

        return manuallyAddableChildServiceTypes;
    }

    public int getCreatableChildTypesCount() {
        return this.creatableChildTypesCount;
    }

    public int getDeletableChildTypesCount() {
        return this.deletableChildTypesCount;
    }

    public int getManuallyAddableChildTypesCount() {
        return this.manuallyAddableChildTypesCount;
    }

    public String getChildTypeFilter() {
        return this.childTypeFilter;
    }

    public void setChildTypeFilter(String childTypeFilter) {
        this.childTypeFilter = childTypeFilter;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListChildResourcesDataModel(PageControlView.ChildResourcesList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public String getManuallyAddResourceTypeId() {
        return this.manuallyAddResourceTypeId;
    }

    public void setManuallyAddResourceTypeId(String resourceTypeId) {
        this.manuallyAddResourceTypeId = resourceTypeId;
    }

    public String getCreateNewResourceTypeId() {
        return createNewResourceTypeId;
    }

    public void setCreateNewResourceTypeId(String resourceTypeId) {
        this.createNewResourceTypeId = resourceTypeId;
    }

    public void initChildTypeFilter() {
        // As a workaround for JSF calling getRowData() prior to calling setChildTypeFilter(), bypass JSF and set the
        // field ourselves.
        if (this.childTypeFilter == null) {
            this.childTypeFilter = FacesContextUtility.getOptionalRequestParameter(CHILD_TYPE_FILTER_INPUT_CLIENT_ID);
        }
    }

    private List<ResourceType> getManuallyAddableResourceTypes(List<ResourceType> resourceTypes) {
        // TODO: Do this via a separate SQL query rather than Java code.
        List<ResourceType> manuallyAddableResourceTypes = new ArrayList<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            if (resourceType.isSupportsManualAdd()) {
                manuallyAddableResourceTypes.add(resourceType);
            }
        }

        return manuallyAddableResourceTypes;
    }

    private List<ResourceType> getCreatableResourceTypes(List<ResourceType> resourceTypes) {
        // TODO: Do this via a separate SQL query rather than Java code.
        List<ResourceType> creatableResourceTypes = new ArrayList<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            if (resourceType.isCreatable()) {
                creatableResourceTypes.add(resourceType);
            }
        }

        return creatableResourceTypes;
    }

    private List<ResourceType> getDeleteableResourceTypes(List<ResourceType> resourceTypes) {
        // TODO: Do this via a separate SQL query rather than Java code.
        List<ResourceType> deletableResourceTypes = new ArrayList<ResourceType>();
        for (ResourceType resourceType : resourceTypes) {
            if (resourceType.isDeletable()) {
                deletableResourceTypes.add(resourceType);
            }
        }

        return deletableResourceTypes;
    }

    private SelectItem createSelectItemForResourceCategory(ResourceCategory resourceCategory, boolean disable) {
        SelectItem selectItem = new SelectItem(resourceCategory.name(), resourceCategory.getDisplayName() + "s");
        selectItem.setDisabled(disable);
        return selectItem;
    }

    private List<SelectItem> convertToSelectItems(List<ResourceType> resourceTypes, boolean pluralize) {
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ResourceType resourceType : resourceTypes) {
            String label = NBSP + " - " + ((pluralize) ? pluralize(resourceType.getName()) : resourceType.getName());
            SelectItem selectItem = new SelectItem(resourceType.getId(), label);
            selectItem.setEscape(false); // so that the ampersands in the non-blanking spaces will not be escaped to &amp;
            selectItems.add(selectItem);
        }

        return selectItems;
    }

    private String pluralize(String name) {
        return (name.endsWith("s")) ? name : (name + "s");
    }

    protected class ListChildResourcesDataModel extends PagedListDataModel<ResourceComposite> {
        private static final String DEFAULT_SORT_COLUMN = "res.name";

        ListChildResourcesDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        @Override
        public PageList<ResourceComposite> fetchPage(PageControl pageControl) {
            pageControl.initDefaultOrderingField(DEFAULT_SORT_COLUMN);
            ResourceCategory resourceCategory = null;
            int resourceTypeId = -1;
            ListChildResourcesUIBean.this.initChildTypeFilter();
            if ((ListChildResourcesUIBean.this.childTypeFilter != null)
                && !ListChildResourcesUIBean.this.childTypeFilter.equals(CHILD_TYPE_FILTER_VALUE_ALL)) {
                try {
                    // first see if the filter is a category name (i.e. "SERVER" or "SERVICE")
                    resourceCategory = ResourceCategory.valueOf(ListChildResourcesUIBean.this.childTypeFilter);
                } catch (IllegalArgumentException e) {
                    // not a category - see if it's a type id
                    try {
                        resourceTypeId = Integer.parseInt(ListChildResourcesUIBean.this.childTypeFilter);
                    } catch (NumberFormatException e1) {
                        throw new IllegalStateException("Invalid child type filter: "
                            + ListChildResourcesUIBean.this.childTypeFilter);
                    }
                }
            }

            PageList<ResourceComposite> resourceComposites = this.resourceManager
                .getResourceCompositeForParentAndTypeAndCategory(EnterpriseFacesContextUtility.getSubject(),
                    resourceCategory, resourceTypeId, EnterpriseFacesContextUtility.getResource(), pageControl);
            return resourceComposites;
        }
    }
}