/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.inventory.group;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.model.DataModel;

import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListResourceGroupMembersUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListResourceGroupMembersUIBean";

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private boolean showRecursiveMembers = false;

    public ListResourceGroupMembersUIBean() {
    }

    public String addNewResources() {
        return "addResourcesToGroup";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourceGroupMembersDataModel(PageControlView.ResourceGroupMemberList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    public void setShowRecursiveMembers(boolean showRecursiveMembers) {
        this.showRecursiveMembers = showRecursiveMembers;
        dataModel = null; // this will force getDataModel to query next time it's called
    }

    public boolean getShowRecursiveMembers() {
        return this.showRecursiveMembers;
    }

    public int getNumberOfColumns() {
        return 4;
    }

    public List<Tuple<String, Integer>> getResourceTypeCounts() {
        Map<String, Integer> typeMap = resourceTypeManager.getResourceTypeCountsByGroup(getSubject(),
            getResourceGroup(), getShowRecursiveMembers());

        String[] typeNames = typeMap.keySet().toArray(new String[typeMap.keySet().size()]);
        Arrays.sort(typeNames, new Comparator<String>() {
            // case-insensitive sort
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });

        typeNames = evenlyShuffle(typeNames, getNumberOfColumns());

        List<Tuple<String, Integer>> tupleResults = new ArrayList<Tuple<String, Integer>>(typeMap.size());
        for (String typeName : typeNames) {
            tupleResults.add(new Tuple<String, Integer>(typeName, typeMap.get(typeName)));
        }

        return tupleResults;
    }

    private String[] evenlyShuffle(String[] input, int columns) {
        int pieces = (input.length / columns) + (input.length % columns);

        List<List<String>> outputPieces = new ArrayList<List<String>>();
        for (int i = 0; i < pieces; i++) {
            outputPieces.add(new ArrayList<String>());
        }
        for (int i = 0; i < input.length; i++) {
            outputPieces.get(i % pieces).add(input[i]);
        }

        List<String> totalOutput = new ArrayList<String>();
        for (int i = 0; i < pieces; i++) {
            totalOutput.addAll(outputPieces.get(i));
        }

        String[] results = totalOutput.toArray(new String[totalOutput.size()]);
        return results;
    }

    protected class ListResourceGroupMembersDataModel extends PagedListDataModel<ResourceWithAvailability> {
        private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        public ListResourceGroupMembersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceWithAvailability> fetchPage(PageControl pageControl) {
            PageList<ResourceWithAvailability> results = null;
            if (getShowRecursiveMembers()) {
                results = resourceManager.getImplicitResourceWithAvailabilityByResourceGroup(getSubject(),
                    getResourceGroup(), pageControl);
            } else {
                results = resourceManager.getExplicitResourceWithAvailabilityByResourceGroup(
                        LookupUtil.getSubjectManager().getOverlord(),
                        getResourceGroup(), pageControl);
            }

            return results;
        }
    }
}