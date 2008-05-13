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
package org.rhq.enterprise.gui.definition.group;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListGroupDefinitionsUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "ListGroupDefinitionsUIBean";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";

    private GroupDefinitionManagerLocal groupDefinitionManager = LookupUtil.getGroupDefinitionManager();

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupDefinitionsDataModel(PageControlView.GroupDefinitionsList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListGroupDefinitionsDataModel extends PagedListDataModel<GroupDefinition> {
        public ListGroupDefinitionsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<GroupDefinition> fetchPage(PageControl pc) {
            PageList<GroupDefinition> groupDefinitions = LookupUtil.getGroupDefinitionManager().getGroupDefinitions(pc);
            for (GroupDefinition definition : groupDefinitions) {
                // prepare for nicer display in the browser
                if (definition.getExpression() != null) {
                    definition.setExpression(definition.getExpression().replaceAll("\n", "<br/>"));
                }
            }
            return groupDefinitions;
        }
    }

    public String createNewGroupDefinition() {
        return "createNew";
    }

    public String deleteSelectedGroupDefinitions() {
        String[] selectedGroupDefinitions = getSelectedGroupDefinitions();
        Integer[] groupDefinitionIds = getIntegerArray(selectedGroupDefinitions);
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        try {
            for (Integer groupDefinitionId : groupDefinitionIds) {
                groupDefinitionManager.removeGroupDefinition(subject, groupDefinitionId);
            }

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted selected group definitions.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete group definitions.", e);
            return OUTCOME_FAILURE;
        }

        return OUTCOME_SUCCESS;
    }

    private String[] getSelectedGroupDefinitions() {
        return FacesContextUtility.getRequest().getParameterValues("selectedGroupDefinitions");
    }

    private Integer[] getIntegerArray(String[] input) {
        Integer[] output = new Integer[input.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.valueOf(input[i]);
        }

        return output;
    }
}