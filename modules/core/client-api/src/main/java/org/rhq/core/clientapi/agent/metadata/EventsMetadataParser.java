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
package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.clientapi.descriptor.plugin.EventDescriptor;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.util.StringUtils;

/**
 * Parses event metadata and builds a corresponding definition domain object.
 *
 * @author Ian Springer
 */
public class EventsMetadataParser {
    public static EventDefinition parseEventsMetadata(EventDescriptor eventDescriptor, ResourceType resourceType) {
        EventDefinition eventDefinition = new EventDefinition(resourceType, eventDescriptor.getName());
        if (eventDescriptor.getDisplayName() != null) {
            eventDefinition.setDisplayName(eventDescriptor.getDisplayName());
        } else {
            eventDefinition.setDisplayName(StringUtils.deCamelCase(eventDescriptor.getName()));
        }
        eventDefinition.setDescription(eventDescriptor.getDescription());
        return eventDefinition;
    }
}