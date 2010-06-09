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
package org.rhq.enterprise.gui.coregui.client.bundle;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.bundle.list.BundlesDataSource;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 */
public class BundleSelector extends AbstractSelector<Bundle> {

    private BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();

    public BundleSelector() {
        super();
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(4);
        final TextItem search = new TextItem("search", "Search");

        final SelectItem bundleTypeSelect = new SelectItem("bundleType", "Bundle Type");
        bundleService.getAllBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to load Bundle data", caught);
            }

            public void onSuccess(ArrayList<BundleType> result) {
                String[] values = new String[result.size()];
                for (int i = 0, size = result.size(); (i < size); ++i) {
                    values[i] = result.get(i).getName();
                }
                bundleTypeSelect.setValueMap(values);
            }
        });
        bundleTypeSelect.setAllowEmptyValue(true);
        availableFilterForm.setItems(search, bundleTypeSelect);

        return availableFilterForm;
    }

    protected RPCDataSource<Bundle> getDataSource() {
        return new SelectedBundleDataSource();
    }

    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        Criteria latestCriteria = new Criteria();
        Object search = availableFilterForm.getValue("search");
        Object bundleType = availableFilterForm.getValue("bundleType");
        latestCriteria.setAttribute("name", search);
        latestCriteria.setAttribute("bundleType", bundleType);

        return latestCriteria;
    }

    private class SelectedBundleDataSource extends BundlesDataSource {

        @Override
        public ListGridRecord[] buildRecords(Collection<Bundle> bundles) {
            ListGridRecord[] records = super.buildRecords(bundles);
            for (ListGridRecord record : records) {
                if (selection.contains(record.getAttributeAsInt("id"))) {
                    record.setEnabled(false);
                }
            }
            return records;
        }
    }
}
