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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

/**
 * Component class for JVM definitions on managed server / server-group level
 * @author Heiko W. Rupp
 */
public class JVMDefinitionComponent extends BaseComponent<JVMDefinitionComponent> implements CreateChildResourceFacet {

    private static final String BASE_DEFINITION = "baseDefinition";

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        // We need to filter out the baseDefinition property and use its value as user selected resource name

        Configuration configuration = report.getResourceConfiguration();
        PropertySimple baseDefinitionProp = configuration.getSimple(BASE_DEFINITION);
        if (baseDefinitionProp != null) {
            configuration.remove(BASE_DEFINITION);

            String baseDefinitionName = baseDefinitionProp.getStringValue();

            report.setUserSpecifiedResourceName(baseDefinitionName);
            report = super.createResource(report);
        } else {
            report.setStatus(CreateResourceStatus.INVALID_CONFIGURATION);
            report.setErrorMessage("No base definition given that we can use");
        }
        return report;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration configuration = super.loadResourceConfiguration();

        // We need to sneak the baseDefinition back in, otherwise the UI will complain
        String baseDefinitionName = path.substring(path.lastIndexOf('=') + 1);
        PropertySimple propertySimple = new PropertySimple(BASE_DEFINITION, baseDefinitionName);
        configuration.put(propertySimple);

        return configuration;
    }
}
