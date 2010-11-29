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
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupSelector extends AbstractSelector<ResourceGroup> {

    public ResourceGroupSelector(String locatorId) {
        super(locatorId);
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new LocatableDynamicForm(this.getLocatorId());
        availableFilterForm.setWidth100();
        availableFilterForm.setNumCols(4);

        final TextItem search = new TextItem("search", MSG.common_title_search());

        SelectItem groupCategorySelect = new SelectItem("groupCategory", MSG.widget_resourceSelector_groupCategory());
        groupCategorySelect.setValueMap("Compatible", "Mixed"); // I don't think we can i18n these, may need these literal values - need to double check this
        groupCategorySelect.setAllowEmptyValue(true);
        availableFilterForm.setItems(search, groupCategorySelect, new SpacerItem());

        return availableFilterForm;
    }

    protected RPCDataSource<ResourceGroup> getDataSource() {
        return new SelectedResourceGroupsDataSource();
    }

    // TODO: Until http://code.google.com/p/smartgwt/issues/detail?id=490 is fixed, avoid AdvancedCriteria and always
    // use server-side fetch and simple criteria. When fixed, use the commented version below. Also see
    // AbstractSelector and ResourceGroupDataSource.
    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");
        String category = (String) availableFilterForm.getValue("groupCategory");
        Criteria criteria = new Criteria();
        if (null != search) {
            criteria.addCriteria("name", search);
        }
        if (null != category) {
            criteria.addCriteria("category", category);
        }

        return criteria;
    }

    @Override
    protected String getItemTitle() {
        return MSG.common_title_groups();
    }

    //    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
    //        String search = (String) availableFilterForm.getValue("search");
    //        String category = (String) availableFilterForm.getValue("groupCategory");
    //        ArrayList<Criterion> criteria = new ArrayList<Criterion>(2);
    //        if (null != search) {
    //            criteria.add(new Criterion("name", OperatorId.CONTAINS, search));
    //        }
    //        if (null != category) {
    //            criteria.add(new Criterion("category", OperatorId.EQUALS, category));
    //        }
    //        AdvancedCriteria latestCriteria = new AdvancedCriteria(OperatorId.AND, criteria.toArray(new Criterion[criteria
    //            .size()]));
    //
    //        return latestCriteria;
    //    }

    public class SelectedResourceGroupsDataSource extends ResourceGroupsDataSource {
        @Override
        protected ResourceGroupCriteria getFetchCriteria(final DSRequest request) {
            ResourceGroupCriteria result = super.getFetchCriteria(request);
            if (null != result) {
                result.setStrict(false);
            }
            return result;
        }
    }
}
