/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009-2013, Red Hat, Inc. and/or its affiliates, and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.rhq.helpers.pluginAnnotations.agent;

/**
 * Metric Units.
 *
 * @author Galder Zamarreño
 * @author Heiko W. Rupp
 * See also org.rhq.core.domain.measurement.MeasurementUnits
 * @since 4.0
 */
@SuppressWarnings("unused")
public enum Units {
    NONE,  PERCENTAGE,
    BYTES, KILOBYTES, MEGABYTES, GIGABYTES, TERABYTES, PETABYTES,
    BITS, KILOBITS, MEGABITS, GIGABITS, TERABITS, PETABITS,
    EPOCH_MILLISECONDS, EPOCH_SECONDS,
    JIFFYS, NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS,
    CELSIUS, KELVIN, FAHRENHEIT;

   @Override
   public String toString() {
      return super.toString().toLowerCase();
   }
}
