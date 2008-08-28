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
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private final Log log = LogFactory.getLog(EditGroupDefinitionGeneralPropertiesUIBean.class);

    public static final String MANAGED_BEAN_NAME = "EditGroupDefinitionGeneralPropertiesUIBean";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";

    private static final String TEMPORARY_EDIT_MARKER = "temporaryGroupDef-Marker";
    private static final String TEMPORARY_EXPRESSION_ATTRIBUTE = "temporaryGroupDefExpr";
    private static final String TEMPORARY_NAME_ATTRIBUTE = "temporaryGroupDefName";
    private static final String TEMPORARY_DESCRIPTION_ATTRIBUTE = "temporaryGroupDefDescription";
    private static final String TEMPORARY_RECURSIVE_ATTRIBUTE = "temporaryGroupDefRecursive";

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

        HttpSession session = FacesContextUtility.getRequest().getSession();

        String marker = (String) session.getAttribute(TEMPORARY_EDIT_MARKER);
        String previousExpression = (String) session.getAttribute(TEMPORARY_EXPRESSION_ATTRIBUTE);
        String previousName = (String) session.getAttribute(TEMPORARY_NAME_ATTRIBUTE);
        String previousDescription = (String) session.getAttribute(TEMPORARY_DESCRIPTION_ATTRIBUTE);
        String previousRecursive = (String) session.getAttribute(TEMPORARY_RECURSIVE_ATTRIBUTE);

        if (marker == null) {
            this.expression = groupDefinition.getExpression();
            this.name = groupDefinition.getName();
            this.description = groupDefinition.getDescription();
            this.recursive = groupDefinition.isRecursive();
        } else {
            this.expression = previousExpression;
            this.name = previousName;
            this.description = previousDescription;
            this.recursive = previousRecursive.equals("TRUE");
        }

        session.removeAttribute(TEMPORARY_EDIT_MARKER);
        session.removeAttribute(TEMPORARY_EXPRESSION_ATTRIBUTE);
        session.removeAttribute(TEMPORARY_NAME_ATTRIBUTE);
        session.removeAttribute(TEMPORARY_DESCRIPTION_ATTRIBUTE);
        session.removeAttribute(TEMPORARY_RECURSIVE_ATTRIBUTE);
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

            HttpSession session = FacesContextUtility.getRequest().getSession();

            session.setAttribute(TEMPORARY_EDIT_MARKER, "marker");
            session.setAttribute(TEMPORARY_EXPRESSION_ATTRIBUTE, expression);
            session.setAttribute(TEMPORARY_NAME_ATTRIBUTE, name);
            session.setAttribute(TEMPORARY_DESCRIPTION_ATTRIBUTE, description);
            session.setAttribute(TEMPORARY_RECURSIVE_ATTRIBUTE, (recursive ? "TRUE" : "FALSE"));

            this.groupDefinitionManager.updateGroupDefinition(EnterpriseFacesContextUtility.getSubject(),
                groupDefinition);

            // don't bother logging the GroupDefinitionException or InvalidExpressionException, since these are expected
        } catch (GroupDefinitionException gde) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem updating group definition: "
                + gde.getMessage());
            return OUTCOME_FAILURE;
        } catch (InvalidExpressionException iee) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Syntax error in your group definition: "
                + iee.getMessage());
            return OUTCOME_FAILURE;
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Error updating group definition: "
                + e.getMessage());
            log.error("Error updating group definition: ", e);
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
            log.error("Problem updating group definition: ", gde);
            return OUTCOME_FAILURE;
        } catch (InvalidExpressionException iee) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Syntax error in one of your group definition expressions: " + iee.getMessage());
            log.error("Syntax error in one of your group definition expressions: ", iee);
            return OUTCOME_FAILURE;
        } catch (ResourceGroupUpdateException rgue) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "There was a problem adding one or more resource groups: " + rgue.getMessage());
            log.error("There was a problem adding one or more resource groups: ", rgue);
            return OUTCOME_FAILURE;
        } catch (Exception e) {
            Throwable t = e.getCause();
            if (t instanceof ClassCastException) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN, "Probable syntax error: "
                    + "this is likely due to comparing an integer property of resource (such as id) to a non-numeric, "
                    + "or using a string function (startswith, endswith, or contains) on an integer property");
                log.error("There was a problem calculating the results: ", t);
                return OUTCOME_FAILURE;
            } else {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                    "There was a problem calculating the results: " + e.getMessage());
                log.error("There was a problem calculating the results: ", e);
                return OUTCOME_FAILURE;
            }
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