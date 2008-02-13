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
package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ContentDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ContentDescriptorCategory;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.content.PackageCategory;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.util.StringUtils;

/**
 * Parser responsible for translating the content section of the rhq-plugin.xml descriptor into domain objects.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public class ContentMetadataParser {
    // Public  --------------------------------------------

    /**
     * Parses the contents of the content descriptor and populates an instance of the domain model representation.
     *
     * @param  descriptor non-null content descriptor.
     *
     * @return domain model object populated with the descriptor's values.
     *
     * @throws InvalidPluginDescriptorException if the descriptor contains data that does not pass the usage
     *                                          specification
     */
    public static PackageType parseContentDescriptor(ContentDescriptor descriptor)
        throws InvalidPluginDescriptorException {
        PackageCategory category = translateCategory(descriptor.getCategory());

        PackageType type = new PackageType();
        type.setName(descriptor.getName());
        type.setDescription(descriptor.getDescription());
        type.setCategory(category);

        String displayName = descriptor.getDisplayName();

        if ((displayName == null) || displayName.equals("")) {
            displayName = StringUtils.deCamelCase(descriptor.getName());
        }

        type.setDisplayName(displayName);

        type.setDiscoveryInterval(descriptor.getDiscoveryInterval());

        type.setCreationData(descriptor.isIsCreationType());

        ConfigurationDescriptor configurationDescriptor = descriptor.getConfiguration();
        ConfigurationDefinition configurationDefinition = ConfigurationMetadataParser.parse(descriptor.getName(),
            configurationDescriptor);
        type.setDeploymentConfigurationDefinition(configurationDefinition);

        return type;
    }

    // Private  --------------------------------------------

    /**
     * Translates the descriptor's category enumerated value into the domain model.
     *
     * @param  descriptorCategory rhq-plugin.xml descriptor specified category.
     *
     * @return domain model representation of the artifact category.
     *
     * @throws IllegalArgumentException if the descriptorCategory does not map to a domain model category.
     */
    private static PackageCategory translateCategory(ContentDescriptorCategory descriptorCategory) {
        PackageCategory category = null;

        switch (descriptorCategory) {
        case CONFIGURATION: {
            category = PackageCategory.CONFIGURATION;
            break;
        }

        case DEPLOYABLE: {
            category = PackageCategory.DEPLOYABLE;
            break;
        }

        case EXECUTABLE_BINARY: {
            category = PackageCategory.EXECUTABLE_BINARY;
            break;
        }

        case EXECUTABLE_SCRIPT: {
            category = PackageCategory.EXECUTABLE_SCRIPT;
            break;
        }
        }

        if (category == null) {
            throw new IllegalArgumentException("Descriptor category " + descriptorCategory
                + " does not translate to domain model");
        }

        return category;
    }
}