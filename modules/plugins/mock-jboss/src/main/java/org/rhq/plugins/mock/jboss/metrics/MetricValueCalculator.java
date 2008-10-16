/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.jboss.on.plugins.mock.jboss.metrics;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;

/**
 * Interface for the different mechanisms used to generate values for a measurement. There will be one instance of this
 * interface for each resource/metric name pairing. Subsequent calls to the resource to retrieve measurement values will
 * be fielded by the same instance to allow for values to build upon each other.
 *
 * @author Jason Dobies
 */
public interface MetricValueCalculator
{
   /**
    * Returns a value for a specific metric. Depending on the implmentation, the algorithm for how the next value is
    * computed will vary.
    *
    * @param request used in the creation of the <code>MeasurementData</code> return value.
    *
    * @return measurement domain object to be added to the measurement report.
    */
   MeasurementData nextValue(MeasurementScheduleRequest request);
}
