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
package org.rhq.enterprise.server.measurement;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.util.PageList;

/**
 * Public API for measurement definitions.
 */
@Remote
public interface MeasurementDefinitionManagerRemote {

    /**
     * Returns a MeasurementDefinition by its id or null.
     *
     * @param  subject      user that is calling this method
     * @param  definitionId id of the desired {@link MeasurementDefinition} to fetch
     *
     * @return the MeasurementDefinition or null if not found
     */
    MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(Subject subject,
        MeasurementDefinitionCriteria criteria);

}
