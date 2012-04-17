/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class ThreadsComponent extends BaseComponent<ResourceComponent<?>> {

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        if (report.getResourceType().getName().equals("ThreadPool")) {
            //Need to munge the report and configuration definition for ThreadPools:
            //1) Remove type from the properties and configuration.
            //2) Update path to the selected thread pool type.
            ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition();
            configDef.getPropertyDefinitions().remove("type");
            CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, connection, address);

            Configuration configuration = report.getResourceConfiguration();
            PropertySimple threadPoolType = (PropertySimple) configuration.get("type");
            configuration.remove("type");
            report.getPluginConfiguration().put(new PropertySimple("path", threadPoolType.getStringValue()));

            return delegate.createResource(report);
        } else {
            return super.createResource(report);
        }
    }

}
