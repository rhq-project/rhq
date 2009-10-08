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
package org.rhq.plugins.perftest.configuration;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * @author Jason Dobies
 */
public interface ConfigurationFactory {
    /**
     * Generates a configuration that adheres to the specified definition. The algorithm for generating the values
     * depends on the underlying implementation
     *
     * @param  definition defines the values that will be created in this call
     *
     * @return configuration instance, will not be <code>null</code>
     */
    Configuration generateConfiguration(ConfigurationDefinition definition);
}