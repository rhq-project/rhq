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
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * Resource specific context through which to make availability related calls back into the plugin container.
 *
 * @author Jay Shaughnessy
 */
public interface AvailabilityContext {

    /**
     * Under certain circumstances, a resource component may want to perform asynchronous availability checks, as
     * opposed to {@link AvailabilityFacet#getAvailability()} blocking waiting for the managed resource to return
     * its availability status. Using asynchronous availability checking frees the resource component from having
     * to guarantee that the managed resource will provide availability status in a timely fashion.
     * 
     * If the resource component needs to perform asynchronous availability checking, it should call this
     * method to create an instance of {@link AvailabilityCollectorRunnable} inside the {@link ResourceComponent#start} method.
     * It should then call the returned object's {@link AvailabilityCollectorRunnable#start()} method within the same resource
     * component {@link ResourceComponent#start(ResourceContext)} method. The resource component should call the
     * {@link AvailabilityCollectorRunnable#stop()} method when the resource component
     * {@link ResourceComponent#stop() stops}. The resource component's {@link AvailabilityFacet#getAvailability()} method
     * should simply return the value returned by {@link AvailabilityCollectorRunnable#getLastKnownAvailability()}. This
     * method will be extremely fast since it simply returns the last availability that was retrieved by the
     * given availability checker. Only when the availability checker finishes checking for availability of the managed resource
     * (however long it takes to do so) will the last known availability state change.
     * 
     * For more information, read the javadoc in {@link AvailabilityCollectorRunnable}.
     *
     * @param availChecker the object that will perform the actual check of the managed resource's availability
     * @param interval the interval, in milliseconds, between availability checks. The minimum value allowed
     *                 for this parameter is {@link AvailabilityCollectorRunnable#MIN_INTERVAL}.
     *
     * @return the availability collector runnable that will perform the asynchronous checking
     */
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
}
