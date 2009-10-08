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
package org.rhq.enterprise.server.alert.engine;

/**
 * @author Joseph Marques
 */

public enum AlertDefinitionEvent {
    CREATED, // when an alert definition is created, notify the cache by passing the definition and this event type 
    DELETED, // when an alert definition is deleted, notify the cache by passing the definition and this event type
    ENABLED, // when an alert definition is enabled, notify the cache by passing the definition and this event type
    DISABLED;// when an alert definition is disable, notify the cache by passing the definition and this event type
}