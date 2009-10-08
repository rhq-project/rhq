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

import org.rhq.enterprise.communications.command.CommandType;

/**
 * The public interface to the directory that provides information on what {@link CommandType command types} a
 * particular {@link CommandServiceMBean command service} supports.
 *
 * <p>If a command type is supported by a command service, it is said that the command service is a <i>provider</i> of
 * that command type.</p>
 *
 * <p>Each command service belongs to a particular subsystem - a subsystem is made up of one or more command services.
 * Subsystems are identified by simple <code>String</code> names - with the unnamed, anonymous subsystem being
 * identified by the <code>null</code> subsystem name.</p>
 *
 * @author John Mazzitelli
 */
public interface CommandServiceDirectoryMBean {
    /**
     * Given a particular {@link CommandType} and subsystem, this method will return an entry that gives you the <code>
     * ObjectName</code> to a {@link CommandServiceMBean command service} that provides support for that command type
     * within that subsystem.
     *
     * <p>If no command service provider exists for the given command type within the given subsystem, <code>null</code>
     * is returned.</p>
     *
     * <p><code>subsystem</code> may be <code>null</code> to denote the unnamed, anonymous subsystem.</p>
     *
     * @param  subsystem   the subsystem to look in (may be <code>null</code>)
     * @param  commandType find a provider for this command type
     *
     * @return the entry that indicates the name of the subsystem's command service that provides support for the given
     *         command type, or <code>null</code> if no provider exists
     */
    CommandServiceDirectoryEntry getCommandTypeProvider(String subsystem, CommandType commandType);

    /**
     * Given a subsystem, this will return all the entries for that subsystem - in other words, all the supported
     * command types and the command services that provide them.
     *
     * <p>If no command service providers exists for the given subsystem, an empty array is returned.</p>
     *
     * <p><code>subsystem</code> may be <code>null</code> to denote the unnamed, anonymous subsystem.</p>
     *
     * @param  subsystem the subsystem to look in (may be <code>null</code>)
     *
     * @return array of all entries for the given subsystem
     */
    CommandServiceDirectoryEntry[] getSubsystemEntries(String subsystem);

    /**
     * This will return all the entries for all subsystems - in other words, all the supported command types and the
     * command services that provide them in all subsystems.
     *
     * <p>If no command service providers exists, an empty array is returned.</p>
     *
     * @return array of all entries in the directory
     */
    CommandServiceDirectoryEntry[] getAllEntries();

    /**
     * This attribute will indicate if new commands are allowed to be added dynamically at runtime. If <code>
     * false</code>, only those command services currently installed at the time of this directory's registration will
     * be found in the directory. If <code>true</code>, the directory will add new command services to the directory as
     * they come online.
     *
     * <p>Note that this has no effect on dynamically discovering the <i>removal</i> of command services. The directory
     * will always detect the removal of commands and remove their entries from the directory.</p>
     *
     * @return <code>true</code> will allow the directory to discovery changes during runtime. <code>false</code> means
     *         only those command services already in existence at the time the directory service is registered will be
     *         stored in the directory.
     */
    boolean getAllowDynamicDiscovery();

    /**
     * This attribute will indicate if new commands are allowed to be added dynamically at runtime. If <code>
     * false</code>, only those command services currently installed at the time of this directory's registration will
     * be found in the directory. If <code>true</code>, the directory will add new command services to the directory as
     * they come online.
     *
     * <p>Note that this has no effect on dynamically discovering the <i>removal</i> of command services. The directory
     * will always detect the removal of commands and remove their entries from the directory.</p>
     *
     * @param flag <code>true</code> will allow the directory to discovery changes during runtime. <code>false</code>
     *             means only those command services already in existence at the time the directory service is
     *             registered will be stored in the directory.
     */
    void setAllowDynamicDiscovery(boolean flag);
}