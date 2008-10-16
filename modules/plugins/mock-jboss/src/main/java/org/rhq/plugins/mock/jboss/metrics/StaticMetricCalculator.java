/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.jboss.on.plugins.mock.jboss.metrics;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.plugins.mock.jboss.scenario.MetricStatic;

/**
 * Measurement calculator implementation that returns the same value on each call.
 *
 * @author Jason Dobies
 */
public class StaticMetricCalculator implements MetricValueCalculator
{
   // Attributes  --------------------------------------------

   private double value;

   // Constructors  --------------------------------------------

   public StaticMetricCalculator(MetricStatic policy)
   {
      this.value = policy.getValue();
   }

   // MetricValueCalculator  --------------------------------------------

   public MeasurementData nextValue(MeasurementScheduleRequest request)
   {
      return new MeasurementDataNumeric(request, this.value);
   }
}
