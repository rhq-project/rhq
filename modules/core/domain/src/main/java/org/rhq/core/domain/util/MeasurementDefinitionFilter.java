/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import org.rhq.core.domain.measurement.MeasurementDefinition;

/**
 * A filter for {@link MeasurementDefinition}s
 * <p/>
 * Instances of this interface may be passed to the {@link
 * ResourceTypeUtility#getMeasurementDefinitions(org.rhq.core.domain.resource.ResourceType, MeasurementDefinitionFilter)}
 * method.
 *
 * @since 4.4
 *
 * @author Ian Springer
 */
public interface MeasurementDefinitionFilter {

    /**
     * Tests whether or not the specified MeasurementDefinition should be included in a MeasurementDefinition list.
     *
     * @param  metricDef  The MeasurementDefinition to be tested
     * @return  <code>true</code> if and only if <code>metricDef</code> should be included
     */
    boolean accept(MeasurementDefinition metricDef);

}
