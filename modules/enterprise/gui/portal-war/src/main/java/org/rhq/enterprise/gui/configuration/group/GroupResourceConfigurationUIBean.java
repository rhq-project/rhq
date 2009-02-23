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
package org.rhq.enterprise.gui.configuration.group;

import java.util.Map;

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.Redirect;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A POJO Seam component that handles loading and updating of Resource configurations across a compatible Group.
 *
 * @author Ian Springer
 */
@Name("GroupResourceConfigurationUIBean")
@Scope(ScopeType.CONVERSATION)
public class GroupResourceConfigurationUIBean {
    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

    @In(value = "org.jboss.seam.faces.redirect")
    private Redirect redirect;

    private ResourceGroup group;
    private Map<Integer, Configuration> resourceConfigurations;
    private ConfigurationSet configurationSet;

    @Create
    @Begin
    /**
     * Load the ConfigurationDefinition and member Configurations for the current compatible group.
     *
     * @return viewId to be redirected to
     */
    public String loadConfigurations() {
        try {
            this.group = loadGroup();
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_FATAL, e.getMessage());
            return null;
        }
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        this.resourceConfigurations = this.configurationManager
            .getResourceConfigurationMapForCompatibleGroup(this.group);
        this.configurationSet = GroupResourceConfigurationUtility.buildConfigurationSet(subject, group,
            resourceConfigurations);
        this.redirect.setParameter(ParamConstants.GROUP_ID_PARAM, this.group.getId());
        return null;
    }

    /**
     * Asynchronously persist the group member Configurations to the DB as well as push them out to the corresponding
     * Agents.
     *
     * @return viewId to be redirected to
     */
    public String updateConfigurations() {
        try {
            // TODO: See if there's some way for the config renderer to handle calling applyAggregateConfiguration(),
            //       so the managed bean doesn't have to worry about doing it.
            this.configurationSet.applyAggregateConfiguration();
            this.configurationManager.scheduleAggregateResourceConfigurationUpdate(EnterpriseFacesContextUtility
                .getSubject(), this.group.getId(), this.resourceConfigurations);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to schedule group Resource Configuration update - cause: " + e);
            this.redirect.setViewId("/rhq/group/configuration/current.xhtml");
            this.redirect.execute();
            return null;
        }
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Group Resource Configuration update scheduled.");
        Conversation.instance().endBeforeRedirect();
        this.redirect.setViewId("/rhq/group/configuration/history.xhtml");
        this.redirect.execute();
        return null;
    }

    public ConfigurationSet getConfigurationSet() {
        return this.configurationSet;
    }

    private ResourceGroup loadGroup() throws Exception {
        ResourceGroup group;
        try {
            group = EnterpriseFacesContextUtility.getResourceGroup();
        } catch (Exception e) {
            throw new Exception("No group is associated with this request ('groupId' request parameter is not set).");
        }
        if (group.getGroupCategory() != GroupCategory.COMPATIBLE) {
            throw new Exception("Group with id " + group.getId() + " is not a compatible group.");
        }
        return group;
    }

}
