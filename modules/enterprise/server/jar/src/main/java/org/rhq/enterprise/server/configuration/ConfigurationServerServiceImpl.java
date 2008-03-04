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
package org.rhq.enterprise.server.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ConfigurationServerServiceImpl implements ConfigurationServerService {
    private static final Log LOG = LogFactory.getLog(ConfigurationServerServiceImpl.class);

    public void completeConfigurationUpdate(ConfigurationUpdateResponse response) {
        /*
         * Would be nice to get the resource name, but it is not in the response, so we could bring back the log message
         * that used to be here
         */
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        int configurationUpdateId = response.getConfigurationUpdateId();

        ResourceConfigurationUpdate update = configurationManager.getResourceConfigurationUpdate(subjectManager
            .getOverlord(), configurationUpdateId);

        if (update != null) {
            Resource resource = update.getResource();

            if (resource != null) {
                LOG.info("ConfigurationUpdateResponse status for " + "Resource" + "[ " + "id = " + resource.getId()
                    + ", " + "name = " + resource.getName() + "] " + "was " + response.getStatus());
            }
        }

        configurationManager.completeResourceConfigurationUpdate(response);
    }
}