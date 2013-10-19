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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.resource.Resource;

/**
 * Public API for measurement baselines.
 */
@Remote
public interface MeasurementBaselineManagerRemote {

    /**
     * Return a list of {@link MeasurementBaseline} objects for the {@link Resource} represented by the given id.
     *
     * @param subject    the user request to view the baseline history for the given resource
     * @param resourceId the id of the resource whose baselines are to be returned
     *
     * @return a list of baselines for all measurements scheduled on the given resource
     */
    List<MeasurementBaseline> findBaselinesForResource(Subject subject, int resourceId);
}
