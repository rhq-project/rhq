/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.core.pluginapi.inventory;

import java.util.HashMap;
import java.util.Set;

import org.rhq.core.system.ProcessInfo;

/**
 * A helper class to {@link ResourceContext} to store the process infos of individual
 * resources of a resource type.
 *
 * @author Lukas Krejci
 */
class ResourceTypeProcesses {

    private HashMap<String, ProcessInfo> processes;

    //why do we synchronize on something different than "this"? because if the callers
    //synchronize on "this" and try to call some of the below methods outside of that
    //synchronized block, they won't deadlock, which they would if the below methods were
    //syncing on "this" (i.e. if the methods below were synchronized).
    private final Object lock = new Object();

    public ProcessInfo getProcessInfo(String resourceKey) {
        synchronized(lock) {
            if (processes==null) {
                return null;
            }
            return processes.get(resourceKey);
        }
    }

    public void update(Set<DiscoveredResourceDetails> discoveryResults) {
        synchronized(lock) {
            if (processes!=null) {
                processes.clear();
            }

            if (discoveryResults==null || discoveryResults.isEmpty()) {
                return;
            }

            if (processes==null) {
                processes = new HashMap<String, ProcessInfo>(discoveryResults.size());
            }
            for (DiscoveredResourceDetails details : discoveryResults) {
                processes.put(details.getResourceKey(), details.getProcessInfo());
            }
        }
    }
}
