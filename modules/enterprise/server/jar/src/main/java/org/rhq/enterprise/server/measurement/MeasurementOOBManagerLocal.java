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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.measurement.MeasurementOOB;

/**
 * Interface for the OOB Manager
 * @see org.rhq.enterprise.server.measurement.MeasurementOOBManagerBean
 *
 * @author Heiko W. Rupp
 */
@Local
public interface MeasurementOOBManagerLocal {

    /**
     * Compute oobs from the values in the 1h measurement table that just got added.
     * For the total result, this is an incremental computation. The idea is that
     * it gets run *directly* after the 1h compression (and the baseline recalculation too)
     */
    void computeOOBsFromLastHour(Subject subject, long begin);

    /**
     * Remove old OOB entries from the database
     * @param subject
     * @param end oldest value to keep
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    void removeOldOOBs(Subject subject, long end);


    List<MeasurementOOBComposite> getSchedulesWithOOBs(Subject subject, long begin, long end);

    /**
     * Get the individual oob values for the given time frame and schedule Id.
     * @param subject Caller
     * @param scheduleId PK of the schedule we are interested in
     * @param begin Start timestamp of the time frame
     * @param end End timestamp of the time frame
     * @return A list of individual OOB entries
     * @todo Do we want to fill gaps with count=0 or factor=0 in the result or not?
     */
    List<MeasurementOOB> getOObsForSchedule(Subject subject, int scheduleId, long begin, long end);
}
