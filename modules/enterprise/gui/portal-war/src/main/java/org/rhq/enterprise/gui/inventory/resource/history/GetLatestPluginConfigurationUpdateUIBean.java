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
package org.rhq.enterprise.gui.inventory.resource.history;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class GetLatestPluginConfigurationUpdateUIBean {
    private AbstractResourceConfigurationUpdate latestConfigurationUpdate = null;

    public AbstractResourceConfigurationUpdate getLatestConfigurationUpdate() {
        if (latestConfigurationUpdate == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Resource resource = EnterpriseFacesContextUtility.getResource();
            ConfigurationManagerLocal manager = LookupUtil.getConfigurationManager();

            latestConfigurationUpdate = manager.getLatestPluginConfigurationUpdate(subject, resource.getId());
        }

        return latestConfigurationUpdate;
    }
}