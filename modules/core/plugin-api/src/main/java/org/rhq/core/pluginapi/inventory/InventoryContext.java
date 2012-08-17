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

package org.rhq.core.pluginapi.inventory;


/**
 * @author Stefan Negrea
 *
 */
public interface InventoryContext {

    /**
     * This method requests a deferred service discovery process for children of the resource. It requests the discovery
     * process and returns immediately without waiting for results. The discovery process imports into inventory all discovered
     * resources, so no further action is required from the caller.
     *
     * This method should be used by resources following an action (outside of create/delete children) that results
     * in having additional children discoverable. A good example is an operation execution that enables extra functionality
     * on the managed resource which in turn translates into having additional children available for management.
     *
     * Note: All services are discovered by the regular discovery process that runs every 24 hours. This method allows
     * a resource to request an immediate run of the discovery process for child resources, rather than wait for the scheduled
     * service discovery.
     */
    public void requestDeferredChildResourcesDiscovery();


    /**
     * This method requests a service discovery process for children of the resource. It schedules the discovery
     * process and blocks until the discovery finishes. The discovery process imports into inventory all discovered
     * resources, so no further action is required from the caller.
     *
     * This method should be used by resources following an action (outside of create/delete children) that results
     * in having additional children discoverable. A good example is an operation execution that enables extra functionality
     * on the managed resource which in turn translates into having additional children available for management. This method blocks
     * until the discovery processes finishes, this includes committing resources to inventory. For additional processing,
     * the calling resource can then request the list of child resources (it will include newly discovered resources too).
     *
     * Note: All services are discovered by the regular discovery process that runs every 24 hours. This method allows
     * a resource to request an immediate run of the discovery process for child resources, rather than wait for the scheduled
     * service discovery.
     *
     * @return discovered child resources
     */
    public void requestChildResourcesDiscovery();

}
