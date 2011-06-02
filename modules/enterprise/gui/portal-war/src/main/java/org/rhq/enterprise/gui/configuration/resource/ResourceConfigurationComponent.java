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
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

//@Name("resourceConfiguration")
//@AutoCreate
public class ResourceConfigurationComponent {

//    private Configuration originalResource


//    @RequestParameter("id")
    private int resourceId;

    //    @In
    private ConfigurationDefinition resourceConfigurationDefinition;

//    @Create
    public void loadResourceConfiguration() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        AbstractResourceConfigurationUpdate configurationUpdate =
                configurationManager.getLatestResourceConfigurationUpdate(subject, resourceId);
        Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
    }



//    @Unwrap
    public Configuration lookupResourceConfiguration() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();

        AbstractResourceConfigurationUpdate configurationUpdate =
                configurationManager.getLatestResourceConfigurationUpdate(subject, resourceId);
        Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;

        return configuration;
    }

}
