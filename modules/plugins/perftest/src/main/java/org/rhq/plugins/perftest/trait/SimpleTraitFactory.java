/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.plugins.perftest.trait;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.perftest.trait.TraitFactory;

import java.util.Date;

/**
 * Create trait data
 *
 * @author Heiko W. Rupp
 */
public class SimpleTraitFactory implements TraitFactory {

    public MeasurementDataTrait nextValue(MeasurementScheduleRequest request) {

        String name = request.getName();
        Date date = new Date();

        MeasurementDataTrait data = new MeasurementDataTrait(request,name + ", " + date);

        return data;

    }
}
