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

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionException;
import org.rhq.enterprise.server.util.LookupUtil;

public class GroupDefinitionUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "GroupDefinitionUIBean";

    private GroupDefinition groupDefinition;

    public GroupDefinitionUIBean() {
        try {
            this.groupDefinition = lookupGroupDefinition();
        } catch (GroupDefinitionException gde) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem looking up group definition: "
                + gde.getMessage());
        }
    }

    public int getGroupDefinitionId() {
        return this.groupDefinition.getId();
    }

    @NotNull
    public String getName() {
        return this.groupDefinition.getName();
    }

    public String getDescription() {
        return this.groupDefinition.getDescription();
    }

    public long getCreatedTime() {
        return this.groupDefinition.getCreatedTime();
    }

    public long getModifiedTime() {
        return this.groupDefinition.getModifiedTime();
    }

    public Long getLastCalculationTime() {
        return this.groupDefinition.getLastCalculationTime();
    }

    public Long getRecalculationInterval() {
        return this.groupDefinition.getRecalculationInterval();
    }

    public Long getNextCalculationTime() {
        return this.groupDefinition.getNextCalculationTime();
    }

    public Boolean getRecursive() {
        return this.groupDefinition.isRecursive();
    }

    public String getExpression() {
        String expression = this.groupDefinition.getExpression();

        return ((expression == null) ? "" : expression);
    }

    public static GroupDefinition lookupGroupDefinition() throws GroupDefinitionException {
        int groupDefinitionId = FacesContextUtility.getRequiredRequestParameter(
            ParamConstants.GROUP_DEFINITION_ID_PARAM, Integer.class);
        return LookupUtil.getGroupDefinitionManager().getById(groupDefinitionId);
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListGroupDefinitionMembersDataModel(PageControlView.GroupDefinitionMembers,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListGroupDefinitionMembersDataModel extends PagedListDataModel<ResourceGroupComposite> {
        public ListGroupDefinitionMembersDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<ResourceGroupComposite> fetchPage(PageControl pc) {
            int groupDefinitionId = groupDefinition.getId();

            GroupDefinitionManagerLocal groupDefinitionManager = LookupUtil.getGroupDefinitionManager();

            return groupDefinitionManager.getManagedResourceGroups(groupDefinitionId, pc);
        }
    }
}