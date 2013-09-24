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
package org.rhq.enterprise.gui.coregui.client.bundle.deploy.selection;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.ResourceGroupsDataSource;

@Deprecated
// could use SingleResourceGroupSelectorItem seeded with addFilterBundleTargetableOnly(true);
public class SingleCompatibleResGroupSelector extends ComboBoxItem {

    public SingleCompatibleResGroupSelector(String name, String title) {
        super(name, title);

        ListGridField nameField = new ListGridField("name");
        ListGridField descriptionField = new ListGridField("description");

        setOptionDataSource(new CompatibleResourceGroupsDS());

        setWidth(240);
        setTitle(CoreGUI.getMessages().common_title_resource_group());

        setValueField("id");
        setDisplayField("name");
        setPickListWidth(450);
        setPickListFields(nameField, descriptionField);
        setTextMatchStyle(TextMatchStyle.SUBSTRING);
    }

    protected class CompatibleResourceGroupsDS extends ResourceGroupsDataSource {

        @Override
        protected ResourceGroupCriteria getFetchCriteria(final DSRequest request) {
            // We don't want to use the superclass's getFetchCriteria because our selected value
            // is either a Integer (when a real group has been selected) or a String (when a partial search string is selected).
            // So, here we create our own criteria. See BZ 802528.
            ResourceGroupCriteria result = new ResourceGroupCriteria();
            String filterString = getFilter(request, "id", String.class);
            if (filterString != null) {
                try {
                    Integer id = new Integer(filterString);
                    result.addFilterId(id);
                } catch (Exception e) {
                    result.addFilterName(filterString);
                }
            }

            // we only want to show those groups that can have bundles deployed to them
            result.addFilterBundleTargetableOnly(true);
            return result;
        }
    }
}
