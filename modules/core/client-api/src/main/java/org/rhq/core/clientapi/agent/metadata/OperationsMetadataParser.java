 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.plugin.OperationDescriptor;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.util.StringUtils;

/**
 * @author Greg Hinkle
 */
public class OperationsMetadataParser {
    public static OperationDefinition parseOperationDescriptor(OperationDescriptor operationDescriptor)
        throws InvalidPluginDescriptorException {
        String name = operationDescriptor.getName();
        OperationDefinition operationDefinition = new OperationDefinition(operationDescriptor.getName(), // TODO display name
            "", ///operationDescriptor.getVersion().getMatch(), // TODO build the version embedded object
            operationDescriptor.getDescription());

        operationDefinition.setParametersConfigurationDefinition(ConfigurationMetadataParser.parse(operationDescriptor
            .getName(), operationDescriptor.getParameters()));

        operationDefinition.setResultsConfigurationDefinition(ConfigurationMetadataParser.parse(operationDescriptor
            .getName(), operationDescriptor.getResults()));

        operationDefinition.setTimeout(operationDescriptor.getTimeout());

        String displayName = operationDescriptor.getDisplayName();

        if ((displayName == null) || displayName.equals("")) {
            displayName = StringUtils.deCamelCase(name);
        }

        operationDefinition.setDisplayName(displayName);

        return operationDefinition;
    }
}