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
package org.rhq.coregui.client.bundle.deploy.selection;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.inventory.groups.ResourceGroupsDataSource;

public class SingleCompatibleResourceGroupSelector extends ComboBoxItem {

    private String bundleType;

    public SingleCompatibleResourceGroupSelector(String name, String title, String bundleType) {
        super(name, title);

        this.bundleType = bundleType;

        ListGridField nameField = new ListGridField("name");
        ListGridField descriptionField = new ListGridField("description");

        setOptionDataSource(new CompatibleResourceGroupsDataSource());

        setWidth(240);
        setTitle(CoreGUI.getMessages().common_title_resource_group());

        setValueField("id");
        setDisplayField("name");
        setPickListWidth(450);
        setPickListFields(nameField, descriptionField);
        setTextMatchStyle(TextMatchStyle.SUBSTRING);
    }

    protected class CompatibleResourceGroupsDataSource extends ResourceGroupsDataSource {
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

            //further we only show groups that can be targeted by the bundle type
            result.addFilterAcceptableTargetForBundleType(bundleType);
            //workaround
            result.setCaseSensitive(true);
            return result;
        }
    }
}
