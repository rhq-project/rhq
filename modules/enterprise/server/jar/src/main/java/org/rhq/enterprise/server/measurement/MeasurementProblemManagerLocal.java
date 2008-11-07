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

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.util.PageList;

/**
 * A manager for working with problems such as out-of-bounds measurements.
 */
@Local
public interface MeasurementProblemManagerLocal {
    /**
     * Returns a list of all "problem resources" where a problem resource has one or more of the following statements
     * true:
     *
     * <ul>
     *   <li>it is known to be {@link AvailabilityType#DOWN down}</li>
     *   <li>one or more of its alerts were triggered</li>
     *   <li>one or more of its measurements were out-of-bounds (compared to their baselines)</li>
     * </ul>
     *
     * If one or more of those are true for any resource, and they became true at or after the given <code>
     * oldestDate</code> time (specified in epoch milliseconds), that resource is returned in the list (assuming the
     * given user has access to view that resource).
     *
     * @param  subject    the user asking for the data
     * @param  oldestDate no problems will be returned that started before this time
     * @param  maxResources the maximum number of resources that should be returned
     * 
     * @return the problems resources (only those resources visible to the user will be returned)
     */
    PageList<ProblemResourceComposite> findProblemResources(Subject subject, long oldestDate, int maxResources);

}