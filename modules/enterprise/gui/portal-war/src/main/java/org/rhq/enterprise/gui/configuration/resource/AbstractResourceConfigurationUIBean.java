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
package org.rhq.enterprise.gui.configuration.resource;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.configuration.AbstractConfigurationUIBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * This is a JSF managed bean for the Resource Configuration view and edit pages. The contents of the page are
 * dynamically generated based on the ConfigurationDefinition (metadata) and current Configuration (data) for a
 * specified Resource.
 *
 * @author Ian Springer
 */
public abstract class AbstractResourceConfigurationUIBean extends AbstractConfigurationUIBean {

    public boolean isUpdateInProgress() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        return this.configurationManager.isResourceConfigurationUpdateInProgress(subject, resourceId);
    }

    public String getNullConfigurationDefinitionMessage() {
        return "This resource does not expose a configuration.";
    }

    public String getNullConfigurationMessage() {
        return "This resource's configuration has not yet been initialized.";
    }
}