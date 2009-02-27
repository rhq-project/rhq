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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListResourceGroupMembersUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListResourceGroupMembersUIBean";

    private ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
    private Boolean suppressRecursiveResults;

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

    public DataModel getSmallDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourceGroupMembersDataModel(PageControlView.MiniResourceGroupMemberList,
                MANAGED_BEAN_NAME, 5);
        }

        return dataModel;
    }


    public void setSuppressRecursiveResults(boolean suppressRecursiveResults) {
        this.suppressRecursiveResults = suppressRecursiveResults;
    }

    public boolean getSuppressRecursiveResults() {
        if (suppressRecursiveResults == null) {
            suppressRecursiveResults = FacesContextUtility.getOptionalRequestParameter(
                "groupMembersForm:suppressRecursiveResults", Boolean.class);
        }
        return (this.suppressRecursiveResults == null ? false : true);
    }

    public List<Tuple<String, Integer>> getResourceTypeCounts() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

        Map<String, Integer> typeMap = resourceTypeManager.getResourceTypeCountsByGroup(subject, resourceGroup);

        String[] typeNames = typeMap.keySet().toArray(new String[0]);
        Arrays.sort(typeNames, new Comparator<String>() {
            // case-insensitive sort
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });

        typeNames = evenlyShuffle(typeNames);

        List<Tuple<String, Integer>> tupleResults = new ArrayList<Tuple<String, Integer>>(typeMap.size());
        for (String typeName : typeNames) {
            tupleResults.add(new Tuple<String, Integer>(typeName, typeMap.get(typeName)));
        }

        return tupleResults;
    }

    private String[] evenlyShuffle(String[] input) {
        String[] output = new String[input.length];
        for (int i = 0, j = (output.length + 1) / 2, k = 0; j < output.length; i++, j++, k += 2) {
            output[k] = input[i];
            output[k + 1] = input[j];
        }

        if ((output.length % 2) == 1) {
            output[output.length - 1] = input[output.length / 2];
        }

        return output;
    }

    protected class ListResourceGroupMembersDataModel extends PagedListDataModel<ResourceWithAvailability> {
        private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        int overridenPageSize;


        public ListResourceGroupMembersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public ListResourceGroupMembersDataModel(PageControlView view, String beanName, int overidePageSize) {
            super(view, beanName);
            this.overridenPageSize = overidePageSize;
        }

        @Override
        public PageList<ResourceWithAvailability> fetchPage(PageControl pageControl) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            ResourceGroup resourceGroup = EnterpriseFacesContextUtility.getResourceGroup();

            if (overridenPageSize > 0) {
                pageControl.setPageSize(overridenPageSize);
            }

            PageList<ResourceWithAvailability> results = null;
            if (getSuppressRecursiveResults()) {
                results = resourceManager.getExplicitResourceWithAvailabilityByResourceGroup(subject, resourceGroup,
                    pageControl);
            } else {
                results = resourceManager.getImplicitResourceWithAvailabilityByResourceGroup(subject, resourceGroup,
                    pageControl);
            }

            return results;
        }
    }
}