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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionException;
import org.rhq.enterprise.server.resource.group.definition.framework.InvalidExpressionException;
import org.rhq.enterprise.server.util.LookupUtil;

public class EditGroupDefinitionGeneralPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "EditGroupDefinitionGeneralPropertiesUIBean";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";
    private static final String TEMPORARY_EXPRESSION_ATTRIBUTE_NAME = "temporaryGroupDefExpr";

    private String name;
    private String description;
    private boolean recursive;
    private String expression;

    private GroupDefinitionManagerLocal groupDefinitionManager = LookupUtil.getGroupDefinitionManager();

    public EditGroupDefinitionGeneralPropertiesUIBean() {
        GroupDefinition groupDefinition = null;
        try {
            groupDefinition = GroupDefinitionUIBean.lookupGroupDefinition();
        } catch (GroupDefinitionException gde) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem looking up group definition: "
                + gde.getMessage());
            return;
        }

        this.name = groupDefinition.getName();
        this.description = groupDefinition.getDescription();
        this.recursive = groupDefinition.isRecursive();

        String previousExpression = (String) FacesContextUtility.getRequest().getSession().getAttribute(
            TEMPORARY_EXPRESSION_ATTRIBUTE_NAME);
        FacesContextUtility.getRequest().getSession().removeAttribute(TEMPORARY_EXPRESSION_ATTRIBUTE_NAME);
        if (previousExpression == null) {
            this.expression = groupDefinition.getExpression();
        } else {
            this.expression = previousExpression;
        }

    }

    public String begin() {
        return OUTCOME_SUCCESS;
    }

    public String update() {
        try {
            GroupDefinition groupDefinition = GroupDefinitionUIBean.lookupGroupDefinition();

            groupDefinition.setName(this.name);
            groupDefinition.setDescription(this.description);
            groupDefinition.setRecursive(this.recursive);

            /*
             * cleanse expression of system-dependent new line chars
             *
             * unified everything as a single '\n' character by replacing singular instances of '\f' and '\r' with '\n',
             * and then replacing all adjacent '\n' characters with a single character of the same
             */
            groupDefinition.setExpression(expression.replaceAll("\\r", "\n").replaceAll("\\f", "\n").replaceAll("\\n+",
                "\n"));

            FacesContextUtility.getRequest().getSession().setAttribute(TEMPORARY_EXPRESSION_ATTRIBUTE_NAME, expression);
            this.groupDefinitionManager.updateGroupDefinition(EnterpriseFacesContextUtility.getSubject(),
                groupDefinition);

        } catch (GroupDefinitionException gde) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem updating group definition: "
                + gde.getMessage());
            return OUTCOME_FAILURE;
        } catch (InvalidExpressionException iee) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Syntax error in one of your group definitions: " + iee.getMessage());
            return OUTCOME_FAILURE;
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error updating group definition: "
                + e.getMessage());
            return OUTCOME_FAILURE;
        }

        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "General properties updated.");
        return OUTCOME_SUCCESS;
    }

    public String reset() {
        return OUTCOME_FAILURE;
    }

    public String createGroups() {
        try {
            GroupDefinition groupDefinition = GroupDefinitionUIBean.lookupGroupDefinition();
            Subject subject = EnterpriseFacesContextUtility.getSubject();

            this.groupDefinitionManager.calculateGroupMembership(subject, groupDefinition.getId());
        } catch (GroupDefinitionException gde) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem updating group definition: "
                + gde.getMessage());
            return OUTCOME_FAILURE;
        } catch (InvalidExpressionException iee) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Syntax error in one of your group definition expressions: " + iee.getMessage());
            return OUTCOME_FAILURE;
        } catch (ResourceGroupUpdateException rgue) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There was a problem adding one or more resource groups: " + rgue.getMessage());
            return OUTCOME_FAILURE;
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "There was a problem calculating the results: "
                + e.getMessage());
            return OUTCOME_FAILURE;
        }

        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Group Definition's Resource Groups Calculated.");
        return OUTCOME_SUCCESS;
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "General properties not updated.");
        return OUTCOME_SUCCESS;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }
}