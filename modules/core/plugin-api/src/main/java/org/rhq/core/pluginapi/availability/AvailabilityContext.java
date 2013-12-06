/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.pluginapi.availability;

import org.rhq.core.domain.measurement.AvailabilityType;

/**
 * Resource specific context through which to make availability related calls back into the plugin container.
 *
 * @author Jay Shaughnessy
 */
public interface AvailabilityContext {

    /**
     * @deprecated this is no longer useful - all resources' avail checks are async since 4.10
     */
    @Deprecated
    public AvailabilityCollectorRunnable createAvailabilityCollectorRunnable(AvailabilityFacet availChecker,
        long interval);

    /**
     * The plugin container (PC) checks resource availability based on the collection interval on the {code}AvailabilityType{code}
     * metric.  At times the plugin may want the PC to request an availability check be done prior to the next
     * scheduled check time. For example, Start/Stop/Restart operation implementations may want to request that the PC
     * check availability sooner rather than later, to pick up the new lifecycle state.  This method should be used
     * sparingly, and should not in general override the scheduled avail checks.
     */
    public void requestAvailabilityCheck();

    /**
     * This method will return the last reported AvailabilityType, which can be null if not yet reported. This
     * method *does not* invoke a call to {link {@link AvailabilityFacet#getAvailability()}, raher it will return
     * the result of the most recent call to that method, made by the plugin container.
     *
     * @return the last reported availability type, or null if not yet reported.
     */
    public AvailabilityType getLastReportedAvailability();

    /**
     * This method allows the component to request the server set the resource DISABLED in the same way that
     * a user can set a resource DISABLED.  This should be used with care by component code as it will mean
     * that alerting and availability reporting will essentionally be ignored for the resource until it is
     * again enabled.  A user is free to enable a resource disabled by the component code.  If the resource is
     * already disabled then the call has no effect.
     *
     *  @see {@link #enable()}
     */
    public void disable();

    /**
     * This method allows the component to request the server set the resource ENABLED in the same way that
     * a user can set a resource ENABLED.  This should be used with care by component code as it does not care
     * how the resource was DISABLED. It can override a user action. If the resource is already disabled then
     * the call has no effect.
     *
     * @see {@link #disable()}
     */
    public void enable();

}
