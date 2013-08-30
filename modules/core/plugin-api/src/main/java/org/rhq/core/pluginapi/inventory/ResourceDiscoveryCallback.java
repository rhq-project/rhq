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

import java.util.Set;

/**
 * When another discovery component discovers resources, the discovered details can be funneled through
 * implementations of this callback interface, thus allowing callbacks to tweek details of discovered resources.
 * This is helpful, for example, when one plugin wants to alter the name or description of some other plugin's
 * resource type. This is mainly used when writing plugins that cooperate with each other.
 */
public interface ResourceDiscoveryCallback {
    /**
     * When a set of resource details have been discovered, those details are passed to the callback via this method.
     * The callback can tweek those details as it sees fit or it can simply leave the details as-is and simply return.
     *
     * @param discoveredDetails a set of resource details that were discovered and can be altered by the callback
     *
     * @throws Exception
     */
    void discoveredResources(Set<DiscoveredResourceDetails> discoveredDetails) throws Exception;
}