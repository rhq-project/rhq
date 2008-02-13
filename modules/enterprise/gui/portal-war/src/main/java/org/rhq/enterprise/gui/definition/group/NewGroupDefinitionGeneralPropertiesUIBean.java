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
import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.group.definition.exception.GroupDefinitionException;
import org.rhq.enterprise.server.util.LookupUtil;

public class NewGroupDefinitionGeneralPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "NewGroupDefinitionGeneralPropertiesUIBean";

    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_FAILURE = "failure";
    private static final String OUTCOME_CANCEL = "cancel";

    private String name;
    private String description;
    private boolean recursive;

    private int id;

    private GroupDefinitionManagerLocal groupDefinitionManager = LookupUtil.getGroupDefinitionManager();

    public NewGroupDefinitionGeneralPropertiesUIBean() {
    }

    public String begin() {
        return OUTCOME_SUCCESS;
    }

    public String create() {
        try {
            GroupDefinition groupDefinition = new GroupDefinition(this.name);

            groupDefinition.setDescription(this.description);
            groupDefinition.setRecursive(this.recursive);

            GroupDefinition newGroupdefinition = this.groupDefinitionManager.createGroupDefinition(groupDefinition);
            id = newGroupdefinition.getId();
        } catch (GroupDefinitionException gde) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Problem creating group definition: "
                + gde.getMessage());
            return OUTCOME_FAILURE;
        }

        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "General properties updated.");
        return OUTCOME_SUCCESS;
    }

    public String cancel() {
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "General properties not updated.");
        return OUTCOME_CANCEL;
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

    public int getId() {
        return this.id;
    }
}