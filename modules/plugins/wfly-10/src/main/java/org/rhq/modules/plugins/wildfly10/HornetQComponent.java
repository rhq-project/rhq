/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

/**
 * Component class for HornetQ related stuff
 * @author Heiko W. Rupp
 */
public class HornetQComponent extends TemplatedSubResourcesComponent {

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {

        Configuration resourceConfiguration = report.getResourceConfiguration();
        PropertyList entries = resourceConfiguration.getList("entries");
        if (entries != null && entries.getList().isEmpty()) {
            report.setErrorMessage("You need to provide at least one JNDI name");
            report.setStatus(CreateResourceStatus.FAILURE);
            return report;
        }

        report = super.createResource(report);
        return report;
    }
}
