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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.selection;

import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.PLUGIN;
import static org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;

import java.util.Collection;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.IPickTreeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypePluginTreeDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class ResourceSelector extends AbstractSelector<Resource> {

    private ResourceType requireType;

    public ResourceSelector(String id) {
        super(id);
    }

    public ResourceType getRequireType() {
        return requireType;
    }

    public void setRequireType(ResourceType requireType) {
        this.requireType = requireType;
        markForRedraw();
    }

    protected DynamicForm getAvailableFilterForm() {
        if (null == availableFilterForm) {
            availableFilterForm = new LocatableDynamicForm("ResSelectAvailFilterForm");
            availableFilterForm.setNumCols(6);
            final TextItem search = new TextItem("search", "Search");

            IPickTreeItem typeSelectItem = new IPickTreeItem("type", "Type");
            typeSelectItem.setDataSource(new ResourceTypePluginTreeDataSource());
            typeSelectItem.setValueField("id");
            typeSelectItem.setCanSelectParentItems(true);
            typeSelectItem.setLoadDataOnDemand(false);
            typeSelectItem.setEmptyMenuMessage("Loading...");
            typeSelectItem.setShowIcons(true);

            if (requireType != null) {
                // TODO: Currently ignore the typeSelectItem widget because we already know the type.
                // Alternatively, we could display it disabled but we'd want the type name to be displayed as the
                // value. To get this to display the type name I think we need to pre-fetch the type tree here. We could
                // potentially optimize typeSelectItem.setValue(requireType.getId()) to build a tree that includes only
                // this single type.
                //typeSelectItem.setValue(requireType.getId());
                //typeSelectItem.setDisabled(true);
                availableFilterForm.setItems(search);
            } else {
                SelectItem categorySelect = new SelectItem("category", "Category");
                categorySelect.setValueMap("Platform", "Server", "Service");
                categorySelect.setAllowEmptyValue(true);

                availableFilterForm.setItems(search, typeSelectItem, categorySelect);
            }
        }

        return availableFilterForm;
    }

    protected RPCDataSource<Resource> getDataSource() {
        if (null == datasource) {
            datasource = new SelectedResourceDataSource();
        }

        return datasource;
    }

    // TODO: Until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed, avoid AdvancedCriteria and always
    // use server-side fetch and simple criteria. When fixed, use the commented version below. Also see
    // ResourceDataSource.
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");
        String type = availableFilterForm.getValueAsString("type");
        String category = (String) availableFilterForm.getValue("category");
        Criteria criteria = new Criteria();
        if (null != search) {
            criteria.addCriteria(NAME.propertyName(), search);
        }
        if (null != type) {
            // If type is a number its a typeId, otherwise a plugin name
            try {
                Integer.parseInt(type);
                criteria.addCriteria(TYPE.propertyName(), type);
            } catch (NumberFormatException nfe) {
                criteria.addCriteria(PLUGIN.propertyName(), type);
            }
        }
        if (null != category) {
            criteria.addCriteria(CATEGORY.propertyName(), category);
        }

        return criteria;
    }

    //  protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
    //  String search = (String) availableFilterForm.getValue("search");
    //  String type = availableFilterForm.getValueAsString("type");
    //  String category = (String) availableFilterForm.getValue("category");
    //  ArrayList<Criterion> criteria = new ArrayList<Criterion>(3);
    //  if (null != search) {
    //      criteria.add(new Criterion(NAME.propertyName(), OperatorId.CONTAINS, search));
    //  }
    //  if (null != type) {
    //      // If type is a number its a typeId, otherwise a plugin name
    //      try {
    //          Integer.parseInt(type);
    //          criteria.add(new Criterion(TYPE.propertyName(), OperatorId.EQUALS, type));
    //      } catch (NumberFormatException nfe) {
    //          criteria.add(new Criterion(PLUGIN.propertyName(), OperatorId.EQUALS, type));
    //      }
    //  }
    //  if (null != category) {
    //      criteria.add(new Criterion(CATEGORY.propertyName(), OperatorId.EQUALS, category));
    //  }
    //  AdvancedCriteria latestCriteria = new AdvancedCriteria(OperatorId.AND, criteria.toArray(new Criterion[criteria
    //      .size()]));
    //
    //  return latestCriteria;
    //}

    private class SelectedResourceDataSource extends ResourceDatasource {

        @Override
        public ListGridRecord[] buildRecords(Collection<Resource> resources) {
            ListGridRecord[] records = super.buildRecords(resources);
            for (ListGridRecord record : records) {
                if (getSelection().contains(record.getAttributeAsInt("id"))) {
                    record.setEnabled(false);
                }
            }
            return records;
        }

        @Override
        protected ResourceCriteria getFetchCriteria(final DSRequest request) {
            ResourceCriteria result = super.getFetchCriteria(request);

            // additional filters
            if (null != requireType) {
                result.addFilterResourceTypeId(requireType.getId());
            }

            // additional return data
            result.fetchResourceType(true);

            return result;
        }

    }
}
