/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.modcluster;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author Stefan Negrea
 *
 */
public class ModclusterServerComponent extends MBeanResourceComponent {

    private AvailabilityCollectorRunnable availabilityCollector;

    /* (non-Javadoc)
     * @see org.rhq.plugins.jmx.MBeanResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    @Override
    public void start(ResourceContext context) {

        availabilityCollector = context.createAvailabilityCollectorRunnable(new AvailabilityFacet() {
            public AvailabilityType getAvailability() {
                try {
                    OperationResult result = ModclusterServerComponent.this.invokeOperation("refresh",
                        new Configuration());

                    int numberOfAttempts = 0;

                    //The configuration should be loaded in less than 20 attempts.
                    //Only in extraneous cases (like network overload or a huge list webapp contexts)
                    //it can take more than this. 
                    while (numberOfAttempts < 20) {
                        String rawProxyInfo = (String) getEmsBean().getAttribute("proxyInfo").refresh().toString();
                        ProxyInfo proxyInfo = new ProxyInfo(rawProxyInfo);

                        if (proxyInfo.getAvailableContexts().size() != 0) {
                            break;
                        }

                        numberOfAttempts++;
                        Thread.sleep(2000);
                    }
                } catch (Exception e) {
                    log.info("mod_cluster availability update failed. Node configuration could not be refreshed.", e);
                    return AvailabilityType.DOWN;
                }

                return ModclusterServerComponent.super.getAvailability();
            }
        }, 60000L); // 1 minute - the minimum interval allowed

        // Now that you've created your availability collector, you must start it. Once started,
        // it is assigned a thread in a thread pool and begins periodically collecting availability.
        availabilityCollector.start();

        super.start(context);
    }

    /**
     * Cleans the old resource context and the old MBean.
     * @see ResourceComponent#stop()
     */
    public void stop() {
        availabilityCollector.stop();
        super.stop();
    }

    /* (non-Javadoc)
     * @see org.rhq.plugins.jmx.MBeanResourceComponent#getAvailability()
     */
    @Override
    public AvailabilityType getAvailability() {
        return availabilityCollector.getLastKnownAvailability();
    }
}
