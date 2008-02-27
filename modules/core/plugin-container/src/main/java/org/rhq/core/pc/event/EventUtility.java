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
package org.rhq.core.pc.event;

import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.resource.ResourceType;

import java.util.Set;

/**
 * @author Ian Springer
 */
public abstract class EventUtility {
    /**
     * Returns the EventDefinition with the specified name for the given Resource type, or null if no EventDefintion
     * by that name exists.
     *
     * @param name the name of the EventDefinition
     * @param resourceType a resource type
     *
     * @return the EventDefinition with the specified name for the given Resource type, or null if no EventDefintion
     *         by that name exists
     */
    @Nullable
    public static EventDefinition getEventDefinition(String name, ResourceType resourceType) {
        Set<EventDefinition> eventDefinitions = resourceType.getEventDefinitions();
        if (eventDefinitions != null) {
            for (EventDefinition eventDefinition : eventDefinitions) {
                if (eventDefinition.getName().equals(name)) {
                    return eventDefinition;
                }
            }
        }
        return null;
    }

    private EventUtility() {
    }
}
