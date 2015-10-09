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
package org.rhq.coregui.client.bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.bundle.list.BundlesDataSource;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.components.selector.AbstractSelector;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 */
public class BundleSelector extends AbstractSelector<Bundle, BundleCriteria> {

    private boolean canAssign;
    private boolean canUnassign;

    public BundleSelector() {
        super();
    }

    public BundleSelector(ListGridRecord[] initiallyAssigned, boolean canAssign, boolean canUnassign) {
        super(!(canAssign || canUnassign));

        this.canAssign = canAssign;
        this.canUnassign = canUnassign;

        setAssigned(initiallyAssigned);
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(4);
        final TextItem search = new TextItem("search", MSG.common_title_search());

        final SelectItem bundleTypeSelect = new SortedSelectItem("bundleType", MSG.view_bundle_bundleType());
        GWTServiceLookup.getBundleService().getAllBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.dataSource_bundle_loadFailed(), caught);
            }

            public void onSuccess(ArrayList<BundleType> result) {
                LinkedHashMap<String, String> values = new LinkedHashMap<String, String>(result.size());
                for (BundleType type : result) {
                    values.put(String.valueOf(type.getId()), type.getName());
                }
                bundleTypeSelect.setValueMap(values);
            }
        });
        bundleTypeSelect.setAllowEmptyValue(true);
        availableFilterForm.setItems(search, bundleTypeSelect);

        return availableFilterForm;
    }

    protected RPCDataSource<Bundle, BundleCriteria> getDataSource() {
        return new SelectedBundlesDataSource();
    }

    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");
        String bundleType = availableFilterForm.getValueAsString("bundleType");

        Criteria latestCriteria = new Criteria();
        latestCriteria.addCriteria("search", search);
        latestCriteria.addCriteria("bundleType", bundleType);

        return latestCriteria;
    }

    @Override
    protected String getItemTitle() {
        return MSG.common_title_bundles();
    }

    public class SelectedBundlesDataSource extends BundlesDataSource {
        @Override
        protected BundleCriteria getFetchCriteria(final DSRequest request) {
            BundleCriteria result = super.getFetchCriteria(request);
            if (null != result) {
                result.setStrict(false);
            }
            return result;
        }
    }

    // override to make sure the user doesn't unassign bundles from the group if he has no perms to do so
    @Override
    public void removeSelectedRows() {
        if (canUnassign || null == this.initialSelection) {
            super.removeSelectedRows();
            return;
        }

        // only allow if removing rows not in the initial selection
        String selectorKey = getSelectorKey();
        for (ListGridRecord r : this.initialSelection) {
            String initialKey = r.getAttribute(selectorKey);
            boolean found = false;
            for (ListGridRecord selectedAssignedRecord : this.assignedGrid.getSelectedRecords()) {
                if (initialKey.equals(selectedAssignedRecord.getAttribute(selectorKey))) {
                    found = true;
                    break;
                }
            }
            if (found) {
                SC.warn(MSG.view_bundleGroup_unassignFailPerm());
                return;
            }
        }

        super.removeSelectedRows();
    }

    // override to make sure the user doesn't assign bundles to the group if he has no perms to do so
    @Override
    public void addSelectedRows() {
        if (canAssign) {
            super.addSelectedRows();
            return;
        }

        // only allow if assigning rows in the initial selection
        String selectorKey = getSelectorKey();
        for (ListGridRecord r : this.availableGrid.getSelectedRecords()) {
            String selectedAvailableKey = r.getAttribute(selectorKey);
            boolean found = false;
            if (null != this.initialSelection) {
                for (ListGridRecord initialRecord : this.initialSelection) {
                    if (selectedAvailableKey.equals(initialRecord.getAttribute(selectorKey))) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                SC.warn(MSG.view_bundleGroup_assignFailPerm());
                return;
            }
        }

        super.addSelectedRows();
    }

}
