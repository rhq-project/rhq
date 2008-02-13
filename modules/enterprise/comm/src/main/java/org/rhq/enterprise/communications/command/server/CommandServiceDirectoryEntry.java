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

import java.io.Serializable;
import javax.management.ObjectName;
import org.rhq.enterprise.communications.command.CommandType;

/**
 * Encapsulates the information from a single entry in the {@link CommandServiceDirectory command service directory}. An
 * entry tells you the {@link #getCommandServiceName() name of the command service} that supports the entry's
 * {@link #getCommandType() command type} in the entry's {@link #getSubsystem() subsystem}.
 *
 * @author John Mazzitelli
 */
public class CommandServiceDirectoryEntry implements Serializable {
    private final String m_subsystem;
    private final CommandType m_commandType;
    private final ObjectName m_commandServiceName;

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link CommandServiceDirectoryEntry}.
     *
     * @param  subsystem          the entry's subsystem (may be <code>null</code>)
     * @param  commandType        the entry's command type (must not be <code>null</code>)
     * @param  commandServiceName the entry's command service name (must not be <code>null</code>)
     *
     * @throws IllegalArgumentException if a non-<code>null</code> able argument is <code>null</code>
     */
    public CommandServiceDirectoryEntry(String subsystem, CommandType commandType, ObjectName commandServiceName)
        throws IllegalArgumentException {
        if (commandType == null) {
            throw new IllegalArgumentException("commandType=null");
        }

        if (commandServiceName == null) {
            throw new IllegalArgumentException("commandServiceName=null");
        }

        if ((subsystem == null) || subsystem.equals(CommandServiceDirectory.NULL_SUBSYSTEM)) {
            m_subsystem = null;
        } else {
            m_subsystem = subsystem;
        }

        m_commandType = commandType;
        m_commandServiceName = commandServiceName;
    }

    /**
     * Returns the value of subsystem. The returned value may be <code>null</code>, which represents the unnamed,
     * anonymous subsystem.
     *
     * @return subsystem
     */
    public String getSubsystem() {
        return m_subsystem;
    }

    /**
     * Returns the value of commandType.
     *
     * @return commandType
     */
    public CommandType getCommandType() {
        return m_commandType;
    }

    /**
     * Returns the value of commandServiceName.
     *
     * @return commandServiceName
     */
    public ObjectName getCommandServiceName() {
        return m_commandServiceName;
    }

    /**
     * @see Object#toString()
     */
    public String toString() {
        StringBuffer strBuf = new StringBuffer("CommandServiceDirectoryEntry: ");

        strBuf.append("subsystem=[");
        strBuf.append(m_subsystem);
        strBuf.append("]; command-type=[");
        strBuf.append(m_commandType);
        strBuf.append("]; command-service-name=[");
        strBuf.append(m_commandServiceName);
        strBuf.append("]");

        return strBuf.toString();
    }

    /**
     * @see Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof CommandServiceDirectoryEntry)) {
            return false;
        }

        CommandServiceDirectoryEntry compareThis = (CommandServiceDirectoryEntry) obj;
        boolean retVal = false;

        if (this.m_commandType.equals(compareThis.m_commandType)) {
            if (this.m_commandServiceName.equals(compareThis.m_commandServiceName)) {
                String thisSubsystem = (this.m_subsystem == null) ? CommandServiceDirectory.NULL_SUBSYSTEM
                    : this.m_subsystem;
                String compareThisSubsystem = (compareThis.m_subsystem == null) ? CommandServiceDirectory.NULL_SUBSYSTEM
                    : compareThis.m_subsystem;

                if (thisSubsystem.equals(compareThisSubsystem)) {
                    retVal = true;
                }
            }
        }

        return retVal;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return toString().hashCode();
    }
}