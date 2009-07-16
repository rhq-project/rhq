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

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.FetchException;

/**
 * A manager for {@link MeasurementDefinition}s.
 */
@Local
public interface MeasurementDefinitionManagerLocal {

    /**
     * Remove the given definition with its attached schedules and MeasurementData
     *
     * @param def MeasurementDefinition to remove
     */
    void removeMeasurementDefinition(MeasurementDefinition def);

    /**
     * Returns a list of MeasurementDefinitions based on ResourceType
     *
     * @param  user           user that is calling this method
     * @param  resourceTypeId id Of the resourceType to use as criteria
     * @param  dataType       dataType
     * @param  displayType    displayType
     *
     * @return List<MeasurementDefinition> list of definitions found
     */
    List<MeasurementDefinition> findMeasurementDefinitionsByResourceType(Subject user, int resourceTypeId,
        DataType dataType, DisplayType displayType);

    /**
     * Returns a list of MeasurmentDefintions for each of the ids passed in the array
     *
     * @param  subject                  user that is calling this method
     * @param  measurementDefinitionIds Array of ints for the ids to search by
     *
     * @return List<MeasurementDefinition> list of definitions found
     */
    List<MeasurementDefinition> findMeasurementDefinitionsByIds(Subject subject, Integer[] measurementDefinitionIds);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * Returns a MeasurementDefinition by its id or null.
     *
     * @param  subject      user that is calling this method
     * @param  definitionId id of the desired {@link MeasurementDefinition} to fetch
     *
     * @return the MeasurementDefinition or null if not found
     */
    MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId);

    PageList<MeasurementDefinition> findMeasurementDefinitions(Subject sessionSubject, MeasurementDefinition criteria,
        PageControl pc) throws FetchException;

    PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(Subject subject,
        MeasurementDefinitionCriteria criteria);
}