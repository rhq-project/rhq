/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pluginapi.inventory;

/**
 * When another discovery component discovers resources, the discovered details can be funneled through
 * implementations of this callback interface, thus allowing callbacks to tweek details of discovered resources.
 * This is helpful, for example, when one plugin wants to alter the name or description of some other plugin's
 * resource type. This is mainly used when writing plugins that cooperate with each other.
 */
public interface ResourceDiscoveryCallback {

    enum DiscoveryCallbackResults {
        /**
         * If the callback left the discovered details as-is, it should return this enum value.
         * The callback should only return this value if it did not alter any details data; if it did,
         * it should return PROCESSED instead.
         */
        UNPROCESSED,
        /**
         * If the callback recognized the discovered resource, it should return this enum value
         * to let the plugin container know that the details were processed by this callback and
         * were possibly altered from their original state. If more than one callback returned
         * this enum for the same discovered resource details, the discovery for that resource will
         * be aborted and it will not go into inventory. Multiple plugin callbacks cannot claim
         * ownership of the same resource details and return this enum value.
         */
        PROCESSED,
        /**
         * If the callback determines that the discovered resource is invalid or for some reason should
         * not go into inventory, it can veto its discovery via this enum value.
         */
        VETO
    }

    /**
     * When a resource has been discovered, its discovered resource details are passed to the callback via this method.
     * The callback can tweek those details as it sees fit or it can simply leave the details as-is and simply return.
     *
     * @param discoveredDetails resource details that were discovered and can be altered by the callback
     * @return PROCESSED if the callback has identified the discovered resource and possibly altered the details.
     *         VETO if the callback determines that the resource should not go into inventory and these details
     *         should be skipped by the plugin container.
     *         Otherwise, return UNPROCESSED to let the plugin container know that this callback doesn't recognize
     *         the details and they were left as-is. A null return value will be equivalent to UNPROCESSED.
     * @throws Exception
     */
    DiscoveryCallbackResults discoveredResources(DiscoveredResourceDetails discoveredDetails) throws Exception;
}