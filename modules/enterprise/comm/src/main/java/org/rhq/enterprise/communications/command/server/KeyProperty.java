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
package org.rhq.enterprise.communications.command.server;

import org.rhq.enterprise.communications.ServiceContainer;

/**
 * Defines key properties and their valid values that command service MBeans can use in their <code>ObjectName</code>s.
 *
 * @author John Mazzitelli
 */
public interface KeyProperty {
    /**
     * This is a key name that defines the type of MBean. All MBeans that are deployed as part of the Command framework
     * must have this key property defined. Valid values of properties that have this key name are {@link #TYPE_COMMAND}
     * and {@link #TYPE_DIRECTORY}.
     */
    String TYPE = "type";

    /**
     * Value of a key property named {@link #TYPE} that specifies a type of MBean that supports/executes commands. An
     * MBean that has this key property value is considered a provider of a command type.
     */
    String TYPE_COMMAND = "command";

    /**
     * Value of a key property named {@link #TYPE} that specifies a type of MBean who maintains a directory of command
     * services and their supported command types.
     */
    String TYPE_DIRECTORY = "directory";

    /**
     * This is a key name that defines the subsystem in which this MBean belongs to. A subsystem is an arbitrary string
     * that groups together command services and their directory. All MBeans that are deployed as part of the Command
     * framework should have this key property defined. If this is not defined, the MBeans will be assigned the unnamed,
     * anonymous subsystem.
     */
    String SUBSYSTEM = "subsystem";

    /**
     * The key name that defines the unique index number may be assigned to the services.
     *
     * @see ServiceContainer#addCommandService(CommandService)
     */
    String ID = "id";
}