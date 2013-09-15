/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.bundle.group;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.criteria.BundleGroupCriteria;
import org.rhq.enterprise.gui.coregui.client.components.selector.AbstractSelector;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 */
public class BundleGroupSelector extends AbstractSelector<BundleGroup, BundleGroupCriteria> {

    private Integer[] idsFilter;

    public BundleGroupSelector() {
        this(null, null, false);
    }

    public BundleGroupSelector(boolean isReadonly) {
        this(null, null, isReadonly);
    }

    /**
     * @param idsFilter optionally narrow the results to some predefined set, typically when narrowing to
     *                  bundle groups associated with roles having specific permissions. ignored if null.
     * @param initiallyAssigned
     * @param isReadOnly
     */
    public BundleGroupSelector(Integer[] idsFilter, ListGridRecord[] initiallyAssigned, boolean isReadOnly) {
        super(isReadOnly);
        setAssigned(initiallyAssigned);

        this.idsFilter = idsFilter;
    }

    protected DynamicForm getAvailableFilterForm() {
        DynamicForm availableFilterForm = new DynamicForm();
        availableFilterForm.setNumCols(4);
        final TextItem search = new TextItem("search", MSG.common_title_search());

        availableFilterForm.setItems(search);

        return availableFilterForm;
    }

    protected RPCDataSource<BundleGroup, BundleGroupCriteria> getDataSource() {
        return new SelectedBundleGroupsDataSource();
    }

    protected Criteria getLatestCriteria(DynamicForm availableFilterForm) {
        String search = (String) availableFilterForm.getValue("search");

        Criteria latestCriteria = new Criteria();
        latestCriteria.addCriteria("search", search);

        return latestCriteria;
    }

    @Override
    protected String getItemTitle() {
        return MSG.common_title_bundleGroups();
    }

    public class SelectedBundleGroupsDataSource extends BundleGroupsDataSource {
        @Override
        protected BundleGroupCriteria getFetchCriteria(final DSRequest request) {
            BundleGroupCriteria result = super.getFetchCriteria(request);
            if (null != result) {
                result.setStrict(false);
            }
            if (null != idsFilter) {
                result.addFilterIds(idsFilter);
            }
            return result;
        }
    }

    public boolean hasInitialSelection() {
        return null != initialSelection && initialSelection.length > 0;
    }
}
