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

package org.rhq.plugins.onewire;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.PDKAdapterUSB;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Heiko W. Rupp
 *
 */
public class OneWireAdapterComponent implements ResourceComponent {

    private DSPortAdapter adapter = null;
    String port;

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        boolean found = false;
        if (adapter == null)
            return AvailabilityType.DOWN;

        try {
            adapter.beginExclusive(true);
            found = adapter.adapterDetected();
            adapter.endExclusive();
        } catch (OneWireException e) {
            reopenAdapter();
        }

        return found ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        Configuration pluginConfig = context.getPluginConfiguration();
        String device = pluginConfig.getSimple("type").getStringValue();
        port = pluginConfig.getSimple("port").getStringValue();

        if (adapter == null) {
            adapter = new PDKAdapterUSB();
            adapter.selectPort(port);
        }
    }

    /* (non-Javadoc)
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        if (adapter != null) {
            try {
                adapter.freePort();
            } catch (OneWireException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public DSPortAdapter getAdapter() {
        return adapter;
    }

    public void reopenAdapter() {
        if (adapter != null) {
            try {
                adapter.freePort();
                Thread.sleep(500);
                adapter = new PDKAdapterUSB();
                adapter.selectPort(port);

            } catch (OneWireException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                ; // Does not matter
            }
        }
    }

}
