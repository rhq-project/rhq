/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pluginapi.bundle;

/**
 * This facet must be implemented by resource components that want to participate in bundle deployments.
 *
 * @author Thomas Segismont
 */
public interface BundleHandoverFacet {
    /**
     * Called by the plugin container when a bundle plugin indicates that the target resource component participates in
     * the bundle deployment.<br>
     * <br>
     * The class implementing this interface is the {@link org.rhq.core.pluginapi.inventory.ResourceComponent} class
     * backing the bundle target resource.<br>
     * <br>
     * The implementation should:
     * <ul>
     *     <li>document the set of content/action/parameters combinations it supports</li>
     *     <li>close the {@link java.io.InputStream} referenced by <code>content</code> after reading to free resources
     *     as soon as possible; the plugin container will still close it eventually</li>
     * </ul>
     *
     * @param handoverRequest handover parameters and context 
     * @return a report object indicating success or failure
     */
    BundleHandoverResponse handleContent(BundleHandoverRequest handoverRequest);
}
