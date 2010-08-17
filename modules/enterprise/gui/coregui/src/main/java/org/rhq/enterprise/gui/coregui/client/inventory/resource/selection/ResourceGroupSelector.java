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

import java.util.ArrayList;
import java.util.Collection;

import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Criterion;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupSelector extends AbstractSelector<ResourceGroup> {

    public ResourceGroupSelector() {
        super();
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(5);

        final TextItem search = new TextItem("search", "Search");

        SelectItem groupCategorySelect = new SelectItem("groupCategory", "Group Category");
        groupCategorySelect.setValueMap("Compatible", "Mixed");
        groupCategorySelect.setAllowEmptyValue(true);
        availableFilterForm.setItems(search, groupCategorySelect, new SpacerItem());

        return availableFilterForm;
    }

    protected RPCDataSource<ResourceGroup> getDataSource() {
        return new SelectedResourceGroupsDataSource();
    }

    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");
        String category = (String) availableFilterForm.getValue("groupCategory");
        ArrayList<Criterion> criteria = new ArrayList<Criterion>(2);
        if (null != search) {
            criteria.add(new Criterion("name", OperatorId.CONTAINS, search));
        }
        if (null != category) {
            criteria.add(new Criterion("category", OperatorId.EQUALS, category));
        }
        AdvancedCriteria latestCriteria = new AdvancedCriteria(OperatorId.AND, criteria.toArray(new Criterion[criteria
            .size()]));

        return latestCriteria;
    }

    public class SelectedResourceGroupsDataSource extends ResourceGroupsDataSource {

        @Override
        public ListGridRecord[] buildRecords(Collection<ResourceGroup> resourceGroups) {
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
