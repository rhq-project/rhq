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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupSelector extends AbstractSelector {

    public ResourceGroupSelector() {
        super();
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(4);
        final TextItem search = new TextItem("search", "Search");

        SelectItem groupCategorySelect = new SelectItem("groupCategory", "Group Category");
        groupCategorySelect.setValueMap("Compatible", "Mixed");
        groupCategorySelect.setAllowEmptyValue(true);
        availableFilterForm.setItems(search, groupCategorySelect);

        return availableFilterForm;
    }

    protected RPCDataSource<?> getDataSource() {
        return new SelectedResourceGroupsDataSource();
    }

    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        Criteria latestCriteria = new Criteria();
        Object search = availableFilterForm.getValue("search");
        Object category = availableFilterForm.getValue("groupCategory");
        latestCriteria.setAttribute("name", search);
        latestCriteria.setAttribute("groupCategory", category);

        return latestCriteria;
    }

    private class SelectedResourceGroupsDataSource extends ResourceGroupsDataSource {

        @Override
        public ListGridRecord[] buildRecords(PageList<ResourceGroup> resourceGroups) {
            ListGridRecord[] records = super.buildRecords(resourceGroups);
            for (ListGridRecord record : records) {
                if (selection.contains(record.getAttributeAsInt("id"))) {
                    record.setEnabled(false);
                }
            }
            return records;
        }
    }
}
