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
package org.rhq.sample.custombundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * This can be the start of your own custom bundle plugin's component. Review the javadoc for {@link ResourceComponent}
 * and all {@link BundleFacet} interface to learn what you can do in your resource component.
 *
 * <p>You should not only read the javadoc in each of this class' methods, but you should also read the javadocs linked
 * by their "see" javadoc tags since those additional javadocs will contain a good deal of additional information you
 * will need to know.</p>
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class SampleBundlePluginServerComponent implements ResourceComponent, BundleFacet {

    private final Log log = LogFactory.getLog(SampleBundlePluginServerComponent.class);

    /**
     * All AMPS plugins are stateful - this context contains information that your resource component can use when
     * performing its processing.
     */
    private ResourceContext resourceContext;

    /**
     * This is called when your component has been started with the given context. You normally initialize some internal
     * state of your component.
     *
     * @see ResourceComponent#start(ResourceContext)
     */
    public void start(ResourceContext context) {
        resourceContext = context;
    }

    /**
     * This is called when the component is being stopped, usually due to the plugin container shutting down. You can
     * perform some cleanup here; though normally not much needs to be done here.
     *
     * @see ResourceComponent#stop()
     */
    public void stop() {
    }

    /**
     * All resource components must be able to tell the plugin container if the managed resource is available or not.
     * This method is called by the plugin container when it needs to know if the managed resource is actually up and
     * available. Since this plugin component class <b>is</b> the managed resource, we typically just return
     * UP here. However, this plugin method can return DOWN if it determines it should not be able to 
     * process bundles for some reason (e.g. if your bundle plugin has some dependencies on external processes
     * or machines and those processes or machines are down or otherwise inaccessible).
     *
     * @see ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }
}
