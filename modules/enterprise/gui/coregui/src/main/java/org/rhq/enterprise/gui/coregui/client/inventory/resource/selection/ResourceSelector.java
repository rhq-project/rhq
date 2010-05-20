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

import java.util.Collection;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.IPickTreeItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypePluginTreeDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceSelector extends AbstractSelector<Resource> {

    private Integer requireTypeId;

    public ResourceSelector() {
        super();
    }

    public Integer getRequireTypeId() {
        return requireTypeId;
    }

    public void setRequireTypeId(Integer requireTypeId) {
        this.requireTypeId = requireTypeId;
        markForRedraw();
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(6);
        final TextItem search = new TextItem("search", "Search");

        IPickTreeItem typeSelectItem = new IPickTreeItem("type", "Type");
        typeSelectItem.setDataSource(new ResourceTypePluginTreeDataSource());
        typeSelectItem.setValueField("id");
        typeSelectItem.setCanSelectParentItems(true);
        typeSelectItem.setLoadDataOnDemand(false);
        typeSelectItem.setEmptyMenuMessage("Loading...");
        typeSelectItem.setShowIcons(true);

        if (requireTypeId != null) {
            typeSelectItem.setValue(requireTypeId);
            typeSelectItem.setDisabled(true);
            availableFilterForm.setItems(search, typeSelectItem);
        } else {
            SelectItem categorySelect = new SelectItem("category", "Category");
            categorySelect.setValueMap("Platform", "Server", "Service");
            categorySelect.setAllowEmptyValue(true);

            availableFilterForm.setItems(search, typeSelectItem, categorySelect);
        }

        return availableFilterForm;
    }

    protected RPCDataSource<Resource> getDataSource() {
        return new SelectedResourceDataSource();
    }

    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        Criteria latestCriteria = new Criteria();
        latestCriteria.setAttribute("name", availableFilterForm.getValue("search"));

        // If its a number its a typeId, otherwise a plugin name
        try {
            Integer.parseInt((String) availableFilterForm.getValue("type"));
            latestCriteria.setAttribute("type", availableFilterForm.getValue("type"));
        } catch (NumberFormatException nfe) {
            latestCriteria.setAttribute("plugin", availableFilterForm.getValue("type"));
        }
        latestCriteria.setAttribute("category", availableFilterForm.getValue("category"));

        return latestCriteria;
    }

    private class SelectedResourceDataSource extends ResourceDatasource {

        @Override
        public ListGridRecord[] buildRecords(Collection<Resource> resources) {
            ListGridRecord[] records = super.buildRecords(resources);
            for (ListGridRecord record : records) {
                if (selection.contains(record.getAttributeAsInt("id"))) {
                    record.setEnabled(false);
                }
            }
            return records;
        }
    }
}
