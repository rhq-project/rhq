/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.List;

import org.rhq.core.domain.criteria.TraitMeasurementCriteria;
import org.rhq.core.domain.measurement.TraitMeasurement;
import org.rhq.enterprise.gui.coregui.client.gwt.MetricsGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.measurement.MetricsManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class MetricsGWTServiceImpl extends AbstractGWTServiceImpl implements MetricsGWTService {

    private static final long serialVersionUID = 1L;

    private MetricsManagerLocal metricsManager = LookupUtil.getMetricsManager();

    @Override
    public List<TraitMeasurement> findResourceTraits(TraitMeasurementCriteria criteria) throws RuntimeException {
        try {
            List<TraitMeasurement> traits = (List<TraitMeasurement>) metricsManager.findResourceTraits(
                getSessionSubject(), criteria);
            return SerialUtility.prepare(traits, "metricsManager.findResourceTraits");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
